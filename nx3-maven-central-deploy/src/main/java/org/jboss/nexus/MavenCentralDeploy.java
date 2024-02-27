package org.jboss.nexus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.apache.commons.io.input.QueueInputStream;
import org.apache.commons.io.output.QueueOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
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
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.AUTOMATIC;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_ZIP;


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

    /** Sonatype Central Bundle endpoint.
     *
     * @see <a href="https://central.sonatype.com/api-doc" >documentation</a> */
    private static final String BUNDLE_ENDPOINT = "/api/v1/publisher/upload";

    /** Sonatype Central status endpoint.
     *
     * @see <a href="https://central.sonatype.com/api-doc" >documentation</a> */
    private static final String STATUS_ENDPOINT = "/api/v1/publisher/status";


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

              String centralUser = configuration.getCentralUser(deployDefaultConfiguration),
                      centralPassword = configuration.getCentralPassword(deployDefaultConfiguration),
                      centralURL = configuration.getCentralURL(deployDefaultConfiguration),
                      centralMode = configuration.getCentralMode(deployDefaultConfiguration),
                      centralProxy = configuration.getCentralProxy(deployDefaultConfiguration)  ;
              Integer centralProxyPort = configuration.getCentralProxyPort(deployDefaultConfiguration);

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
              String deploymentCreated = null;

              if (publishPossible && !configuration.getDryRun()) {
                  if(centralURL.endsWith("/"))
                      centralURL = centralURL.substring(0, centralURL.length()-1);

                  // curl -u 'dhladky@redhat.com:redacted' -F bundle=@kieuploadtest.zip 'https://central.sonatype.com/api/v1/publisher/upload?name=testbundle;publishingType=USER_MANAGED'

                  final Credentials credentials = new UsernamePasswordCredentials(centralUser, centralPassword);

                  try (QueueOutputStream queueOutputStream = new QueueOutputStream()) {
                      try (QueueInputStream queueInputStream = queueOutputStream.newQueueInputStream()) {

                          HttpPost httpPost = new HttpPost(centralURL+BUNDLE_ENDPOINT+"?name="+configuration.getBundleName()+"&publishingType="+centralMode );

                          MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
                                  .addBinaryBody("bundle", queueInputStream, ContentType.create(APPLICATION_ZIP), (configuration.getBundleName()+".zip"));

                          try (ZipOutputStream zipStream = new ZipOutputStream(queueOutputStream)) {
                              for (Component component : toDeploy) {
                                  publishArtifact(component, zipStream);

                                  if (component.getCreated() > latestComponentTime)
                                      latestComponentTime = component.getCreated();
                              }
                          }

                          httpPost.setEntity(multipartEntityBuilder.build());

                          Header authenticateHeader = new BasicScheme().authenticate(credentials, httpPost, new BasicHttpContext());
                          httpPost.addHeader(authenticateHeader);

                          HttpClientBuilder httpClientBuilder = getHttpClientBuilder(centralProxy, centralProxyPort);

                          // check for man in the middle without login to avoid main in the middle attacks
                          try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                              HttpPost httpNoBodyPost = new HttpPost(httpPost.getURI());
                              try(CloseableHttpResponse requestResponse = httpClient.execute(httpNoBodyPost)) {
                                  if(requestResponse.getStatusLine().getStatusCode() != 401) // the endpoint should complain about not being authenticated
                                      throw new IOException("Unexpected return value when connecting " + httpNoBodyPost.getURI().toString());
                              }
                          }


                          try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                              try(CloseableHttpResponse requestResponse = httpClient.execute(httpPost)) {
                                  if(requestResponse.getStatusLine().getStatusCode() != 201) {
                                     throw new IOException("Unexpected response from Maven Central: "+requestResponse.getStatusLine().getStatusCode()+" - "+requestResponse.getStatusLine().getReasonPhrase());
                                  } else {
                                      try(BufferedReader reader = new BufferedReader(new InputStreamReader(requestResponse.getEntity().getContent(), StandardCharsets.UTF_8))){
                                          deploymentCreated = reader.readLine();
                                      }
                                  }

                              }
                          }

                      }
                  } catch (IOException ex) {
                      errors.add(new FailedCheck("Failed to deploy "+ ex.getMessage()));
                      log.error(ex.getMessage());
                  } catch (AuthenticationException e) {
                      // it can not happen with basic authentication only
                      log.error("Unexpected authentication error: "+e.getMessage());
                      throw new RuntimeException(e);
                  }


                  if(StringUtils.isNotBlank(deploymentCreated)) {
                      try {

                          HttpClientBuilder httpClientBuilder = getHttpClientBuilder(centralProxy, centralProxyPort);

                          // dhladky@dhladky-mac ~ % curl -u 'user:redacted'  -X POST 'https://central.sonatype.com/api/v1/publisher/status?id=cbfe2fa8-c84c-4ec0-8e45-c71cdb5f6390'
                          HttpPost httpPost = new HttpPost(centralURL+STATUS_ENDPOINT+"?id="+deploymentCreated );

                          Header authenticateHeader;
                          try {
                              authenticateHeader = new BasicScheme().authenticate(credentials, httpPost, new BasicHttpContext());
                          } catch (AuthenticationException e) {
                              // this should never happen for basic authentication only
                              throw new RuntimeException(e);
                          }

                          httpClientBuilder.setDefaultHeaders(Collections.singleton(authenticateHeader));

                          waitForMavenCentralResults(errors, httpClientBuilder, httpPost);

                      } catch (IOException e) {
                          throw new RuntimeException(e); // TODO: 2024-02-26 - just workaround - proper error handling missing!


                      }
                  }


              } else {
                  // do not publish (dry run or error)
                  Optional<Long> oldestFound = toDeploy.stream().map(Component::getCreated).max(Long::compareTo);
                  if(oldestFound.isPresent() && oldestFound.get() > latestComponentTime)
                      latestComponentTime = oldestFound.get();

              }

              if(configuration.getMarkArtifacts() && errors.isEmpty() && (StringUtils.isNotBlank(deploymentCreated)) || configuration.getDryRun()) // only record latest artifacts if the reason is not an error
                configuration.setLatestComponentTime(String.valueOf(latestComponentTime));

             final String publishedTag;

             if (errors.isEmpty() && (StringUtils.isNotBlank(deploymentCreated) || configuration.getDryRun()) && configuration.getMarkArtifacts() && mcdTagSetupConfiguration != null && StringUtils.isNotBlank((publishedTag = mcdTagSetupConfiguration.getDeployedTagName()))) {
                if (tagStore == null || tagService == null) {
                   String msg = "Cannot mark synchronized artifacts! This version of Nexus does not support tagging.";
                   log.error(msg);
                   response.append("\n- Warning: ").append(msg);
                } else {
                   log.info("Tagging " + toDeploy.size() + " artifacts.");
                   verifyTag(publishedTag, mcdTagSetupConfiguration.getDeployedTagAttributes(), templateVariables);

                   toDeploy.forEach(component -> {
                      if (log.isDebugEnabled())
                         log.debug("Tagging deployed artifact: " + component.toStringExternal());

                      tagService.maybeAssociateById(publishedTag, repository, component.entityId());
                   });
                }
             }

             if(publishPossible ) {
                 if(errors.isEmpty()) {
                     response.append("\n- no errors were found.");
                 } else {
                     response.append("\n- deployment was not successful");

                     errors.stream().map(FailedCheck::getProblem)
                             .forEach(problem -> response.append("\n    - ").append(problem));

                 }
             } else {
                 response.append("\n- validation was OK, but the Maven Central deployment is not properly configured.");
             }

             if (configuration.getDryRun())
                response.append("\n- the deployment was a dry run (no actual publishing).");
          }

          if(!errors.isEmpty()) {

             response.append("\n- ").append(errors.size()).append(" problems found!");

             for (TestReportCapability<?> report : reports) {
                report.createReport(configuration, errors,  new HashMap<>(templateVariables)); // re-pack template variables so each report may work within its space
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

                         listOfFailures.stream().filter(FailedCheck::isHasComponent).map(FailedCheck::getComponent).distinct().forEach(component -> {
                            if (log.isDebugEnabled())
                              log.debug("Tagging failed artifact: " + component.toStringExternal());

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

    @NotNull
    static HttpClientBuilder getHttpClientBuilder(String centralProxy, Integer centralProxyPort) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if(StringUtils.isNotBlank(centralProxy)) {
            HttpHost httpHost;
            if(centralProxyPort != null) {
                httpHost = new HttpHost(centralProxy, centralProxyPort);
            } else
                httpHost = new HttpHost(centralProxy);

            httpClientBuilder.setProxy(httpHost);
        }
        return httpClientBuilder;
    }


    /** Verifies, whether the tag of given name exists. If not, the tag is created. Also, the function ensures the attributes defined by tagAttributes of this tag exist and have the right value.
    *
    * @param tagName name of the tag to get
    * @param tagAttributes possible tag attributes, that need to be set
    */
     void verifyTag(@NotNull String tagName, @Nullable String tagAttributes, @NotNull final Map<String, Object> taskConfiguration) {
      tagName = templateRenderingHelper.render(tagName, taskConfiguration);
      Tag result = tagStore.get(tagName);

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

    private static final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("RegExpRedundantEscape")
    private static final Pattern keyPattern = Pattern.compile("^pkg:maven/([\\w\\.-]+)/([\\w\\.-]+)@([\\w\\.-]+)(\\?.*)?");


    /** The method waits for Maven Central processes the deployment and if any validation errors appear there, it reports them.
     *
     * @param errors list for errors to be added if the validation fails
     * @param httpClientBuilder  client builder with pre-configured authentication and proxy
     * @param httpPost the post request to be used
     */
    private void waitForMavenCentralResults(List<FailedCheck> errors, HttpClientBuilder httpClientBuilder, HttpPost httpPost) throws IOException {
        // TODO: 2024-02-22 - wait for validation on Sonatype site finishes and analyze possible errors


        do {
            try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
                try(CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {

                   if (httpResponse.getStatusLine().getStatusCode() == 200) {
                       JsonNode jsonParser = mapper.readTree(httpResponse.getEntity().getContent());

                       JsonNode deploymentStateNode = jsonParser.get("deploymentState");
                       if (deploymentStateNode == null) {
                           String msg = "Missing deploymentState field when calling " + STATUS_ENDPOINT;
                           log.error(msg);
                           errors.add(new FailedCheck(msg));
                           return;
                       }

                       String deploymentState = deploymentStateNode.asText();
                       switch (deploymentState) {
                           case "PENDING":
                               // TODO: 2024-02-26 - what does it mean???

                           case "VALIDATING":
                           case "PUBLISHING":
                               waitSomeTime(5000);
                               break;
                           case "PUBLISHED":
                               return; // all is done and OK, nothing to report
                           case "FAILED":
                               JsonNode errorsNode = jsonParser.get("errors");
                               Iterator<String> errorIterator = errorsNode.fieldNames();
                               while (errorIterator.hasNext()) {
                                   final String packageName = errorIterator.next();
                                   final Component component = getComponent(packageName);

                                   Iterator<JsonNode> detailedErrorsIterator = errorsNode.get(packageName).elements();
                                   while (detailedErrorsIterator.hasNext()) {
                                       errors.add(new FailedCheck(component, detailedErrorsIterator.next().asText()));
                                   }
                               }
                               return;
                           default:
                               String msg = "Unexpected value " + deploymentState + " when calling " + STATUS_ENDPOINT;
                               throw new RuntimeException(msg);
                       }

                   } else {
                       String msg = "Unexpected error processing the status request for deployment "+ httpPost.getURI().getQuery()+": "+httpResponse.getStatusLine().getStatusCode()+" - "+httpResponse.getStatusLine().getReasonPhrase();
                       log.error(msg);
                       errors.add(new FailedCheck(msg));
                       return;
                    }
                }
            }
        } while (true);

    }




    /** Parses component from Sonatype defined package name
     *
     * @param packageName name of the component in format pkg:maven/xcom.sonatype.central.testing.david-hladky/kie-api@7.42.0.Final?type=bundle
     * @return either properly parsed component or {@link FailedCheck#NO_COMPONENT}
     */
    @NotNull
    private static Component getComponent(String packageName) {
        final Component component;
        if(packageName != null) {
            Matcher keyMatch = keyPattern.matcher(packageName);
            if(keyMatch.matches()) {
                component = new TemplateRenderingHelper.FictiveComponent(keyMatch.group(1), keyMatch.group(2), keyMatch.group(3));
            } else
                component = FailedCheck.NO_COMPONENT;
        } else
            component = FailedCheck.NO_COMPONENT;
        return component;
    }


    @SuppressWarnings("SameParameterValue")
    private static void waitSomeTime(long milliseconds)  {
        long end = System.currentTimeMillis()+milliseconds;
        do {
            CancelableHelper.checkCancellation(); // if possible do something more useful than wait here
            try {
                //noinspection BusyWait
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new TaskInterruptedException("Thread '" + Thread.currentThread().getName() + "' is interrupted", false);
            }
        } while (System.currentTimeMillis() < end);
    }
}


