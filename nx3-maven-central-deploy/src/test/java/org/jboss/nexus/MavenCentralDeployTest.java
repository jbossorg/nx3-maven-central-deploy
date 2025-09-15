package org.jboss.nexus;

import com.sonatype.nexus.tags.Tag;
import com.sonatype.nexus.tags.TagStore;
import com.sonatype.nexus.tags.service.TagService;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.AUTOMATIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"FieldCanBeLocal", "unchecked", "rawtypes"})
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

    @Mock
    private HttpClientBuilder httpClientBuilder;

    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Mock
    private CloseableHttpClient closeableHttpClientStatus;

    @Mock
    private CloseableHttpResponse closeableHttpResponse1;

    @Mock
    private CloseableHttpResponse closeableHttpResponse2;
    @Mock
    private CloseableHttpResponse closeableHttpResponseStatus1, closeableHttpResponseStatus2;

    @Mock
    private StatusLine statusLine1, statusLine2;

    @Mock
    private StatusLine statusLineStatus1, statusLineStatus2;

    private MavenCentralDeployTaskConfiguration testConfiguration;

    private List<FailedCheck> failedChecks;
    private ContentBrowser contentBrowser;

    @Mock
    private ZipOutputStream zipOutputStream;

    private List<Component> okComponentsToDeploy;


    private final String errorSample1 = """
            {
               "deploymentId":"cbfe2fa8-c84c-4ec0-8e45-c71cdb5f6390",
               "deploymentName":"xcom",
               "deploymentState":"FAILED",
               "purls":[
                  "pkg:maven/xcom.sonatype.central.testing.david-hladky/kie-api@7.42.0.Final?type=bundle"
               ],
               "errors":{
                  "pkg:maven/xcom.sonatype.central.testing.david-hladky/kie-api@7.42.0.Final?type=bundle":[
                     "Invalid 'md5' checksum for file: kie-api-7.42.0.Final.pom",
                     "Invalid 'sha1' checksum for file: kie-api-7.42.0.Final.pom",
                     "Missing signature for file: kie-api-7.42.0.Final-javadoc.jar",
                     "Missing signature for file: kie-api-7.42.0.Final-sources.jar",
                     "Missing signature for file: kie-api-7.42.0.Final-test-sources.jar",
                     "Missing signature for file: kie-api-7.42.0.Final-tests.jar",
                     "Missing signature for file: kie-api-7.42.0.Final.jar",
                     "Missing signature for file: kie-api-7.42.0.Final.pom",
                     "Namespace 'xcom.sonatype.central.testing.david-hladky' is not allowed",
                     "Dependency version information is missing",
                     "Developers information is missing",
                     "License information is missing",
                     "Project URL is not defined",
                     "SCM URL is not defined"
                  ]
               },
               "cherryBomUrl":null
            }""";

    private final String pendingSample = """
            {
               "deploymentId":"cbfe2fa8-c84c-4ec0-8e45-c71cdb5f6390",
               "deploymentName":"com",
               "deploymentState":"PENDING",
               "purls":[
                  "pkg:maven/com.sonatype.central.testing.david-hladky/kie-api@7.42.0.Final?type=bundle"
               ],
               "errors":{ },
               "cherryBomUrl":null
            }""";

    private final String publishedSample = """
            {
               "deploymentId":"cbfe2fa8-c84c-4ec0-8e45-c71cdb5f6390",
               "deploymentName":"com",
               "deploymentState":"PUBLISHED",
               "purls":[
                  "pkg:maven/com.sonatype.central.testing.david-hladky/kie-api@7.42.0.Final?type=bundle"
               ],
               "errors":{ },
               "cherryBomUrl":null
            }""";


    @SuppressWarnings("ExtractMethodRecommender")
    @Before
    public void setup() throws IOException {
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

        when(httpClientBuilder.build())
                .thenReturn(closeableHttpClient).thenReturn(closeableHttpClient)
                .thenReturn(closeableHttpClientStatus);

        when(closeableHttpClient.execute(any(HttpUriRequest.class))) // responses for the first and the second call of upload endpoint
                .thenReturn(closeableHttpResponse1)
                .thenReturn(closeableHttpResponse2);

        when(closeableHttpResponse1.getStatusLine()).thenReturn(statusLine1);
        when(closeableHttpResponse2.getStatusLine()).thenReturn(statusLine2);

        when(statusLine1.getStatusCode()).thenReturn(401); // first call not authenticated
        when(statusLine2.getStatusCode()).thenReturn(201); // second call if successful

        HttpEntity httpIdEntity = mock(HttpEntity.class);
        when(httpIdEntity.getContent()).thenReturn(new ByteArrayInputStream("some-id".getBytes(StandardCharsets.UTF_8)));

        when(closeableHttpResponse2.getEntity()).thenReturn(httpIdEntity);

       // ---- status checking - first request Maven Central Processing, second request Maven Central published
       when(closeableHttpClientStatus.execute(any(HttpUriRequest.class))) // responses for the status calls
               .thenReturn(closeableHttpResponseStatus1)
               .thenReturn(closeableHttpResponseStatus2);

        when(closeableHttpResponseStatus1.getStatusLine()).thenReturn(statusLineStatus1);
        when(closeableHttpResponseStatus2.getStatusLine()).thenReturn(statusLineStatus2);

        when(statusLineStatus1.getStatusCode()).thenReturn(200);
        when(statusLineStatus2.getStatusCode()).thenReturn(200);

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(pendingSample.getBytes(StandardCharsets.UTF_8)));

        when(closeableHttpResponseStatus1.getEntity()).thenReturn(httpEntity);

        httpEntity = mock(HttpEntity.class);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(publishedSample.getBytes(StandardCharsets.UTF_8)));

        when(closeableHttpResponseStatus2.getEntity()).thenReturn(httpEntity);

    }

    @Test
    public void processDeploymentMinimalRun() {
        okComponentsToDeploy.clear(); // nothing to deploy
        mavenCentralDeploy.processDeployment(testConfiguration);

        assertEquals("""
                Processed 0 components.
                - no errors were found.
                - the deployment was a dry run (no actual publishing).""", testConfiguration.getLatestStatus());
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
    public void processDeploymentSuccessDryValidationTask() throws IOException {

        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.VALIDATION_TASK, true);

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

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            mavenCentralDeploy.processDeployment(testConfiguration);
        }

        verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // deployed once
        verify(report, never()).createReport(any(), any(), any());
    }


    @Test
    public void processDeploymentSuccessTagging() throws IOException {

        setupTagging();

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            mavenCentralDeploy.processDeployment(testConfiguration);
        }

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

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            mavenCentralDeploy.processDeployment(testConfiguration);
        }

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

    @SuppressWarnings("rawtypes")
    @Test
    public void verifyMavenCentralInformationIsMissing() throws IOException {
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        String value = testConfiguration.getCentralURL();
        testConfiguration.setCentralURL(null);
        mavenCentralDeploy.processDeployment(testConfiguration);
        verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // missing information
        testConfiguration.setCentralURL(value);

        value = testConfiguration.getCentralUser();
        testConfiguration.setCentralUser(null);
        mavenCentralDeploy.processDeployment(testConfiguration);
        verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // missing information
        testConfiguration.setCentralUser(value);

        value = testConfiguration.getCentralPassword();
        testConfiguration.setCentralPassword(null);
        mavenCentralDeploy.processDeployment(testConfiguration);
        verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // missing information
        testConfiguration.setCentralPassword(value);

        // the full information is present
        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            mavenCentralDeploy.processDeployment(testConfiguration);
        }
        verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // deployed once
    }


    @Test(expected = RuntimeException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processDeploymentMavenCentralConnectionFailureSSL() throws IOException {
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            when(closeableHttpClient.execute(any(HttpUriRequest.class)))
                    .thenThrow(new SSLException("Something went wrong with SSL"));


            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (RuntimeException e) {
            assertEquals("Validations failed!", e.getMessage());

            verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // not deploying - SSL error
            verify(closeableHttpClient).execute(any(HttpUriRequest.class)); // called just once
            verify(closeableHttpClientStatus, never()).execute(any(HttpUriRequest.class));
            verify(report).createReport(any(), any(), any());
            throw e;
        }


    }
    @Test(expected = RuntimeException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processDeploymentMavenCentralConnectionFailure500Error() throws IOException {
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            StatusLine statusLine500 = mock(StatusLine.class);
            when(statusLine500.getStatusCode()).thenReturn(500);

            when(closeableHttpResponse1.getStatusLine()).thenReturn(statusLine500);

            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (RuntimeException e) {
            assertEquals("Validations failed!", e.getMessage());
            verify(mavenCentralDeploy, never()).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // no attempt to deploy - access failed
            verify(closeableHttpClient).execute(any(HttpUriRequest.class)); // called just once
            verify(closeableHttpClientStatus, never()).execute(any(HttpUriRequest.class));
            verify(report).createReport(any(), any(), any()); // there needs to be a status error for failed push
            throw e;
        }
    }

    @Test
    public void processDeploymentDeploymentFullySuccessful() throws IOException {
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            mavenCentralDeploy.processDeployment(testConfiguration);
        }

        verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // deployed once
        verify(closeableHttpClient, times(2)).execute(any(HttpUriRequest.class));
        verify(closeableHttpClientStatus, times(2)).execute(any(HttpUriRequest.class));
        verify(report, never()).createReport(any(), any(), any());
    }

    @Test(expected = RuntimeException.class)
    public void processDeploymentAuthenticationError() throws IOException {
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            when(closeableHttpResponse2.getStatusLine()).thenReturn(statusLine1); // also call with credentials complains about wrong credentials

            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (RuntimeException e) {
            assertEquals("Validations failed!", e.getMessage());
            verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // deployed once
            verify(closeableHttpClient, times(2)).execute(any(HttpUriRequest.class)); // called once for ssl check and once with wrong credentials
            verify(closeableHttpClientStatus, never()).execute(any(HttpUriRequest.class));
            verify(report).createReport(any(), any(), any()); // there needs to be a status error for failed push
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void processDeploymentMavenCentralErrorsFound() throws IOException {
        testConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DRY_RUN, false);

        TestReportCapability report = mock(TestReportCapability.class);
        reports.add(report);

        try (MockedStatic<MavenCentralDeploy> mockedStatic = mockStatic(MavenCentralDeploy.class)) {
            mockedStatic.when(() -> MavenCentralDeploy.getHttpClientBuilder(nullable(String.class), nullable(Integer.class))).thenReturn(httpClientBuilder);

            HttpEntity httpEntity = mock(HttpEntity.class);
            when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(errorSample1.getBytes(StandardCharsets.UTF_8)));
            when(closeableHttpResponseStatus2.getEntity()).thenReturn(httpEntity);

            mavenCentralDeploy.processDeployment(testConfiguration);
        } catch (RuntimeException e) {
            assertEquals("Validations failed!", e.getMessage());
            verify(mavenCentralDeploy).publishArtifact(same(testComponent), any(ZipOutputStream.class)); // deployed once
            verify(closeableHttpClient, times(2)).execute(any(HttpUriRequest.class)); // called once for ssl check and once with wrong credentials
            verify(closeableHttpClientStatus, times(2)).execute(any(HttpUriRequest.class));
            verify(report).createReport(any(), any(), any()); // there needs to be a status error for failed push
            throw e;
        }

    }



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
