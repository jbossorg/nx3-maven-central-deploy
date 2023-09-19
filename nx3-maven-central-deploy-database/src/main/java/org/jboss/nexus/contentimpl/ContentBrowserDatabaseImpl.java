package org.jboss.nexus.contentimpl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.Filter;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Asset;
import org.jboss.nexus.content.Component;
import org.jboss.nexus.content.ContentBrowser;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.slf4j.Logger;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.scheduling.CancelableHelper;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

import static org.jboss.nexus.MavenCentralDeploy.SEARCH_COMPONENT_PAGE_SIZE;

@Named
public class ContentBrowserDatabaseImpl implements ContentBrowser {


    @Inject
    public ContentBrowserDatabaseImpl(SearchService searchService, Set<CentralValidation> validations) {
        this.searchService = Preconditions.checkNotNull(searchService);
        this.validations = Preconditions.checkNotNull(validations);
    }


    private final SearchService searchService;

    private final Set<CentralValidation> validations;

    @Override
    public void prepareValidationData(final Repository repository, final Filter filter, final MavenCentralDeployTaskConfiguration configuration, final List<FailedCheck> listOfFailures, final List<Component> toDeploy, Logger log) {
        //todo


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
                    continuationToken = validateDatabaseAssets(configuration, listOfFailures, filter, toDeploy, browse, log);

                } while (StringUtils.isNotBlank(continuationToken));
            } else {
                String continuationToken = null;
                do {
                    final Continuation<FluentComponent> browse = mavenContentFacet.components().browse(SEARCH_COMPONENT_PAGE_SIZE, continuationToken);
                    continuationToken = validateDatabaseAssets(configuration, listOfFailures, filter, toDeploy, browse, log);
                }  while (StringUtils.isNotBlank(continuationToken)); // fixme Is this correct end of the continuation?
            }
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
    private String validateDatabaseAssets(MavenCentralDeployTaskConfiguration configuration, List<FailedCheck> listOfFailures, Filter filter, List<Component> toDeploy, Continuation<FluentComponent> browse, Logger log) {
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
}
