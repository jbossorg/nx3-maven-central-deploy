package org.jboss.nexus;

import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.jboss.nexus.tagging.MCDTagSetupConfiguration;
import org.jboss.nexus.validation.checks.CentralValidation;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MavenCentralDeployTest{

    private MavenCentralDeploy mavenCentralDeploy;

    @Mock
    private RepositoryManager repositoryManager;

    @Mock
    private BrowseService browseService;

    private Set<CentralValidation> validations;

    private Set<TestReportCapability<?>> reports;

    @Mock
    private Repository testRepository;

    @Mock
    private TagStore tagStore;

    @Mock
    private TagService tagService;

    @Mock
    private Component testComponent;

    @Mock
    private Asset testAsset;

    private MavenCentralDeployTaskConfiguration testConfiguration;



    @Before
    public void setup() {
        validations = new HashSet<>();
        reports = new HashSet<>();

        mavenCentralDeploy = spy(new MavenCentralDeploy(repositoryManager, browseService, validations, reports, tagStore, tagService,  new TemplateRenderingHelper()));

        testConfiguration = new MavenCentralDeployTaskConfiguration();

        Map<String, String> necessaryProperties = new HashMap<>();
        necessaryProperties.put(MavenCentralDeployTaskConfiguration.REPOSITORY, "test_repository");

        testConfiguration.addAll(necessaryProperties);

        when(repositoryManager.get("test_repository")).thenReturn(testRepository);
    }

    @Test
    public void processDeploymentMinimalRun() {
        mavenCentralDeploy.processDeployment(testConfiguration);

        assertEquals("Processed 0 components.\n" +
            "- no errors were found.\n" +
            "- the deployment was a dry run (no actual publishing).", testConfiguration.getLatestStatus());
    }

    @Test
    public void processDeploymentNoTagService() {
        MavenCentralDeploy tested = new MavenCentralDeploy(repositoryManager, browseService, validations, reports, null, null,  new TemplateRenderingHelper());

        HashMap<String, String> tagConfiguration = new HashMap<>();
        tagConfiguration.put(MCDTagSetupConfiguration.DEPLOYED_TAG_NAME, "deployed");

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, true);

        MCDTagSetupConfiguration tagSetupConfiguration = new MCDTagSetupConfiguration(tagConfiguration);
        tested.registerConfiguration(tagSetupConfiguration);

        tested.processDeployment(testConfiguration);

        assertTrue( testConfiguration.getLatestStatus().contains("- Warning: Cannot mark synchronized artifacts! This version of Nexus does not support tagging."));
    }

    /** The validation class, that always returns errors no matter what input it gets. */
    private static class AutoFailValidation extends CentralValidation {
        @Override
        public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<Asset> assets, @NotNull List<FailedCheck> listOfFailures) {
             listOfFailures.addAll(TemplateRenderingHelper.generateFictiveErrors());
        }
    }


    private void mockBrowseComponents() {
        PageResult<Component> pageResult = new PageResult<>(1, Collections.singletonList(testComponent));
        when(browseService.browseComponents(eq(testRepository), any())).thenReturn(pageResult).thenReturn(new PageResult<>(0, Collections.emptyList()));

        PageResult<Asset> pageResult1 = new PageResult<>(1, Collections.singletonList(testAsset));
        when(browseService.browseComponentAssets(testRepository, testComponent)).thenReturn(pageResult1);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test(expected = RuntimeException.class)
    public void processDeploymentErrorsFound() {
        mockBrowseComponents();
        validations.add(new AutoFailValidation());

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try {
            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (Exception e) {
            assertEquals("Validations failed!", e.getMessage());
            verify(mavenCentralDeploy, never()).publishArtifact(any());

            ArgumentCaptor<List> failedCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<Long> longCaptor = ArgumentCaptor.forClass(Long.class);

            verify(report).createReport(eq(testConfiguration), failedCaptor.capture(), longCaptor.capture());

            assertEquals(1L, longCaptor.getValue().longValue());
            List<FailedCheck> failedChecksSource = TemplateRenderingHelper.generateFictiveErrors();
            List<FailedCheck> failedChecksResult = failedCaptor.getValue();
            assertEquals(failedChecksSource.size(), failedChecksResult.size());

            assertTrue(failedChecksSource.stream().map(FailedCheck::getProblem).allMatch(problem ->  failedChecksResult.stream().anyMatch(result -> result.getProblem().equals(problem))))  ;

            throw e;
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processDeploymentSuccessDryRun() {
        mockBrowseComponents();

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, true);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy, never()).publishArtifact(testComponent);
        verify(report, never()).createReport(any(), any(), anyLong());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processDeploymentSuccessNoTags() {
        mockBrowseComponents();
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy).publishArtifact(testComponent); // deployed once
        verify(report, never()).createReport(any(), any(), anyLong());
    }


    @Test
    public void processDeploymentSuccessTagging() {
        setupTagging();

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy).publishArtifact(testComponent);
        verify(mavenCentralDeploy).verifyTag(eq("deployTag"), nullable(String.class), any());
        verify(mavenCentralDeploy, never()).verifyTag(eq("failedTag"), nullable(String.class), any());
        verify(tagService).associateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));
        verify(tagService, never()).associateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));
    }

    @Test
    public void processDeploymentSuccessTaggingDisabled() {
        setupTagging();

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, false);

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy).publishArtifact(testComponent);
        verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());
        verify(mavenCentralDeploy, never()).verifyTag(eq("failedTag"), nullable(String.class), any());
        verify(tagService, never()).associateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));
        verify(tagService, never()).associateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));
    }


    @Test(expected = RuntimeException.class)
    public void processDeploymentFailureTagging() {
        setupTagging();

        validations.add(new AutoFailValidation());

        try {
            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (Exception e) {
            assertEquals("Validations failed!", e.getMessage());

            verify(mavenCentralDeploy, never()).publishArtifact(testComponent);
            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());


            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());
            verify(mavenCentralDeploy).verifyTag(eq("failedTag"), nullable(String.class), any());

            verify(tagService, never()).associateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));

            verify(tagService, times(3)).associateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));

            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void processDeploymentFailureTaggingDisabled() {
        setupTagging();

        validations.add(new AutoFailValidation());

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, false);


        try {
            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (Exception e) {
            assertEquals("Validations failed!", e.getMessage());

            verify(mavenCentralDeploy, never()).publishArtifact(testComponent);
            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());

            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());
            verify(mavenCentralDeploy, never()).verifyTag(eq("failedTag"), nullable(String.class), any());

            verify(tagService, never()).associateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));

            verify(tagService, never()).associateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));

            throw e;
        }
    }



    private void setupTagging() {
        mockBrowseComponents();

        HashMap<String, String> tagSetupMap = new HashMap<>();
        tagSetupMap.put(MCDTagSetupConfiguration.DEPLOYED_TAG_NAME, "deployTag");
        tagSetupMap.put(MCDTagSetupConfiguration.FAILED_TAG_NAME, "failedTag");

        MCDTagSetupConfiguration tagSetupConfiguration = new MCDTagSetupConfiguration(tagSetupMap);

        doNothing().when(mavenCentralDeploy).verifyTag(anyString(), nullable(String.class), any());
        doNothing().when(mavenCentralDeploy).publishArtifact(any());

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, true);
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        mavenCentralDeploy.registerConfiguration(tagSetupConfiguration);

        when(testComponent.getEntityMetadata()).thenReturn(mock(EntityMetadata.class));
    }

    @Test
    public void verifyTagEmpty() {

        // TODO: 21.03.2023
    }

    @Test
    public void verifyTagNoAttribute() {
        // TODO: 21.03.2023
    }

    @Test
    public void verifyTagMultipleAttributes() {
        // TODO: 21.03.2023
    }

    @Test
    public void verifyTagTemplateAttribute() {
        // TODO: 21.03.2023
    }

}
