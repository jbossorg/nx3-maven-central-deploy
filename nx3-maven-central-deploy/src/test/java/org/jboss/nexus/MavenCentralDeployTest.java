package org.jboss.nexus;

import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.jboss.nexus.content.Component;
import org.jboss.nexus.content.ContentBrowser;
import org.jboss.nexus.tagging.MCDTagSetupConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import java.io.IOException;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.AUTOMATIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MavenCentralDeployTest{
    private MavenCentralDeploy mavenCentralDeploy;

    @Mock
    private RepositoryManager repositoryManager;

    private Set<TestReportCapability<?>> reports;

    @Mock
    private Repository testRepository;

    @Mock
    private TagStore tagStore;

    @Mock
    private TagService tagService;

    @Mock
    private Component testComponent;

    private MavenCentralDeployTaskConfiguration testConfiguration;

    private List<FailedCheck> failedChecks;
    private ContentBrowser contentBrowser;

    @Mock
    private ZipOutputStream zipOutputStream;

    private List<Component> okComponentsToDeploy;

    @SuppressWarnings("ExtractMethodRecommender")
    @Before
    public void setup() {
        //validations = new HashSet<>();
        reports = new HashSet<>();
        failedChecks = new ArrayList<>();

        contentBrowser = new FictiveContentBrowserWithErrors(failedChecks);

        mavenCentralDeploy = spy(new MavenCentralDeploy(repositoryManager, reports, tagStore, tagService, new TemplateRenderingHelper(), contentBrowser));

        try {
            doNothing().when(mavenCentralDeploy).publishArtifact(any(Component.class), any(ZipOutputStream.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        testConfiguration = new MavenCentralDeployTaskConfiguration();

        final Map<String, String> necessaryProperties = new HashMap<>();
        necessaryProperties.put(MavenCentralDeployTaskConfiguration.REPOSITORY, "test_repository");
        necessaryProperties.put(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_URL, "https://central.sonatype.org");
        necessaryProperties.put(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_PASSWORD, "password");
        necessaryProperties.put(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_MODE, AUTOMATIC);
        necessaryProperties.put(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_USER, "user");

        testConfiguration.addAll(necessaryProperties);


        when(repositoryManager.get("test_repository")).thenReturn(testRepository);

        okComponentsToDeploy = new ArrayList<>();
        okComponentsToDeploy.add(testComponent);
    }

    @Test
    public void processDeploymentMinimalRun() {
        okComponentsToDeploy.clear(); // nothing to deploy
        mavenCentralDeploy.processDeployment(testConfiguration);

        assertEquals("Processed 0 components.\n" +
            "- no errors were found.\n" +
            "- the deployment was a dry run (no actual publishing).", testConfiguration.getLatestStatus());
    }

    @Test
    public void processDeploymentNoTagService() {
        MavenCentralDeploy tested = new MavenCentralDeploy(repositoryManager, reports, null, null, new TemplateRenderingHelper(), contentBrowser);

        HashMap<String, String> tagConfiguration = new HashMap<>();
        tagConfiguration.put(MCDTagSetupConfiguration.DEPLOYED_TAG_NAME, "deployed");

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, true);

        MCDTagSetupConfiguration tagSetupConfiguration = new MCDTagSetupConfiguration(tagConfiguration);
        tested.registerConfiguration(tagSetupConfiguration);

        tested.processDeployment(testConfiguration);

        assertTrue( testConfiguration.getLatestStatus().contains("- Warning: Cannot mark synchronized artifacts! This version of Nexus does not support tagging."));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test(expected = RuntimeException.class)
    public void processDeploymentErrorsFound() throws IOException {

        failedChecks.addAll(TemplateRenderingHelper.generateFictiveErrors());

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try {
            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (Exception e) {
            assertEquals("Validations failed!", e.getMessage());
            verify(mavenCentralDeploy, never()).publishArtifact(any(), zipOutputStream);

            ArgumentCaptor<List> failedCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<Map> templateVariablesCaptor = ArgumentCaptor.forClass(Map.class);

            verify(report).createReport(eq(testConfiguration), failedCaptor.capture(), templateVariablesCaptor.capture());

            Map templateVariables = templateVariablesCaptor.getValue();
            assertEquals(1L, templateVariables.get(TemplateRenderingHelper.PROCESSED));
            List<FailedCheck> failedChecksSource = TemplateRenderingHelper.generateFictiveErrors();
            List<FailedCheck> failedChecksResult = failedCaptor.getValue();
            assertEquals(failedChecksSource.size(), failedChecksResult.size());

            assertTrue(failedChecksSource.stream().map(FailedCheck::getProblem).allMatch(problem ->  failedChecksResult.stream().anyMatch(result -> result.getProblem().equals(problem))))  ;

            throw e;
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processDeploymentSuccessDryRun() throws IOException {

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, true);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class));
        verify(report, never()).createReport(any(), any(), any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processDeploymentSuccessNoTags() throws IOException {

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);


        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // deployed once
        verify(report, never()).createReport(any(), any(), any());
    }


    @Test
    public void processDeploymentSuccessTagging() throws IOException {

        setupTagging();

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class));
        verify(mavenCentralDeploy).verifyTag(eq("deployTag"), nullable(String.class), any());
        verify(mavenCentralDeploy, never()).verifyTag(eq("failedTag"), nullable(String.class), any());
        verify(tagService).maybeAssociateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));
        verify(tagService, never()).maybeAssociateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));
    }

    @Test
    public void processDeploymentSuccessTaggingDisabled() throws IOException {

        setupTagging();

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, false);

        mavenCentralDeploy.processDeployment(testConfiguration);

        verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class));
        verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());
        verify(mavenCentralDeploy, never()).verifyTag(eq("failedTag"), nullable(String.class), any());
        verify(tagService, never()).maybeAssociateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));
        verify(tagService, never()).maybeAssociateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));
    }


    @Test(expected = RuntimeException.class)
    public void processDeploymentFailureTagging() throws IOException {
        setupTagging();

        failedChecks.addAll(TemplateRenderingHelper.generateFictiveErrors());

        try {
            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (Exception e) {
            assertEquals("Validations failed!", e.getMessage());

            verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class));
            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());


            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());
            verify(mavenCentralDeploy).verifyTag(eq("failedTag"), nullable(String.class), any());

            verify(tagService, never()).maybeAssociateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));

            verify(tagService, times(3)).maybeAssociateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));

            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void processDeploymentFailureTaggingDisabled() throws IOException {
        setupTagging();

        failedChecks.addAll(TemplateRenderingHelper.generateFictiveErrors());

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, false);


        try {
            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (Exception e) {
            assertEquals("Validations failed!", e.getMessage());

            verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class));
            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());

            verify(mavenCentralDeploy, never()).verifyTag(eq("deployTag"), nullable(String.class), any());
            verify(mavenCentralDeploy, never()).verifyTag(eq("failedTag"), nullable(String.class), any());

            verify(tagService, never()).maybeAssociateById(eq("deployTag"), eq(testRepository), nullable(EntityId.class));

            verify(tagService, never()).maybeAssociateById(eq("failedTag"), eq(testRepository), nullable(EntityId.class));

            throw e;
        }
    }



    private void setupTagging() throws IOException {
        HashMap<String, String> tagSetupMap = new HashMap<>();
        tagSetupMap.put(MCDTagSetupConfiguration.DEPLOYED_TAG_NAME, "deployTag");
        tagSetupMap.put(MCDTagSetupConfiguration.FAILED_TAG_NAME, "failedTag");

        MCDTagSetupConfiguration tagSetupConfiguration = new MCDTagSetupConfiguration(tagSetupMap);

        doNothing().when(mavenCentralDeploy).verifyTag(anyString(), nullable(String.class), any());
        doNothing().when(mavenCentralDeploy).publishArtifact(any(), any());

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, true);
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        mavenCentralDeploy.registerConfiguration(tagSetupConfiguration);
    }

    @Test
    public void verifyTagEmpty() {
        // TODO: 21.03.2023 what was this?
    }

    private static class TestTag implements Tag {

        private String name;
        private NestedAttributesMap attributes;

        private final DateTime firstCreated;

        private DateTime lastUpdated;

        public TestTag() {
            this.firstCreated = new DateTime();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Tag name(String s) {
            this.name = s;
            this.lastUpdated = new DateTime();
            return this;
        }

        @Override
        public NestedAttributesMap attributes() {
            return attributes;
        }

        @Override
        public Tag attributes(NestedAttributesMap nestedAttributesMap) {
            this.attributes = nestedAttributesMap;
            this.lastUpdated = new DateTime();
            return this;
        }

        @Override
        public DateTime firstCreated() {
            return firstCreated;
        }

        @Override
        public DateTime lastUpdated() {
            return lastUpdated;
        }
    }

    @Test
    public void verifyTagDoesNotExist() {
        final String tagName = "myTag";

        Tag testTag = new TestTag();
        when(tagStore.newTag()).thenReturn(testTag);

        mavenCentralDeploy.verifyTag(tagName, null, new TemplateRenderingHelper().generateTemplateParameters(testConfiguration, new ArrayList<>(), 5));

        verify(tagStore).create(same(testTag));
        assertTrue(testTag.attributes().isEmpty());
    }

    @Test
    public void verifyTagExistingTag() {
        final String tagName = "myTag";

        Tag testTag = new TestTag().attributes(new NestedAttributesMap());
        when(tagStore.get(tagName)).thenReturn(testTag);

        mavenCentralDeploy.verifyTag(tagName, null, new TemplateRenderingHelper().generateTemplateParameters(testConfiguration, new ArrayList<>(), 5));

        verify(tagStore, never()).create(same(testTag));
        verify(tagStore).update(same(testTag));
        assertTrue(testTag.attributes().isEmpty());
    }


    @Test
    public void verifyTagWithAttribute() {
        final String tagName = "myTag";

        Tag testTag = new TestTag();
        when(tagStore.newTag()).thenReturn(testTag);

        mavenCentralDeploy.verifyTag(tagName, "attribute=value", new TemplateRenderingHelper().generateTemplateParameters(testConfiguration, new ArrayList<>(), 5));

        verify(tagStore).create(same(testTag));

        assertEquals(1, testTag.attributes().size());
        assertTrue(testTag.attributes().contains("attribute"));
        assertEquals("value", testTag.attributes().get("attribute"));
    }

    @Test
    public void verifyTagMultipleAttributes() {
        final String tagName = "myTag";

        Tag testTag = new TestTag();
        when(tagStore.newTag()).thenReturn(testTag);

        mavenCentralDeploy.verifyTag(tagName, "attribute=value\n# comment here! \nanother=anotherValue", new TemplateRenderingHelper().generateTemplateParameters(testConfiguration, new ArrayList<>(), 5));

        verify(tagStore).create(same(testTag));

        assertEquals(2, testTag.attributes().size());
        assertTrue(testTag.attributes().contains("attribute"));
        assertEquals("value", testTag.attributes().get("attribute"));
        assertTrue(testTag.attributes().contains("another"));
        assertEquals("anotherValue", testTag.attributes().get("another"));
    }

    @Test
    public void verifyTagTemplateTagName() {
        final String tagName = "myTag-$variable1 and ${variable2}";

        Tag testTag = new TestTag();
        when(tagStore.newTag()).thenReturn(testTag);

        final Map<String, Object> taskConfiguration = new HashMap<>();
        taskConfiguration.put("variable1", "value1");
        taskConfiguration.put("variable2", "value2");

        mavenCentralDeploy.verifyTag(tagName, "attribute=$variable1\n# comment here! \nanother=${variable2}", taskConfiguration);

        assertEquals(2, testTag.attributes().size());
        assertTrue(testTag.attributes().contains("attribute"));
        assertEquals("value1", testTag.attributes().get("attribute"));
        assertTrue(testTag.attributes().contains("another"));
        assertEquals("value2", testTag.attributes().get("another"));
        assertEquals("myTag-value1 and value2", testTag.name());
    }

    // TODO: 2024-02-09 -  add the tests for these four cases:
    // 11:45:47.975 ERROR [main] org.jboss.nexus.MavenCentralDeploy - The artifacts can not be published. Username of the Maven Central account is missing!
    // 11:45:47.976 ERROR [main] org.jboss.nexus.MavenCentralDeploy - The artifacts can not be published. Password of the Maven Central account is missing!
    // 11:45:47.976 ERROR [main] org.jboss.nexus.MavenCentralDeploy - The artifacts can not be published. Deployment mode should either be USER_MANAGED or AUTOMATIC! It is null
    // 11:45:47.981 ERROR [main] org.jboss.nexus.MavenCentralDeploy - The artifacts can not be published. The URL of the Maven Central is not valid! The value is null

    // TODO: 2024-02-09 - test publish information with registered default configuration

    // TODO: 2024-02-09 - test the publish information being overridden by variable


    /** Helper class to mock the failing tests.
     *
     */
    private class FictiveContentBrowserWithErrors implements ContentBrowser {

        private final List<FailedCheck> failuresToReport;

        /** Constructor.
         *
         * @param failuresToReport failures that will be processed as found during {@link #prepareValidationData(Repository, Filter, MavenCentralDeployTaskConfiguration, List, List, Logger)}
         */
        private FictiveContentBrowserWithErrors(List<FailedCheck> failuresToReport) {
            this.failuresToReport = failuresToReport;
        }

        @Override
        public void prepareValidationData(Repository repository, Filter filter, MavenCentralDeployTaskConfiguration configuration, List<FailedCheck> listOfFailures, List<Component> toDeploy, Logger log) {
            listOfFailures.addAll(failuresToReport);
            toDeploy.addAll(okComponentsToDeploy);
        }
    }

}
