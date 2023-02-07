package org.jboss.nexus;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.storage.*;


import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class MavenCentralDeploy extends ComponentSupport {

    private final RepositoryManager repositoryManager;

    private final BrowseService browseService;

    private final BlobStoreManager blobStoreManager;

    private final Set<CentralValidation> validations;

    private final Set<TestReportCapability> reports;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public MavenCentralDeploy(RepositoryManager repositoryManager, BrowseService browseService, BlobStoreManager blobStoreManager, Set<CentralValidation> validations, Set<TestReportCapability> reports) {
        this.repositoryManager = checkNotNull(repositoryManager);
        this.browseService = checkNotNull(browseService);
        this.blobStoreManager = checkNotNull(blobStoreManager);
        this.validations = checkNotNull(validations);
        checkArgument(!validations.isEmpty());
        this.reports = reports;
        checkArgument(!reports.isEmpty());
    }

    private static final int SEARCH_COMPONENT_PAGE_SIZE = 10;

   public void processDeployment(MavenCentralDeployTaskConfiguration configuration) {
        // TODO: 15.11.2022  define the business logic
        log.info("Deploying content.....");

        checkNotNull(configuration, "Configuration was not found");


        Repository releases = repositoryManager.get(checkNotNull(configuration.getRepository(), "Repository not configured for the task!"));

       Filter filter = Filter.parseFilterString(configuration.getFilter());
        QueryOptions queryOptions =  new QueryOptions(filter.getSearchString(), "id", "asc", 0, SEARCH_COMPONENT_PAGE_SIZE, null, false);
        // TODO: 10.01.2023 add time filter to remove already updated stuff
        
        
        PageResult<Component> result = browseService.browseComponents(releases, queryOptions);

        int counter_component = SEARCH_COMPONENT_PAGE_SIZE-1;

        List<FailedCheck> listOfFailures = new ArrayList<>();

        List<Component> toDeploy = new ArrayList<>();

       // validation
       while(!result.getResults().isEmpty()) {
           for (Component component : result.getResults()) {

              if(filter.checkComponent(component)) {
                 log.info("Validating component: " + component.toStringExternal());
                 toDeploy.add(component);

                 PageResult<Asset> assetsInside = browseService.browseComponentAssets(releases, component);

                 for (CentralValidation validation : validations) {
                    validation.validateComponent(configuration, component, assetsInside.getResults(), listOfFailures);
                 }
              }
           }

          queryOptions = new QueryOptions(queryOptions.getFilter(), queryOptions.getSortProperty(), queryOptions.getSortDirection(), counter_component ,queryOptions.getLimit(), null, false);
          result = browseService.browseComponents(releases, queryOptions);
          counter_component += SEARCH_COMPONENT_PAGE_SIZE;
       }

       StringBuilder response = new StringBuilder("Processed ").append(result.getTotal()).append(" components.");

       if(listOfFailures.isEmpty()) {

           // TODO: 10.01.2023 Go ahead with publishing

          response.append("\n- no errors were found.");
       } else {
           response.append("\n- ").append(listOfFailures.size()).append(" problems found!");

           for(TestReportCapability report : reports) {
              report.createReport(configuration, listOfFailures, toDeploy.size());
           }


//           // process
//          CDI.current().select(TestReportCapabilityDescriptorParent.class).forEach(
//              testReport -> testReport.validate()
//          );

       }

       configuration.setLatestStatus(response.toString());
       if(!listOfFailures.isEmpty())
           throw new RuntimeException("Validations failed"); // throw an exception so the task is reported as failed
       
       
//
//        PageResult<Asset> assets = browseService.browseAssets(releases, queryOptions);
//
//        for(Asset asset : assets.getResults()) {
//            log.info(asset.name());
//
//            BlobRef blobRef = asset.blobRef();
//
//
//            BlobStore blobStore = blobStoreManager.get(Objects.requireNonNull(blobRef).getStore());
//            Blob blob = blobStore.get(Objects.requireNonNull(blobRef).getBlobId());
//
//            StringBuilder stringBuilder = new StringBuilder();
//            try(BufferedInputStream is = new BufferedInputStream(Objects.requireNonNull(blob).getInputStream())) {
//                int read;
//                while((read = is.read()) != -1 )
//                    stringBuilder.append( (char)read );
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            stringBuilder.toString();
//        }


    }

    public void cancelDeployment() {
        // TODO: 15.11.2022 cancel running process deployment
        log.warn("Deploying got canceled.");
    }


}


