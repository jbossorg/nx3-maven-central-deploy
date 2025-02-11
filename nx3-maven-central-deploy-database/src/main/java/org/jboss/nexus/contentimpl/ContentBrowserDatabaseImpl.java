package org.jboss.nexus.contentimpl;

import com.google.common.base.Preconditions;
import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.service.TagService;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.Filter;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
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
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.*;

import static org.jboss.nexus.MavenCentralDeploy.SEARCH_COMPONENT_PAGE_SIZE;

@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
@Named
public class ContentBrowserDatabaseImpl implements ContentBrowser {

    @Inject
    public ContentBrowserDatabaseImpl(SearchService searchService, Set<CentralValidation> validations, TagService tagService, SelectorManager selectorManager , VariableResolverAdapterManager variableResolverAdapterManager) {
        this.searchService = Preconditions.checkNotNull(searchService);
        this.validations = Preconditions.checkNotNull(validations);
        this.tagService = tagService; // we can ignore it if not needed
        this.selectorManager = Preconditions.checkNotNull(selectorManager);
        this.variableResolverAdapter = Preconditions.checkNotNull(variableResolverAdapterManager).get(Maven2Format.NAME);
    }

    private final SearchService searchService;

    private final Set<CentralValidation> validations;

    private final TagService tagService;
    private final SelectorManager selectorManager;

    private final VariableResolverAdapter variableResolverAdapter;

    @Override
    public void prepareValidationData(final Repository repository, final Filter filter, final MavenCentralDeployTaskConfiguration configuration, final List<FailedCheck> listOfFailures, final List<Component> toDeploy, Logger log) {
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
                }  while (StringUtils.isNotBlank(continuationToken));
            }
        } else
            log.error("Database implementation of repository browsing is running in wrong environment. See the installation guidelines and deploy the right jars!");
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

        Optional<SelectorConfiguration> selectorConfigurationOptional = StringUtils.isBlank(configuration.getContentSelector()) ? Optional.empty() : selectorManager.findByName(configuration.getContentSelector());

        long currentProcessingEpochTime = System.currentTimeMillis()/1000 - 60L * configuration.getProcessingTimeOffset();

        final SelectorConfiguration selectorConfiguration = selectorConfigurationOptional.orElse(null);

        browse.stream()
                .filter(fluentComponent ->  fluentComponent.created().toEpochSecond() > configuration.getLatestComponentTime() && fluentComponent.created().toEpochSecond() < currentProcessingEpochTime )
                .filter(filter::checkComponent)
                .forEach(
                fluentComponent -> {
                    try {
                        if(selectorConfiguration == null || fluentComponent.assets() != null & !fluentComponent.assets().isEmpty() && selectorManager.evaluate(selectorConfiguration, variableResolverAdapter.fromPath( fluentComponent.assets().iterator().next().path()  ,  Maven2Format.NAME)) ) {

                            List<SearchFilter> searchFilters = new ArrayList<>();
                            searchFilters.add(new SearchFilter("group.raw", fluentComponent.namespace()));
                            searchFilters.add(new SearchFilter("name.raw", fluentComponent.name()));
                            searchFilters.add(new SearchFilter("version", fluentComponent.version()));
                            searchFilters.add(new SearchFilter("repository_name", fluentComponent.repository().getName()));

                            SearchRequest request = SearchRequest.builder().searchFilters(searchFilters).build();
                            Iterable<ComponentSearchResult> components = this.searchService.browse(request);

                            Iterator<ComponentSearchResult> iterator = components.iterator();
                            if (iterator.hasNext()) {
                                ComponentSearchResult searchResult = iterator.next();
                                if (iterator.hasNext())
                                    throw new RuntimeException("Unexpected error: more than one " + fluentComponent.toStringExternal() + " found!");

                                final Component component = new ComponentDatabaseImpl(searchResult, fluentComponent, this);
                                if (filter.checkComponentTag(component)) {
                                    log.info("Validating component: " + component.toStringExternal());
                                    toDeploy.add(component);

                                    for (CentralValidation validation : validations) {
                                        validation.validateComponent(configuration, component, listOfFailures);
                                    }
                                }
                            } else
                                throw new RuntimeException("Unexpected error: Unable to find component " + fluentComponent.toStringExternal() + "!");
                        }
                    }  catch (SelectorEvaluationException e) {
                        log.error("Error evaluating selector configuration: "+e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }
        );
        return browse.nextContinuationToken();
    }


    private final Map<String, Tag> tags = new HashMap<>();

    /** Find tag for the given name
     *
     * @param tagName name of the tag
     * @return the tag
     */
    Tag findTag(@NotNull String tagName) {
        if(tagService == null)
            return null;

        return tags.computeIfAbsent(tagName, tagService::get);
    }
}
