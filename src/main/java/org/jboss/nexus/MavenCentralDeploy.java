package org.jboss.nexus;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.tagging.MCDTagSetupConfiguration;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.jetbrains.annotations.NotNull;
import org.sonatype.goodies.common.ComponentSupport;
import org.eclipse.sisu.Nullable;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.scheduling.CancelableHelper;


import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class MavenCentralDeploy extends ComponentSupport {

    private final RepositoryManager repositoryManager;

    private final BrowseService browseService;

    private final Set<CentralValidation> validations;

    private final Set<TestReportCapability<?>> reports;

    private final TagStore tagStore;


    private final TagService tagService;

    private final TemplateRenderingHelper templateRenderingHelper;


   @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public MavenCentralDeploy(RepositoryManager repositoryManager, BrowseService browseService, Set<CentralValidation> validations, Set<TestReportCapability<?>> reports, @Nullable TagStore tagStore, @Nullable TagService tagService, TemplateRenderingHelper templateRenderingHelper) {
        this.repositoryManager = checkNotNull(repositoryManager);
        this.browseService = checkNotNull(browseService);
        this.validations = checkNotNull(validations);
        this.templateRenderingHelper = checkNotNull(templateRenderingHelper);
        this.reports = reports;
        this.tagStore = tagStore; // I expect this may be null in the community version
        this.tagService = tagService;
    }

    private static final int SEARCH_COMPONENT_PAGE_SIZE = 10;

   public void processDeployment(MavenCentralDeployTaskConfiguration configuration) {
        log.info("Deploying content.....");

        checkNotNull(configuration, "Configuration was not found");

        configuration.increaseRunNumber();

        StringBuilder response = new StringBuilder();

        try {
          Repository repository = repositoryManager.get(checkNotNull(configuration.getRepository(), "Repository not configured for the task!"));

          Filter filter = Filter.parseFilterString(configuration.getFilter());
          QueryOptions queryOptions = new QueryOptions(filter.getSearchString(), "id", "asc", 0, SEARCH_COMPONENT_PAGE_SIZE, null, false);


          // TODO: 10.01.2023 add time filter to remove already updated stuff
          PageResult<Component> result = browseService.browseComponents(repository, queryOptions);

          int counter_component = SEARCH_COMPONENT_PAGE_SIZE - 1;

          List<FailedCheck> listOfFailures = new ArrayList<>();

          List<Component> toDeploy = new ArrayList<>();

          if(result != null ) {
             // validation
             while (!result.getResults().isEmpty()) {
                CancelableHelper.checkCancellation();
                for (Component component : result.getResults()) {

                   if (filter.checkComponent(component)) {
                      log.info("Validating component: " + component.toStringExternal());
                      toDeploy.add(component);

                      PageResult<Asset> assetsInside = browseService.browseComponentAssets(repository, component);

                      for (CentralValidation validation : validations) {
                         validation.validateComponent(configuration, component, assetsInside.getResults(), listOfFailures);
                      }
                   }
                }

                queryOptions = new QueryOptions(queryOptions.getFilter(), queryOptions.getSortProperty(), queryOptions.getSortDirection(), counter_component, queryOptions.getLimit(), null, false);
                result = browseService.browseComponents(repository, queryOptions);
                counter_component += SEARCH_COMPONENT_PAGE_SIZE;
             }
          }

          MCDTagSetupConfiguration mcdTagSetupConfiguration = findConfigurationForPlugin(MCDTagSetupConfiguration.class);
          response.append("Processed ").append(toDeploy.size()).append(" components.");

            ////////////////////
            Comparator<FailedCheck> failedCheckComparator = Comparator.comparing((FailedCheck o) -> o.getComponent().requireGroup())
                    .thenComparing(o -> o.getComponent().name())
                    .thenComparing(o -> o.getComponent().requireVersion());

            // Add parameters for the template processing
            List<FailedCheck> errors = listOfFailures.stream().sorted(failedCheckComparator).collect(Collectors.toList());
            Map<String, Object> templateVariables = templateRenderingHelper.generateTemplateParameters(configuration, errors, toDeploy.size());


          if (listOfFailures.isEmpty()) {
             if (!configuration.getDryRun()) {
                toDeploy.forEach(this::publishArtifact);
             }

             final String publishedTag;
             if (configuration.getMarkArtifacts() && mcdTagSetupConfiguration != null && StringUtils.isNotBlank((publishedTag = mcdTagSetupConfiguration.getDeployedTagName()))) {
                if (tagStore == null || tagService == null) {
                   String msg = "Cannot mark synchronized artifacts! This version of Nexus does not support tagging.";
                   log.error(msg);
                   response.append("\n- Warning: ").append(msg);
                } else {
                   log.info("Tagging " + listOfFailures.size() + " artifacts.");
                   verifyTag(publishedTag, mcdTagSetupConfiguration.getDeployedTagAttributes(), templateVariables);

                   toDeploy.forEach(component -> {
                      if (log.isDebugEnabled())
                         log.debug("Tagging failed artifact: " + component.toStringExternal());

                      //noinspection ConstantConditions
                      tagService.associateById(publishedTag, repository, component.getEntityMetadata().getId());
                   });
                }
             }

             response.append("\n- no errors were found.");

             if (configuration.getDryRun())
                response.append("\n- the deployment was a dry run (no actual publishing).");
          } else {
             response.append("\n- ").append(listOfFailures.size()).append(" problems found!");

             for (TestReportCapability<?> report : reports) {
                report.createReport(configuration, listOfFailures,  new HashMap<>(templateVariables)); // re-pack template variables so each report may work within its space
             }

             final String failedTagName;
             if (configuration.getMarkArtifacts() && mcdTagSetupConfiguration != null && StringUtils.isNotBlank((failedTagName = mcdTagSetupConfiguration.getFailedTagName()))) {
                if (tagStore == null || tagService == null) {
                   String msg = "Cannot mark failed artifacts! This version of Nexus does not support tagging.";
                   log.error(msg);
                   response.append("\n- Warning: ").append(msg);
                } else {
                   log.info("Tagging " + listOfFailures.size() + " failures.");

                   verifyTag(failedTagName, mcdTagSetupConfiguration.getFailedTagAttributes(), templateVariables);

                   listOfFailures.stream().map(FailedCheck::getComponent).distinct().forEach(component -> {

                      if (log.isDebugEnabled())
                         log.debug("Tagging failed artifact: " + component.toStringExternal());

                      //noinspection ConstantConditions
                      tagService.associateById(failedTagName, repository, component.getEntityMetadata().getId());
                   });
                }
             }

             throw new RuntimeException("Validations failed!"); // throw an exception so the task is reported as failed
          }
        } catch (RuntimeException e) {
          if(response.length()>0)
             response.append('\n');

          response.append(e.getMessage());

          throw e;
        } finally {
          configuration.setLatestStatus(response.toString());
        }

    }

   /** Verifies, whether the tag of given name exists. If not, the tag is created. Also, the function ensures the attributes defined by tagAttributes of this tag exist and have the right value.
    *
    * @param tagName name of the tag to get
    * @param tagAttributes possible tag attributes, that need to be set
    */
     void verifyTag(@NotNull String tagName, @Nullable String tagAttributes, @NotNull final Map<String, Object> taskConfiguration) {
      tagName = templateRenderingHelper.render(tagName, taskConfiguration);
      Tag result = tagStore.get(tagName); // TODO: 02.03.2023 Junit testing

      Map<String, Object> attributes = new HashMap<>();
      if(StringUtils.isNotBlank(tagAttributes)) {

         try(StringReader stringReader = new StringReader(tagAttributes)) {
            try {
               Properties properties = new Properties();
               properties.load(stringReader);
               properties.forEach((key, value) -> attributes.put((templateRenderingHelper.render((String)key, taskConfiguration)), (templateRenderingHelper.render((String)value, taskConfiguration))));
            } catch (IOException e) {
               log.error("MavenCentralDeploy::verifyTag: error reading tag attributes from "+tagAttributes);
            }
         }
      }

      if(result == null) {
         result = tagStore.newTag().name(tagName).attributes(new NestedAttributesMap("attributes", attributes)) ;
         tagStore.create(result);
      } else {
         for(String key: attributes.keySet()) {
            result.attributes().set(key, attributes.get(key));
         }
         tagStore.update(result);
      }
    }

    private final HashMap<Class<? extends MavenCentralDeployCapabilityConfigurationParent>, MavenCentralDeployCapabilityConfigurationParent> registeredConfigurations = new HashMap<>();

   /** Register configuration for your capability. It should be called in capability activation.
    *
    * @param configuration the configuration object
    */
    public void registerConfiguration(MavenCentralDeployCapabilityConfigurationParent configuration) {
       registeredConfigurations.put(configuration.getClass(), configuration);
    }

   /** Unregister capability configuration. The method should be called during capability deactivation.
    *
    * @param configuration configuration to remove
    */
    public void unregisterConfiguration(MavenCentralDeployCapabilityConfigurationParent configuration) {
       registeredConfigurations.remove(configuration.getClass());
    }

   /** Updates the specific configuration if needed. It does not activate the feature if it is not active.
    *
    * @param configuration configuration to be updated.
    */
   public void updateConfiguration(@NotNull MavenCentralDeployCapabilityConfigurationParent configuration) {
       if(registeredConfigurations.containsKey(configuration.getClass())) {
          registeredConfigurations.put(configuration.getClass(), configuration);
       }
    }

   /** Find the configuration based on the class configuration.
    *
    * @param configurationClass class to typecast the returned configuration.
    * @return null or the configuration if the capability is enabled
    */
    @org.jetbrains.annotations.Nullable
    public <T extends MavenCentralDeployCapabilityConfigurationParent> T findConfigurationForPlugin(Class<T> configurationClass  ) {
       //noinspection unchecked
       return  (T)registeredConfigurations.get(configurationClass);
    }

    void publishArtifact(Component component) {
       // TODO: 06.03.2023 push the content to Maven Central

    }
}


