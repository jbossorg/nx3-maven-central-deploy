package org.jboss.nexus.contentimpl;

import com.sonatype.nexus.tags.service.TagService;
import org.jboss.nexus.Filter;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Component;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.internal.MavenVariableResolverAdapter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import java.time.OffsetDateTime;
import java.util.*;

import static org.jboss.nexus.MavenCentralDeploy.SEARCH_COMPONENT_PAGE_SIZE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContentBrowserDatabaseImplTest {

    private ContentBrowserDatabaseImpl tested;

    private Set<CentralValidation> validations;

    @Mock
    private Repository repository;

    private MavenCentralDeployTaskConfiguration configuration = new MavenCentralDeployTaskConfiguration();

    private List<FailedCheck> listOfFailures;

    private List<Component> toDeploy;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private SearchService searchService;

    @Mock
    private SelectorManager selectorManager;

    private final TagService tagService = null; // We do not need this

    @Mock
    private MavenContentFacet mavenContentFacet;

    @Mock
    private VariableResolverAdapterManager variableResolverAdapterManager;




    private static int idCounter = 100;

    @Before
    public void setup() {
        validations = new HashSet<>();
        listOfFailures = new ArrayList<>();
        toDeploy = new ArrayList<>();
        configuration = new MavenCentralDeployTaskConfiguration();

        when(variableResolverAdapterManager.get(anyString())).thenReturn(new MavenVariableResolverAdapter(new Maven2MavenPathParser()));

        when(repository.optionalFacet(MavenContentFacet.class)).thenReturn( java.util.Optional.of(mavenContentFacet));
        when(repository.getName()).thenReturn("some-repository");

        when(searchService.browse(any(SearchRequest.class))).thenAnswer((Answer<Iterable<ComponentSearchResult>>) invocation -> {
            SearchRequest searchRequest = invocation.getArgument(0);

            ComponentSearchResult result = new ComponentSearchResult() ;
            searchRequest.getSearchFilters().forEach(parameter -> {
                switch ((parameter.getProperty())) {
                    case "format":
                        result.setFormat(parameter.getValue());
                        break;
                    case "group.raw":
                        result.setGroup(parameter.getValue());
                        break;
                    case "version":
                        result.setVersion(parameter.getValue());
                        break;
                    case "repository_name":
                        result.setRepositoryName(parameter.getValue());
                        break;
                    case "name.raw":
                        result.setName(parameter.getValue());
                        break;
                }
            });
            result.setFormat("maven2");
            result.setId(String.valueOf(idCounter++));

            return Collections.singleton(result);
        });

        tested = new ContentBrowserDatabaseImpl(searchService, validations, tagService, selectorManager, variableResolverAdapterManager);
    }


    @Test
    public void prepareValidationDataNoData() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        FluentQuery<FluentComponent> fluentQuery = getFluentQuery(new ArrayList<>());

        FluentComponents fluentComponentsMock = mock(FluentComponents.class);
        when(fluentComponentsMock.byFilter(anyString(), any())).thenReturn(fluentQuery);
        when(mavenContentFacet.components()).thenReturn(fluentComponentsMock);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertTrue(toDeploy.isEmpty());
    }


    @Test
    public void prepareValidationDataMultiplePages() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        ArrayList<FluentComponent> testComponents = getTestComponents();
        FluentQuery<FluentComponent> fluentQuery = getFluentQuery(testComponents);

        FluentComponents fluentComponentsMock = mock(FluentComponents.class);
        when(fluentComponentsMock.byFilter(anyString(), any())).thenReturn(fluentQuery);
        when(mavenContentFacet.components()).thenReturn(fluentComponentsMock);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertEquals(testComponents.size() , toDeploy.size());

        assertTrue(toDeploy.stream().anyMatch(component -> "version-3".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-5".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-7".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-9".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> ("version-"+ (testComponents.size()-1)).equals(component.version())) );

    }

    @Test
    public void prepareValidationDataSelectorUsed() {
        Filter filter =  Filter.parseFilterString(null); // no filtering by filter

        ArrayList<FluentComponent> testComponents = getTestComponents();

        FluentComponents fluentComponentsMock = mock(FluentComponents.class);
        when(mavenContentFacet.components()).thenReturn(fluentComponentsMock);
        when(fluentComponentsMock.browse(anyInt(), nullable(String.class))).thenReturn(getFluentQuery(testComponents).browse(testComponents.size(), null )); // let us emulate simple continuation

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertEquals(testComponents.size() , toDeploy.size()); // no filter = all components pass

        assertTrue(toDeploy.stream().anyMatch(component -> "version-3".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-5".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-7".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-9".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> ("version-"+ (testComponents.size()-1)).equals(component.version())) );

        SelectorConfiguration selectorConfiguration = mock(SelectorConfiguration.class);
        when(selectorConfiguration.getName()).thenReturn("TestSelector");
//        when(selectorConfiguration.getType()).thenReturn("csel");
//        when(selectorConfiguration.getAttributes()).thenReturn(Collections.singletonMap("expression", "expression -> path =^ \"/org/jboss/nexus/blah-blah\""));

        configuration.setString(MavenCentralDeployTaskConfiguration.CONTENT_SELECTOR, selectorConfiguration.getName());
        when(selectorManager.findByName(eq(selectorConfiguration.getName()))).thenReturn(Optional.of(selectorConfiguration));

        listOfFailures.clear();
        toDeploy.clear();

        tested = new ContentBrowserDatabaseImpl(searchService, validations, tagService, selectorManager, variableResolverAdapterManager); // apply the selector change

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertEquals(0, toDeploy.size()); // we do not have a way of testing actual expression validation so all the components are filtered out by the filter
    }



    @NotNull
    private ArrayList<FluentComponent> getTestComponents() {
        ArrayList<FluentComponent> result = new ArrayList<>();

        final int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;
        for(int i = 0; i < componentCount; i++ ) {
            FluentComponent fluentComponent = mock(FluentComponent.class);
            when(fluentComponent.name()).thenReturn("some-artifact");
            String version = "version-" + i;
            when(fluentComponent.version()).thenReturn(version);
            when(fluentComponent.namespace()).thenReturn("org.jboss.nexus");
            when(fluentComponent.repository()).thenReturn(repository);
            when(fluentComponent.created()).thenReturn(OffsetDateTime.now().minusHours(1));
            when(fluentComponent.toStringExternal()).thenCallRealMethod();


            FluentAsset asset = mock(FluentAsset.class);
            when(asset.path()).thenReturn("/org/jboss/nexus/some-artifact/"+version+"/some-artifact-"+version+".jar");
            when(fluentComponent.assets()).thenReturn(Collections.singleton(asset));
            result.add(fluentComponent);
        }

        return result;
    }


    @Test
    public void prepareValidationDataRemoveAlreadyDeployed() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        ArrayList<FluentComponent> testComponents = getTestComponents();

        // last deployed time 30 minutes ago
        configuration.setLatestComponentTime(  String.valueOf (OffsetDateTime.now().minusMinutes(30).toEpochSecond() ));

        // ignore too fresh artifacts, so we have enough time, so it does not affect us
        configuration.setProcessingTimeOffset(10);

        OffsetDateTime artifactTime = OffsetDateTime.now().minusMinutes(25);

        FluentComponent mockedComponent = testComponents.get(5);
        String deployedVersion1 = mockedComponent.version();
        when(mockedComponent.created()).thenReturn(  artifactTime  ); // 10 minutes is default time for ignoring components

        mockedComponent = testComponents.get(8);
        String deployedVersion2 = mockedComponent.version();
        when(mockedComponent.created()).thenReturn(  artifactTime  ); // 10 minutes is default time for ignoring components

        FluentQuery<FluentComponent> fluentQuery = getFluentQuery(testComponents);

        FluentComponents fluentComponentsMock = mock(FluentComponents.class);
        when(fluentComponentsMock.byFilter(anyString(), any())).thenReturn(fluentQuery);
        when(mavenContentFacet.components()).thenReturn(fluentComponentsMock);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertEquals(2, toDeploy.size());

        assertTrue(toDeploy.stream().anyMatch(component -> deployedVersion1.equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> deployedVersion2.equals(component.version())) );
        assertTrue("Already deployed", toDeploy.stream().noneMatch( component -> "version-3".equals(component.version())) );
        assertTrue("Already deployed", toDeploy.stream().noneMatch(component -> "version-4".equals(component.version())) );
        assertTrue("Already deployed", toDeploy.stream().noneMatch(component -> "version-7".equals(component.version())) );
        assertTrue("Already deployed", toDeploy.stream().noneMatch(component -> "version-9".equals(component.version())) );
        assertTrue("Already deployed", toDeploy.stream().noneMatch(component -> ("version-"+ (testComponents.size()-1)).equals(component.version())) );

    }

    @Test
    public void prepareValidationDataRemoveTooFreshComponents() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        ArrayList<FluentComponent> testComponents = getTestComponents();

        FluentComponent mockedComponent = testComponents.get(2);
        String deployedVersion = mockedComponent.version();

        when(mockedComponent.created()).thenReturn(OffsetDateTime.now().minusSeconds(10)); // 10 minutes is default time for ignoring components

        FluentQuery<FluentComponent> fluentQuery = getFluentQuery(testComponents);

        FluentComponents fluentComponentsMock = mock(FluentComponents.class);
        when(fluentComponentsMock.byFilter(anyString(), any())).thenReturn(fluentQuery);
        when(mavenContentFacet.components()).thenReturn(fluentComponentsMock);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertEquals(testComponents.size()-1 , toDeploy.size());

        assertTrue(toDeploy.stream().noneMatch(component -> deployedVersion.equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-3".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-5".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-7".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> "version-9".equals(component.version())) );
        assertTrue(toDeploy.stream().anyMatch(component -> ("version-"+ (testComponents.size()-1)).equals(component.version())) );

    }


    @Test
    public void prepareValidationDataRemoveFailingTest() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        ArrayList<FluentComponent> testComponents = getTestComponents();
        FluentQuery<FluentComponent> fluentQuery = getFluentQuery(testComponents);

        FluentComponents fluentComponentsMock = mock(FluentComponents.class);
        when(fluentComponentsMock.byFilter(anyString(), any())).thenReturn(fluentQuery);
        when(mavenContentFacet.components()).thenReturn(fluentComponentsMock);

        validations.add(new CentralValidation() {
            @Override
            public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<FailedCheck> listOfFailures) {
                listOfFailures.add(new FailedCheck(component, "Failed due to no reason."));
            }
        });

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertEquals(testComponents.size(),  listOfFailures.size());

        assertTrue(listOfFailures.stream().map(FailedCheck::getComponent).anyMatch(component -> "version-3".equals(component.version())) );
        assertTrue(listOfFailures.stream().map(FailedCheck::getComponent).anyMatch(component -> "version-5".equals(component.version())) );
        assertTrue(listOfFailures.stream().map(FailedCheck::getComponent).anyMatch(component -> "version-7".equals(component.version())) );
        assertTrue(listOfFailures.stream().map(FailedCheck::getComponent).anyMatch(component -> "version-9".equals(component.version())) );
        assertTrue(listOfFailures.stream().map(FailedCheck::getComponent).anyMatch(component -> ("version-"+ (testComponents.size()-1)).equals(component.version())) );



    }

    private static FluentQuery<FluentComponent> getFluentQuery(@NotNull final List<FluentComponent> testComponents) {
        return new FluentQuery<>() {
            final List<FluentComponent> components  = Collections.unmodifiableList(testComponents);

            @Override
            public int count() {
                return components.size();
            }

            @Override
            public Continuation<FluentComponent> browse(int limit, @Nullable String continuationToken) {
                 return getContinuation(testComponents, limit, continuationToken);
            }

            @Override
            public Continuation<FluentComponent> browseEager(int limit, @Nullable String continuationToken) {
                return browse(limit, continuationToken);
            }
        };
    }

    @Test
    public void testGetContinuationNoLimit() {
        List<FluentComponent> testComponents = getTestComponents();
        Continuation<FluentComponent> tested = getContinuation(testComponents, 0, null) ;

        assertEquals(testComponents.size(), tested.size());
        assertNull(tested.nextContinuationToken());
    }

    @Test
    public void testGetContinuationLimit() {
        List<FluentComponent> testComponents = getTestComponents();
        Continuation<FluentComponent> tested = getContinuation(testComponents, SEARCH_COMPONENT_PAGE_SIZE, null) ;

        assertEquals(SEARCH_COMPONENT_PAGE_SIZE, tested.size());
        assertEquals(continuationString(testComponents.get(SEARCH_COMPONENT_PAGE_SIZE)) , tested.nextContinuationToken());
        assertEquals(SEARCH_COMPONENT_PAGE_SIZE, tested.toArray().length);

        tested = getContinuation(testComponents, SEARCH_COMPONENT_PAGE_SIZE, tested.nextContinuationToken());
        assertEquals(SEARCH_COMPONENT_PAGE_SIZE, tested.size());
        assertEquals(continuationString(testComponents.get(SEARCH_COMPONENT_PAGE_SIZE*2)) , tested.nextContinuationToken());
        assertEquals(SEARCH_COMPONENT_PAGE_SIZE, tested.toArray().length);

        tested = getContinuation(testComponents, SEARCH_COMPONENT_PAGE_SIZE, tested.nextContinuationToken());
        assertEquals(SEARCH_COMPONENT_PAGE_SIZE, tested.size());
        assertEquals(continuationString(testComponents.get(SEARCH_COMPONENT_PAGE_SIZE*3)) , tested.nextContinuationToken());
        assertEquals(SEARCH_COMPONENT_PAGE_SIZE, tested.toArray().length);

        tested = getContinuation(testComponents, SEARCH_COMPONENT_PAGE_SIZE, tested.nextContinuationToken());
        assertEquals(tested.size() % SEARCH_COMPONENT_PAGE_SIZE , tested.size());
        assertNull(tested.nextContinuationToken());
        assertEquals(tested.size() % SEARCH_COMPONENT_PAGE_SIZE , tested.toArray().length);
    }




    @NotNull
    private static Continuation<FluentComponent> getContinuation(final List<FluentComponent> testComponents, int limit, final String continuationToken) {

        List<FluentComponent> components = Collections.emptyList();

        final int adjustedLimit;
        if(limit < 1) {
            adjustedLimit = Integer.MAX_VALUE;
        } else
            adjustedLimit = limit;

        if(continuationToken == null) {
            components = testComponents;
        } else {
            Iterator<FluentComponent> it = testComponents.iterator();
            int index = 0;

            while(it.hasNext()) {
                FluentComponent component = it.next();
                if(continuationToken.equals(continuationString(component))) {
                    components = testComponents.subList(index, testComponents.size());
                   break;
                }
                index++;
            }
        }

        final List<FluentComponent> comps = components;

        return new Continuation<>() {
            private final List<FluentComponent> content = comps;

             @Override
            public int size() {
                return Math.min(content.size(), adjustedLimit)   ;
            }

            @Override
            public boolean isEmpty() {
                return content.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return content.subList(0, size()).contains(o);
            }

            @NotNull
            @Override
            public Iterator<FluentComponent> iterator() {
                return content.subList(0, size()).iterator();
            }

            @NotNull
            @Override
            public Object[] toArray() {
                return content.subList(0, size()).toArray();
            }

            @NotNull
            @Override
            public <T> T[] toArray(@NotNull T[] a) {
                return content.subList(0, size()).toArray(a);
            }

            @Override
            public boolean add(FluentComponent fluentComponent) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(@NotNull Collection<?> c) {
                return content.subList(0, size()).containsAll(c);
            }

            @Override
            public boolean addAll(@NotNull Collection<? extends FluentComponent> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(@NotNull Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(@NotNull Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String nextContinuationToken() {
                if(adjustedLimit >= content.size()) {
                    return null;
                } else
                    return continuationString(content.get(adjustedLimit));
            }
        };
    }


    /** Returns implementation of calculating continuation string from the component, that is assumed to be the right one
     *
     * @param component assumed to be the "next" component
     *
     * @return string
     */
    private static String continuationString(FluentComponent component) {
        return component.toStringExternal();
    }

}
