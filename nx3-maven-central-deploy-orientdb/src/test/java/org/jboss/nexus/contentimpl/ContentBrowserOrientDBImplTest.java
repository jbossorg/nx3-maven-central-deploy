package org.jboss.nexus.contentimpl;

import org.jboss.nexus.Filter;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Component;
import org.jboss.nexus.content.ContentBrowserOrientDBImpl;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;

import java.util.*;

import org.slf4j.Logger;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.storage.Asset;

import static org.jboss.nexus.MavenCentralDeploy.SEARCH_COMPONENT_PAGE_SIZE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContentBrowserOrientDBImplTest {

    private ContentBrowserOrientDBImpl tested;

    @Mock
    private BlobStoreManager blobStoreManager = mock(BlobStoreManager.class);

    @Mock
    private BrowseService browseService = mock(BrowseService.class);

    private Set<CentralValidation> validations;

    @Mock
    private Repository repository;

    private MavenCentralDeployTaskConfiguration configuration = new MavenCentralDeployTaskConfiguration();

    private List<FailedCheck> listOfFailures;

    private List<Component> toDeploy;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Before
    public void setup() {
        validations = new HashSet<>();
        listOfFailures = new ArrayList<>();
        toDeploy = new ArrayList<>();
        configuration = new MavenCentralDeployTaskConfiguration();

        tested = new ContentBrowserOrientDBImpl(blobStoreManager, browseService, validations);

        // setup repository
        // when(repository.getName()).thenReturn("repository-name");


    }


    @Test
    public void prepareValidationDataNoData() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        PageResult<org.sonatype.nexus.repository.storage.Component> result = new PageResult<>(0, new ArrayList<>());
        when(browseService.browseComponents(any(), any())).thenReturn(result);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertTrue(toDeploy.isEmpty());
    }

    @Test
    public void prepareValidationDataMultiplePages() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;

        prepareComponents(componentCount);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );
        
        assertTrue(listOfFailures.isEmpty());
        assertEquals(componentCount,  toDeploy.size());

        for(int i = 0; i < componentCount; i++) {
            assertEquals("org.jboss.nexus", toDeploy.get(i).group());
            assertEquals("test-artifact", toDeploy.get(i).name());
            assertEquals("version-"+i, toDeploy.get(i).version());
        }
    }

    private void prepareComponents(int componentCount) {
        org.sonatype.nexus.repository.storage.Component[] components =  new org.sonatype.nexus.repository.storage.Component[componentCount];
        for (int i = 0; i < components.length; i++) {
            components[i]  = mockComponent(i);
        }

        for(int i = 0; i < componentCount; i += SEARCH_COMPONENT_PAGE_SIZE) {
            when(browseService.browseComponents(any(), any(QueryOptions.class))).thenAnswer(
                    (Answer<PageResult<org.sonatype.nexus.repository.storage.Component>>) invocation -> {
                        QueryOptions queryOptions = invocation.getArgument(1);

                        int start = queryOptions.getStart() == null ? 0 : queryOptions.getStart();

                        if(start >= componentCount)
                            return new PageResult<>(0, new ArrayList<>());

                        @SuppressWarnings("DataFlowIssue")
                        ArrayList<org.sonatype.nexus.repository.storage.Component> componentsInResponse = new ArrayList<>(Arrays.asList(components).subList(start,  Math.min(componentCount, start + queryOptions.getLimit())));

                        return new PageResult<>(queryOptions, componentsInResponse);
                    }
            );
        }

        when(browseService.browseComponentAssets(any(Repository.class), any(org.sonatype.nexus.repository.storage.Component.class))).thenReturn(new PageResult<>(0, new ArrayList<>()));
    }

    /** Prepares a mocked component
     *
     * @param entityNumber a number to become an identifier of the component
     * @return  mocked component entity
     */
    private static org.sonatype.nexus.repository.storage.Component mockComponent(int entityNumber) {
        org.sonatype.nexus.repository.storage.Component comp = mock(org.sonatype.nexus.repository.storage.Component.class);
        when(comp.group()).thenReturn("org.jboss.nexus");
        when(comp.requireGroup()).thenReturn("org.jboss.nexus");
        when(comp.version()).thenReturn("version-"+entityNumber); // enable the stubbing if needed
        when(comp.requireVersion()).thenReturn("version-"+ entityNumber);
        when(comp.name()).thenReturn("test-artifact");

        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.getId()).thenReturn(new DetachedEntityId(String.valueOf(entityNumber)));

        when(comp.getEntityMetadata()).thenReturn(entityMetadata);
        return comp;
    }


    @Test
    public void prepareValidationDataFilterComponents() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus&version!=version-5");

        int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;

        prepareComponents(componentCount);

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertEquals(componentCount-1,  toDeploy.size());

        for (Component component : toDeploy) {
            assertEquals("org.jboss.nexus", component.group());
            assertEquals("test-artifact", component.name());
            assertNotEquals("version-5", component.version());
        }
    }

    @Test
    public void prepareValidationDataRemoveAlreadyDeployed() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;

        prepareComponents(componentCount);

        configuration.setLatestComponentTime(String.valueOf(10)); // default time of empty component is 0, which is less than this

        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertTrue(listOfFailures.isEmpty());
        assertTrue(toDeploy.isEmpty());
    }

    @Test
    public void prepareValidationDataRemoveTooFreshComponents() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;

        prepareComponents(componentCount);

        ArrayList<Asset> assets = new ArrayList<>();

        assets.add(new Asset().blobCreated(new DateTime()).name("asset.txt").blobRef(new BlobRef("store", "blob")) );

        PageResult<Asset> assetPageResult = new PageResult<>(assets.size(), assets);

        when(browseService.browseComponentAssets(any(Repository.class), any(org.sonatype.nexus.repository.storage.Component.class))).thenReturn(assetPageResult);


        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log);

        assertTrue(listOfFailures.isEmpty());
        assertTrue(toDeploy.isEmpty());

    }


    @Test
    public void prepareValidationDataRemoveOldEnoughComponents() {

        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus");

        int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;

        prepareComponents(componentCount);

        ArrayList<Asset> assets = new ArrayList<>();

        assets.add(new Asset().blobCreated(DateTime.now().minusMinutes(configuration.getProcessingTimeOffset()+1)).name("asset.txt").blobRef(new BlobRef("store", "blob")) );

        PageResult<Asset> assetPageResult = new PageResult<>(assets.size(), assets);

        when(browseService.browseComponentAssets(any(Repository.class), any(org.sonatype.nexus.repository.storage.Component.class))).thenReturn(assetPageResult);


        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log);

        assertTrue(listOfFailures.isEmpty());
        assertEquals(componentCount,  toDeploy.size());
    }

    @Test
    public void prepareValidationDataRemoveFailingTest() {
        Filter filter =  Filter.parseFilterString("group=org.jboss.nexus&version!=version-5");

        int componentCount = SEARCH_COMPONENT_PAGE_SIZE * 3+2;

        prepareComponents(componentCount);


        validations.add(new CentralValidation() {
            @Override
            public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<org.jboss.nexus.content.Asset> assets, @NotNull List<FailedCheck> listOfFailures) {
                listOfFailures.add(new FailedCheck(component, "Failed due to no reason."));
            }
        });


        tested.prepareValidationData(repository, filter, configuration, listOfFailures, toDeploy, log );

        assertEquals(componentCount-1,  listOfFailures.size()); // one component should not have been tested

        HashSet<String> versions = new HashSet<>();
        for(FailedCheck failedCheck : listOfFailures) {
            assertNotEquals("version-5", failedCheck.getComponent().version());
            versions.add(failedCheck.getComponent().version());
            assertEquals("Failed due to no reason.", failedCheck.getProblem());
        }
        assertEquals("All versions should be distinct", componentCount-1, versions.size());
    }




}
