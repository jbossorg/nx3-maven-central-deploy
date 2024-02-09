package org.jboss.nexus;

import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jboss.nexus.content.Component;
import org.jboss.nexus.content.ContentBrowser;
import org.jboss.nexus.tagging.MCDTagSetupConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.*;


@Named
@Singleton
@SuppressWarnings("CdiInjectionPointsInspection")
public class MavenCentralDeploy extends ComponentSupport {

    private final RepositoryManager repositoryManager;

    private final Set<TestReportCapability<?>> reports;

    private final TagStore tagStore;

    private final TagService tagService;

    private final TemplateRenderingHelper templateRenderingHelper;

    private final ContentBrowser contentBrowser;


    /** Constructor for Nexus running in the database configuration.
     *
     * @param repositoryManager repository manager
     * @param reports reports to be registered for handling
     * @param tagStore tag store
     * @param tagService tag service
     * @param templateRenderingHelper helper method for Velocity rendering
     */
    @Inject
    public MavenCentralDeploy(RepositoryManager repositoryManager, Set<TestReportCapability<?>> reports, @Nullable TagStore tagStore, @Nullable TagService tagService, TemplateRenderingHelper templateRenderingHelper, ContentBrowser contentBrowser) {
        this.repositoryManager = checkNotNull(repositoryManager);
        this.templateRenderingHelper = checkNotNull(templateRenderingHelper);
        this.reports = reports;
        this.tagStore = tagStore; // I expect this may be null in the community version
        this.tagService = tagService;
        this.contentBrowser = contentBrowser;
    }

    @SuppressWarnings("unused")
    public static final int SEARCH_COMPONENT_PAGE_SIZE = 5;

   public void processDeployment(MavenCentralDeployTaskConfiguration configuration) {
        log.info("Deploying content.....");

        checkNotNull(configuration, "Configuration was not found");

        configuration.increaseRunNumber();

        StringBuilder response = new StringBuilder();

       List<FailedCheck> listOfFailures = new ArrayList<>();

       Filter filter = Filter.parseFilterString(configuration.getFilter());

        try {
          List<Component> toDeploy = new ArrayList<>();

          Repository repository = checkNotNull(repositoryManager.get(checkNotNull(configuration.getRepository(), "Repository not configured for the task!")), "Invalid repository configured!");
          contentBrowser.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log);

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

              MavenCentralDeployCentralSettingsConfiguration deployDefaultConfiguration = findConfigurationForPlugin(MavenCentralDeployCentralSettingsConfiguration.class);

              final String centralUser = configuration.getCentralUser(deployDefaultConfiguration),
                      centralPassword = configuration.getCentralPassword(deployDefaultConfiguration),
                      centralURL = configuration.getCentralURL(deployDefaultConfiguration),
                      centralMode = configuration.getCentralMode(deployDefaultConfiguration);

              boolean publishPossible = true;
              if (StringUtils.isBlank(centralUser)) {
                  log.error("The artifacts can not be published. Username of the Maven Central account is missing!");
                  publishPossible = false;
              }
              if (StringUtils.isBlank(centralPassword)) {
                  log.error("The artifacts can not be published. Password of the Maven Central account is missing!");
                  publishPossible = false;
              }
              if (!"USER_MANAGED".equalsIgnoreCase(centralMode) && !AUTOMATIC.equalsIgnoreCase(centralMode)) {
                  log.error("The artifacts can not be published. Deployment mode should either be USER_MANAGED or AUTOMATIC! It is " + centralMode);
                  publishPossible = false;
              }
              if (!UrlValidator.getInstance().isValid(centralURL)) {
                  log.error("The artifacts can not be published. The URL of the Maven Central is not valid! The value is " + centralURL);
                  publishPossible = false;
              }

              long latestComponentTime = configuration.getLatestComponentTime();

              if (publishPossible && !configuration.getDryRun()) {

                  // FIXME 2024-01-29 - just testing zip here
                  File zipTestFile =  new File("/Users/dhladky/xxx/empty/uploadtest/testZip.zip");
                  try {
                      //noinspection ResultOfMethodCallIgnored
                      zipTestFile.createNewFile();
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
                  try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipTestFile))) {

                      for (Component component : toDeploy) {
                          publishArtifact(component, zipStream);

                          if (component.getCreated() > latestComponentTime)
                              latestComponentTime = component.getCreated();
                      }
                  } catch (FileNotFoundException ex) {
                      throw new RuntimeException(ex);// TODO: 2024-01-30 - zip error handling
                  } catch (IOException ex) {
                      throw new RuntimeException(ex); // TODO: 2024-01-30 - zip error handling
                  }
              } else {
                  // do not publish (dry run or error)
                  Optional<Long> oldestFound = toDeploy.stream().map(Component::getCreated).max(Long::compareTo);
                  if(oldestFound.isPresent() && oldestFound.get() > latestComponentTime)
                      latestComponentTime = oldestFound.get();

              }

             if(configuration.getMarkArtifacts())
                configuration.setLatestComponentTime(String.valueOf(latestComponentTime));


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

                      tagService.maybeAssociateById(publishedTag, repository, component.entityId());
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

             if(configuration.getMarkArtifacts()) {
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

                             tagService.maybeAssociateById(failedTagName, repository, component.entityId());
                         });
                     }
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

    void publishArtifact(Component component, ZipOutputStream zipStream) throws IOException {


       // TODO: 06.03.2023 push the content to Maven Central

       // TODO: 15.09.2023 also remove possible error tag from the artifact

       byte[] buffer = new byte[1024];

       String zipName = (component.group().replace('.', '/')+"/" + component.name().replace('.', '/')+"/"+component.version()+"/");
       ZipEntry zipEntry = new ZipEntry(zipName);
       zipStream.putNextEntry(zipEntry);
       zipStream.closeEntry();

       component.assetsInside().forEach(asset -> {
           String assetName = asset.name();
           if(assetName.charAt(0)=='/')
               assetName = assetName.substring(1);

           final ZipEntry assetEntry = new ZipEntry(assetName);
           try(InputStream inputStream = asset.openContentInputStream()) {
                zipStream.putNextEntry(assetEntry);

               int length;
               while ((length = inputStream.read(buffer)) >= 0) {
                   zipStream.write(buffer, 0, length);
               }
           } catch (IOException e) {
               throw new RuntimeException("Error writing "+asset.name(), e);
           }
       });
    }
}


