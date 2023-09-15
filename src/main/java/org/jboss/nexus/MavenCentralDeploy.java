package org.jboss.nexus;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.content.*;
import org.jboss.nexus.tagging.MCDTagSetupConfiguration;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.scheduling.CancelableHelper;


import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;


@Named
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class MavenCentralDeploy extends ComponentSupport {

    private final RepositoryManager repositoryManager;

//    private BrowseService browseService;

    private final Set<CentralValidation> validations;

    private final BlobStoreManager blobStoreManager;

    private final Set<TestReportCapability<?>> reports;

    private final TagStore tagStore;

    private final TagService tagService;

    private final TemplateRenderingHelper templateRenderingHelper;

    private final SearchUtils searchUtils;

    private final SearchService searchService;


//
//    /** Constructor for Nexus running in the OrientDB configuration. Move {@link Inject} here from {@link MavenCentralDeploy#MavenCentralDeploy(RepositoryManager, BlobStoreManager, Set, Set, TagStore, TagService, TemplateRenderingHelper)}
//     *
//     * @param repositoryManager repository manager
//     * @param browseService  browse service
//     * @param blobStoreManager blob store manager
//     * @param validations validations to be registered for handling
//     * @param reports reports to be registered for handling
//     * @param tagStore tag store
//     * @param tagService tag service
//     * @param templateRenderingHelper helper method for Velocity rendering
//     */
//    public MavenCentralDeploy(RepositoryManager repositoryManager, @Nullable BrowseService browseService, @Nullable BlobStoreManager blobStoreManager, Set<CentralValidation> validations, Set<TestReportCapability<?>> reports, @Nullable TagStore tagStore, @Nullable TagService tagService, TemplateRenderingHelper templateRenderingHelper) {
//        this.repositoryManager = checkNotNull(repositoryManager);
//        this.validations = checkNotNull(validations);
//        this.templateRenderingHelper = checkNotNull(templateRenderingHelper);
//        this.blobStoreManager = blobStoreManager;
//        this.browseService = browseService;
//        this.reports = reports;
//        this.tagStore = tagStore; // I expect this may be null in the community version
//        this.tagService = tagService;
//    }

    /** Constructor for Nexus running in the database configuration. Move {@link Inject} here from {@link MavenCentralDeploy#MavenCentralDeploy(RepositoryManager, BrowseService, BlobStoreManager, Set, Set, TagStore, TagService, TemplateRenderingHelper)}
     *
     * @param repositoryManager repository manager
     * @param blobStoreManager blob store manager
     * @param validations validations to be registered for handling
     * @param reports reports to be registered for handling
     * @param tagStore tag store
     * @param tagService tag service
     * @param templateRenderingHelper helper method for Velocity rendering
     */
    @Inject
    public MavenCentralDeploy(SearchService searchService, SearchUtils searchUtils, RepositoryManager repositoryManager, @Nullable BlobStoreManager blobStoreManager, Set<CentralValidation> validations, Set<TestReportCapability<?>> reports, @Nullable TagStore tagStore, @Nullable TagService tagService, TemplateRenderingHelper templateRenderingHelper) {
       // this(repositoryManager, null, blobStoreManager, validations, reports, tagStore, tagService, templateRenderingHelper);
        this.repositoryManager = checkNotNull(repositoryManager);
        this.validations = checkNotNull(validations);
        this.templateRenderingHelper = checkNotNull(templateRenderingHelper);
        this.blobStoreManager = blobStoreManager;
    //    this.browseService = null;
        this.reports = reports;
        this.tagStore = tagStore; // I expect this may be null in the community version
        this.tagService = tagService;
        this.searchUtils = searchUtils;
        this.searchService = searchService;
    }



    private static final int SEARCH_COMPONENT_PAGE_SIZE = 5;

   public void processDeployment(MavenCentralDeployTaskConfiguration configuration) {
        log.info("Deploying content.....");

        checkNotNull(configuration, "Configuration was not found");

        configuration.increaseRunNumber();

        StringBuilder response = new StringBuilder();

       List<FailedCheck> listOfFailures = new ArrayList<>();

       Filter filter = Filter.parseFilterString(configuration.getFilter());

        try {
          List<Component> toDeploy = new ArrayList<>();

          @SuppressWarnings("DataFlowIssue")
          Repository repository = checkNotNull(repositoryManager.get(checkNotNull(configuration.getRepository(), "Repository not configured for the task!")), "Invalid repository configured!");

          Optional<MavenContentFacet> optionalFacet = repository.optionalFacet(MavenContentFacet.class);
          if(optionalFacet.isPresent()) {
              // ---------------------------------------------------------
              // database implementation
              MavenContentFacet mavenContentFacet = optionalFacet.get();

              // TODO: 10.01.2023 add time filter to remove already updated stuff
              if(StringUtils.isNotBlank(filter.getDatabaseSearchString())) {
                  FluentQuery<FluentComponent> fluentComponentFluentQuery = mavenContentFacet.components().byFilter(filter.getDatabaseSearchString(), filter.getDatabaseSearchParameters());
                  String continuationToken = null;
                  do {
                      final Continuation<FluentComponent> browse = fluentComponentFluentQuery.browse(SEARCH_COMPONENT_PAGE_SIZE, continuationToken);
                      continuationToken = validateDatabaseAssets(configuration, listOfFailures, filter, toDeploy, browse);

                  } while (StringUtils.isNotBlank(continuationToken));
              } else {
                  String continuationToken = null;
                  do {
                      final Continuation<FluentComponent> browse = mavenContentFacet.components().browse(SEARCH_COMPONENT_PAGE_SIZE, continuationToken);
                      continuationToken = validateDatabaseAssets(configuration, listOfFailures, filter, toDeploy, browse);
                  }  while (StringUtils.isNotBlank(continuationToken)); // fixme Is this correct end of the continuation?
              }
          } /* else { //fixme we need this!
              // ---------------------------------------------------------
              // OrientDB Implementation (data saved on the local storage)
              QueryOptions queryOptions = new QueryOptions(filter.getOrientDBSearchString(), "id", "asc", null, SEARCH_COMPONENT_PAGE_SIZE, null, false);
              org.sonatype.nexus.repository.browse.BrowseService browseService = null; // fixme! can not be null

              // TODO: 10.01.2023 add time filter to remove already updated stuff
              PageResult<org.sonatype.nexus.repository.storage.Component> result = browseService.browseComponents(repository, queryOptions);

              int componentsCounter = SEARCH_COMPONENT_PAGE_SIZE;

              if(result != null ) {
                  // validation
                  while (!result.getResults().isEmpty()) {
                      CancelableHelper.checkCancellation();

                      for (org.sonatype.nexus.repository.storage.Component storageComponent : result.getResults()) {
                          if (filter.checkComponent(storageComponent)) {
                              Component component = new ComponentOrientDBImpl(storageComponent);
                              log.info("Validating component: " + component.toStringExternal());
                              toDeploy.add(component);

                              PageResult<org.sonatype.nexus.repository.storage.Asset> assetPageResult = browseService.browseComponentAssets(repository, storageComponent);

                              List<Asset> assetsInside = assetPageResult.getResults().stream().map(storageAsset -> new AssetOrientDBImpl(storageAsset, blobStoreManager) ).collect(Collectors.toList());
                              for (CentralValidation validation : validations) {
                                  validation.validateComponent(configuration, component, assetsInside, listOfFailures);
                              }
                          }
                      }


//
//                      for (org.sonatype.nexus.repository.storage.Component component : result.getResults()) {
//
//                          if (filter.checkComponent(component)) {
//                              log.info("Validating component: " + component.toStringExternal());
//                              toDeploy.add(component);
//
//                              PageResult<Asset> assetsInside = browseService.browseComponentAssets(repository, component);
//
//                              for (CentralValidation validation : validations) {
//                                  validation.validateComponent(configuration, component, assetsInside.getResults(), listOfFailures);
//                              }
//                          }
//                      }
//

                      // todo check it actually works!
                      queryOptions = new QueryOptions(queryOptions.getFilter(), queryOptions.getSortProperty(), queryOptions.getSortDirection(), componentsCounter, queryOptions.getLimit(), null, false);
                      result = browseService.browseComponents(repository, queryOptions);
                      componentsCounter += SEARCH_COMPONENT_PAGE_SIZE;
                  }
              }
          }
          */
          // todo now deploy, report and tag everything


          MCDTagSetupConfiguration mcdTagSetupConfiguration = findConfigurationForPlugin(MCDTagSetupConfiguration.class);
          response.append("Processed ").append(toDeploy.size()).append(" components.");

          ////////////////////
          Comparator<FailedCheck> failedCheckComparator = Comparator.comparing((FailedCheck o) -> o.getComponent().group())
                  .thenComparing(o -> o.getComponent().name())
                  .thenComparing(o -> o.getComponent().version());

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

                      tagService.associateById(publishedTag, repository, component.entityId());
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
             if (/*configuration.getMarkArtifacts() && */ mcdTagSetupConfiguration != null && StringUtils.isNotBlank((failedTagName = mcdTagSetupConfiguration.getFailedTagName()))) {
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

                      tagService.associateById(failedTagName, repository, component.entityId());
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

    /** Handles validation of assets when Nexus is configured to use database.
     *
     * @param configuration  task configuration
     * @param listOfFailures list with failures to feed errors
     * @param filter filter
     * @param toDeploy list with component to feed for the deployment
     * @param browse browsing entity to retrieve components
     *
     * @return continuation token for the next batch for the verification
     */
    private String validateDatabaseAssets(MavenCentralDeployTaskConfiguration configuration, List<FailedCheck> listOfFailures, Filter filter, List<Component> toDeploy, Continuation<FluentComponent> browse) {
        CancelableHelper.checkCancellation();
        if(browse.isEmpty())
            return null;

        browse.stream().filter(filter::checkComponent).forEach(
                fluentComponent -> {
                    List<SearchFilter> searchFilters = new ArrayList<>();
                    searchFilters.add(new SearchFilter("group.raw", fluentComponent.namespace()));
                    searchFilters.add(new SearchFilter("name.raw", fluentComponent.name()));
                    searchFilters.add(new SearchFilter("version", fluentComponent.version()));
                    searchFilters.add(new SearchFilter("repository_name", fluentComponent.repository().getName()));

                    SearchRequest request = SearchRequest.builder().searchFilters(searchFilters).build();
                    Iterable<ComponentSearchResult> components = this.searchService.browse(request);

                    String id;
                    Iterator<ComponentSearchResult> iterator = components.iterator();
                    if(iterator.hasNext()) {
                        id = iterator.next().getId();
                        if(iterator.hasNext())
                            throw new RuntimeException("Unexpected error: more than one "+fluentComponent.toStringExternal()+" found!");
                    } else
                        throw new RuntimeException("Unexpected error: Unable to find component "+fluentComponent.toStringExternal()+"!");


                    Component component = new ComponentDatabaseImpl(fluentComponent, id);
                    log.info("Validating component: " + component.toStringExternal());
                    toDeploy.add(component);

                    List<Asset> assetsInside = fluentComponent.assets().stream().map(AssetDatabaseImpl::new).collect(Collectors.toList());
                    for (CentralValidation validation : validations) {
                        validation.validateComponent(configuration, component, assetsInside, listOfFailures);
                    }
                }
        );
        return browse.nextContinuationToken();
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

        // TODO: 15.09.2023 also remove possible error tag from the artifact 

    }
}


