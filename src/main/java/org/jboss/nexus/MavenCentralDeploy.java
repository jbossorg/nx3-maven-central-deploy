package org.jboss.nexus;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.storage.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class MavenCentralDeploy extends ComponentSupport {

    private RepositoryManager repositoryManager;

    private BrowseService browseService;


    private BucketStore bucketStore;

    private BlobStoreManager blobStoreManager;

    Set<CentralValidation> validations;

    @Inject
    public MavenCentralDeploy(RepositoryManager repositoryManager, BrowseService browseService, BucketStore bucketStore, BlobStoreManager blobStoreManager, Set<CentralValidation> validations) {
        this.repositoryManager = checkNotNull(repositoryManager);
        this.browseService = checkNotNull(browseService);
        this.bucketStore = checkNotNull(bucketStore);
        this.blobStoreManager = checkNotNull(blobStoreManager);
        this.validations = checkNotNull(validations);
        checkArgument(!validations.isEmpty());
    }

    private static final int SEARCH_COMPONENT_PAGE_SIZE = 10;
    private static final int SEARCH_ASSET_PAGE_SIZE = 2; // FIXME: 07.12.2022 so small for testing purposes only. Make it 30+


    public void processDeployment(MavenCentralDeployTaskConfiguration configuration) {
        // TODO: 15.11.2022  define the business logic
        log.info("Deploying content.....");

        checkNotNull(configuration, "Configuration was not found");


        Repository releases = repositoryManager.get(checkNotNull(configuration.getRepository(), "Repository not configured for the task!"));

        QueryOptions queryOptions =  new QueryOptions(configuration.getFilter(), "id", "asc", 0, SEARCH_COMPONENT_PAGE_SIZE, null, false);

        PageResult<Component> result = browseService.browseComponents(releases, queryOptions);

        int counter_component = SEARCH_COMPONENT_PAGE_SIZE-1;

        List<FailedCheck> listOfFailures = new ArrayList<>();

       // validation
       while(!result.getResults().isEmpty()) {

           for (Component component : result.getResults()) {
               log.info("Validating component: " + component.toStringExternal());



               PageResult<Asset> assetsInside = browseService.browseComponentAssets(releases, component);


               for(CentralValidation validation : validations) {
                   validation.validateComponent(component, assetsInside.getResults(), listOfFailures);
               }

               queryOptions = new QueryOptions(queryOptions.getFilter(), queryOptions.getSortProperty(), queryOptions.getSortDirection(), counter_component ,queryOptions.getLimit(), null, false);
               result = browseService.browseComponents(releases, queryOptions);
                counter_component += SEARCH_COMPONENT_PAGE_SIZE;
           }
       }
       configuration.setLatestStatus("Processed "+result.getTotal()+"\none\ntwo\nthree\nfour\nfive\nsix\nseven\neight\nnine");
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

