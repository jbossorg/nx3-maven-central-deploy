package org.jboss.nexus.content;

import org.jboss.nexus.Filter;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.slf4j.Logger;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.scheduling.CancelableHelper;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jboss.nexus.MavenCentralDeploy.SEARCH_COMPONENT_PAGE_SIZE;

@Named
public class ContentBrowserOrientDBImpl implements ContentBrowser{

    @Inject
    public ContentBrowserOrientDBImpl(BlobStoreManager blobStoreManager, BrowseService browseService, Set<CentralValidation> validations) {
        this.browseService = browseService;
        this.blobStoreManager = blobStoreManager;
        this.validations = validations;
    }

    private final BlobStoreManager blobStoreManager;

    private final BrowseService browseService;

    private final Set<CentralValidation> validations;


    @Override
    public void prepareValidationData(Repository repository, Filter filter, MavenCentralDeployTaskConfiguration configuration, List<FailedCheck> listOfFailures, List<Component> toDeploy, Logger log) {

              // ---------------------------------------------------------
              // OrientDB Implementation (data saved on the local storage)
              QueryOptions queryOptions = new QueryOptions(filter.getOrientDBSearchString(), "id", "asc", null, SEARCH_COMPONENT_PAGE_SIZE, null, false);

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

                      // todo check it actually works!
                      queryOptions = new QueryOptions(queryOptions.getFilter(), queryOptions.getSortProperty(), queryOptions.getSortDirection(), componentsCounter, queryOptions.getLimit(), null, false);
                      result = browseService.browseComponents(repository, queryOptions);
                      componentsCounter += SEARCH_COMPONENT_PAGE_SIZE;
                  }
              }

    }
}
