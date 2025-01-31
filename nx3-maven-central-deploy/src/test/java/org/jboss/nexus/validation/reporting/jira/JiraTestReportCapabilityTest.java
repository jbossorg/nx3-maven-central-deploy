package org.jboss.nexus.validation.reporting.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("TextBlockMigration")
public class JiraTestReportCapabilityTest {

    private static final JsonMapper mapper = new JsonMapper();

    private JiraTestReportCapability jiraTestReportCapability;

    @Before
    public void setup() {
        jiraTestReportCapability = new JiraTestReportCapability(new JiraTestReportServerInformation(new JiraTestReportCapabilityDescriptor()));
    }

    @Test
    public void removeNotTranslatedVelocityVariablesNoChanges() throws JsonProcessingException {
        String content = "{\n" +
                "  \"fields\" : {\n" +
                "    \"issuetype\" : {\n" +
                "      \"id\" : \"1\",\n" +
                "      \"name\" : \"Bug\",\n" +
                "      \"subtask\" : false\n" +
                "    },\n" +
                "    \"project\" : {\n" +
                "      \"id\" : \"12318029\",\n" +
                "      \"key\" : \"NEXUS\",\n" +
                "      \"name\" : \"repository.jboss.org/nexus\"\n" +
                "    },\n" +
                "    \"fixVersions\" : [ ],\n" +
                "    \"customfield_12314740\" : \"{summaryBean=com.atlassian.jira.plugin.devstatus.rest.SummaryBean@230eb331[summary={pullrequest=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@23160098[overall=PullRequestOverallBean{stateCount=0, state='OPEN', details=PullRequestOverallDetails{openCount=0, mergedCount=0, declinedCount=0}},byInstanceType={}], build=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@280eb8ea[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BuildOverallBean@3b0f6fb3[failedBuildCount=0,successfulBuildCount=0,unknownBuildCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], review=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@40d59a1b[overall=com.atlassian.jira.plugin.devstatus.summary.beans.ReviewsOverallBean@46c557eb[stateCount=0,state=<null>,dueDate=<null>,overDue=false,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], deployment-environment=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@1b517e31[overall=com.atlassian.jira.plugin.devstatus.summary.beans.DeploymentOverallBean@5b63f81f[topEnvironments=[],showProjects=false,successfulCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], repository=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@378ab42a[overall=com.atlassian.jira.plugin.devstatus.summary.beans.CommitOverallBean@5b9cd73f[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], branch=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@4bac9a8b[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BranchOverallBean@f4b30e1[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}]},errors=[],configErrors=[]], devSummaryJson={\\\"cachedValue\\\":{\\\"errors\\\":[],\\\"configErrors\\\":[],\\\"summary\\\":{\\\"pullrequest\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":\\\"OPEN\\\",\\\"details\\\":{\\\"openCount\\\":0,\\\"mergedCount\\\":0,\\\"declinedCount\\\":0,\\\"total\\\":0},\\\"open\\\":true},\\\"byInstanceType\\\":{}},\\\"build\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"failedBuildCount\\\":0,\\\"successfulBuildCount\\\":0,\\\"unknownBuildCount\\\":0},\\\"byInstanceType\\\":{}},\\\"review\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":null,\\\"dueDate\\\":null,\\\"overDue\\\":false,\\\"completed\\\":false},\\\"byInstanceType\\\":{}},\\\"deployment-environment\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"topEnvironments\\\":[],\\\"showProjects\\\":false,\\\"successfulCount\\\":0},\\\"byInstanceType\\\":{}},\\\"repository\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}},\\\"branch\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}}}},\\\"isStale\\\":false}}\",\n" +
                "    \"workratio\" : -1,\n" +
                "    \"watches\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/issue/NEXUS-722/watchers\",\n" +
                "      \"watchCount\" : 0,\n" +
                "      \"isWatching\" : false\n" +
                "    },\n" +
                "    \"priority\" : {\n" +
                "      \"name\" : \"Major\",\n" +
                "      \"id\" : \"3\"\n" +
                "    },\n" +
                "    \"labels\" : [ ],\n" +
                "    \"versions\" : [ ],\n" +
                "    \"issuelinks\" : [ ],\n" +
                "    \"assignee\" : {\n" +
                "      \"name\" : \"dhladky@redhat.com\",\n" +
                "      \"key\" : \"dhladky\"\n" +
                "    },\n" +
                "    \"components\" : [ ],\n" +
                "    \"description\" : \"h2. MCD: KIE deployment number 0 on Wed Jun 07 15:10:35 CEST 2023\\r\\n\\r\\n{*}Processed{*}: 15 components from {_}product-old-releases{_}.\\r\\n{*}Problems found{*}: 6\\r\\nh2. Details\\r\\n\\r\\n||Group||Artifact||Version||File||Error||\\r\\n|org.jboss.failed|failed-1|1.2.3|org/jboss/failed/failed-1/1.2.3/failed-1-1.2.3.pom|ParseError at [row,col]:[1,104]Message: XML document structures must start and end within the same entity.|\\r\\n \\r\\n\",\n" +
                "    \"timetracking\" : { },\n" +
                "    \"attachment\" : [ ],\n" +
                "    \"customfield_12316542\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/customFieldOption/14655\",\n" +
                "      \"value\" : \"False\",\n" +
                "      \"id\" : \"14655\",\n" +
                "      \"disabled\" : false\n" +
                "    },\n" +
                "    \"customfield_12316543\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/customFieldOption/14657\",\n" +
                "      \"value\" : \"False\",\n" +
                "      \"id\" : \"14657\",\n" +
                "      \"disabled\" : false\n" +
                "    },\n" +
                "    \"customfield_12316544\" : \"None\",\n" +
                "    \"customfield_12310840\" : \"9223372036854775807\",\n" +
                "    \"summary\" : \"Maven Central Deploymet Plugin Validation\",\n" +
                "    \"subtasks\" : [ ],\n" +
                "    \"reporter\" : {\n" +
                "      \"name\" : \"dhladky@redhat.com\",\n" +
                "      \"key\" : \"dhladky\"\n" +
                "    },\n" +
                "    \"aggregateprogress\" : {\n" +
                "      \"progress\" : 0,\n" +
                "      \"total\" : 0\n" +
                "    },\n" +
                "    \"customfield_12311940\" : \"1|zbkl3o:\"\n" +
                "  }\n" +
                "}";
        JsonNode tested = mapper.readTree(content);

        String originalToString = tested.toString(); // exported version will have different formatting
        jiraTestReportCapability.removeNotTranslatedVelocityVariables(tested);
        String updatedToString = tested.toString();

        assertEquals(originalToString, updatedToString); // no changes should have been done
    }

    @Test
    public void removeNotTranslatedVelocityVariablesSomeVariables() throws JsonProcessingException {
        String content = "{\n" +
                "  \"fields\" : {\n" +
                "    \"issuetype\" : {\n" +
                "      \"id\" : \"${some_variable}\",\n" +
                "      \"name\" : \"Bug\",\n" +
                "      \"subtask\" : false\n" +
                "    },\n" +
                "    \"project\" : {\n" +
                "      \"id\" : \"12318029\",\n" +
                "      \"key\" : \"NEXUS\",\n" +
                "      \"name\" : \"repository.jboss.org/nexus\"\n" +
                "    },\n" +
                "    \"fixVersions\" : [\"${fixVersions}\" ],\n" +
                "    \"customfield_12314740\" : \"{summaryBean=com.atlassian.jira.plugin.devstatus.rest.SummaryBean@230eb331[summary={pullrequest=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@23160098[overall=PullRequestOverallBean{stateCount=0, state='OPEN', details=PullRequestOverallDetails{openCount=0, mergedCount=0, declinedCount=0}},byInstanceType={}], build=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@280eb8ea[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BuildOverallBean@3b0f6fb3[failedBuildCount=0,successfulBuildCount=0,unknownBuildCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], review=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@40d59a1b[overall=com.atlassian.jira.plugin.devstatus.summary.beans.ReviewsOverallBean@46c557eb[stateCount=0,state=<null>,dueDate=<null>,overDue=false,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], deployment-environment=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@1b517e31[overall=com.atlassian.jira.plugin.devstatus.summary.beans.DeploymentOverallBean@5b63f81f[topEnvironments=[],showProjects=false,successfulCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], repository=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@378ab42a[overall=com.atlassian.jira.plugin.devstatus.summary.beans.CommitOverallBean@5b9cd73f[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], branch=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@4bac9a8b[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BranchOverallBean@f4b30e1[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}]},errors=[],configErrors=[]], devSummaryJson={\\\"cachedValue\\\":{\\\"errors\\\":[],\\\"configErrors\\\":[],\\\"summary\\\":{\\\"pullrequest\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":\\\"OPEN\\\",\\\"details\\\":{\\\"openCount\\\":0,\\\"mergedCount\\\":0,\\\"declinedCount\\\":0,\\\"total\\\":0},\\\"open\\\":true},\\\"byInstanceType\\\":{}},\\\"build\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"failedBuildCount\\\":0,\\\"successfulBuildCount\\\":0,\\\"unknownBuildCount\\\":0},\\\"byInstanceType\\\":{}},\\\"review\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":null,\\\"dueDate\\\":null,\\\"overDue\\\":false,\\\"completed\\\":false},\\\"byInstanceType\\\":{}},\\\"deployment-environment\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"topEnvironments\\\":[],\\\"showProjects\\\":false,\\\"successfulCount\\\":0},\\\"byInstanceType\\\":{}},\\\"repository\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}},\\\"branch\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}}}},\\\"isStale\\\":false}}\",\n" +
                "    \"workratio\" : -1,\n" +
                "    \"watches\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/issue/NEXUS-722/watchers\",\n" +
                "      \"watchCount\" : 0,\n" +
                "      \"isWatching\" : false\n" +
                "    },\n" +
                "    \"priority\" : {\n" +
                "      \"name\" : \"Major\",\n" +
                "      \"id\" : \"3\"\n" +
                "    },\n" +
                "    \"labels\" : [\"${toDelete}\", \"label\" ],\n" +
                "    \"versions\" : [ ],\n" +
                "    \"issuelinks\" : [ ],\n" +
                "    \"assignee\" : {\n" +
                "      \"name\" : \"dhladky@redhat.com\",\n" +
                "      \"key\" : \"dhladky\"\n" +
                "    },\n" +
                "    \"components\" : [ ],\n" +
                "    \"description\" : \"h2. MCD: KIE deployment number 0 on Wed Jun 07 15:10:35 CEST 2023\\r\\n\\r\\n{*}Processed{*}: 15 components from {_}product-old-releases{_}.\\r\\n{*}Problems found{*}: 6\\r\\nh2. Details\\r\\n\\r\\n||Group||Artifact||Version||File||Error||\\r\\n|org.jboss.failed|failed-1|1.2.3|org/jboss/failed/failed-1/1.2.3/failed-1-1.2.3.pom|ParseError at [row,col]:[1,104]Message: XML document structures must start and end within the same entity.|\\r\\n \\r\\n\",\n" +
                "    \"timetracking\" : { },\n" +
                "    \"attachment\" : [ ],\n" +
                "    \"customfield_12316542\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/customFieldOption/14655\",\n" +
                "      \"value\" : \"False\",\n" +
                "      \"id\" : \"14655\",\n" +
                "      \"disabled\" : false\n" +
                "    },\n" +
                "    \"customfield_12316543\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/customFieldOption/14657\",\n" +
                "      \"value\" : \"False\",\n" +
                "      \"id\" : \"14657\",\n" +
                "      \"disabled\" : false\n" +
                "    },\n" +
                "    \"customfield_12316544\" : \"None\",\n" +
                "    \"customfield_12310840\" : \"9223372036854775807\",\n" +
                "    \"summary\" : \"Maven Central Deploymet Plugin Validation\",\n" +
                "    \"subtasks\" : [ ],\n" +
                "    \"reporter\" : {\n" +
                "      \"name\" : \"dhladky@redhat.com\",\n" +
                "      \"key\" : \"dhladky\"\n" +
                "    },\n" +
                "    \"aggregateprogress\" : {\n" +
                "      \"progress\" : 0,\n" +
                "      \"total\" : 0\n" +
                "    },\n" +
                "    \"customfield_12311940\" : \"1|zbkl3o:\"\n" +
                "  }\n" +
                "}";
        JsonNode tested = mapper.readTree(content);

        String originalToString = tested.toString(); // exported version will have different formatting
        jiraTestReportCapability.removeNotTranslatedVelocityVariables(tested);
        String updatedToString = tested.toString();

        JsonNode updatedJson = mapper.readTree(updatedToString);

        JsonNode issueType = updatedJson.get("fields").get("issuetype");

        assertEquals(2, issueType.size());
        assertFalse("ID should have been removed", issueType.has("id"));

        assertTrue("Component field was empty, but should not have been removed", updatedJson.get("fields").has("components"));

        assertFalse("The array should have been removed as the last piece was removed", updatedJson.get("fields").has("fixVersions"));

        assertEquals("One of the fields should have been removed.", 1, updatedJson.get("fields").get("labels").size());
    }

    @Test
    public void fixInvalidArrays() {
        String invalidJson = "{" +
                "\"fields\" : {" +
                "\"labels\" : [\"label1\" \"label2\", \"label3\" ]" +
                   "}" +
                "}";

        String result = JiraTestReportCapability.fixProblematicArrays(invalidJson);
        assertEquals("{" +
                "\"fields\" : {" +
                "\"labels\" : [\"label1\" ,\"label2\", \"label3\" ]" +
                "}" +
                "}", result);
    }

    @Test
    public void fixInvalidArraysNoOp() {
        String content = "{\n" +
                "  \"fields\" : {\n" +
                "    \"issuetype\" : {\n" +
                "      \"id\" : \"${some_variable}\",\n" +
                "      \"name\" : \"Bug\",\n" +
                "      \"subtask\" : false\n" +
                "    },\n" +
                "    \"project\" : {\n" +
                "      \"id\" : \"12318029\",\n" +
                "      \"key\" : \"NEXUS\",\n" +
                "      \"name\" : \"repository.jboss.org/nexus\"\n" +
                "    },\n" +
                "    \"fixVersions\" : [\"${fixVersions}\" ],\n" +
                "    \"customfield_12314740\" : \"{summaryBean=com.atlassian.jira.plugin.devstatus.rest.SummaryBean@230eb331[summary={pullrequest=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@23160098[overall=PullRequestOverallBean{stateCount=0, state='OPEN', details=PullRequestOverallDetails{openCount=0, mergedCount=0, declinedCount=0}},byInstanceType={}], build=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@280eb8ea[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BuildOverallBean@3b0f6fb3[failedBuildCount=0,successfulBuildCount=0,unknownBuildCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], review=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@40d59a1b[overall=com.atlassian.jira.plugin.devstatus.summary.beans.ReviewsOverallBean@46c557eb[stateCount=0,state=<null>,dueDate=<null>,overDue=false,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], deployment-environment=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@1b517e31[overall=com.atlassian.jira.plugin.devstatus.summary.beans.DeploymentOverallBean@5b63f81f[topEnvironments=[],showProjects=false,successfulCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], repository=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@378ab42a[overall=com.atlassian.jira.plugin.devstatus.summary.beans.CommitOverallBean@5b9cd73f[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], branch=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@4bac9a8b[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BranchOverallBean@f4b30e1[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}]},errors=[],configErrors=[]], devSummaryJson={\\\"cachedValue\\\":{\\\"errors\\\":[],\\\"configErrors\\\":[],\\\"summary\\\":{\\\"pullrequest\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":\\\"OPEN\\\",\\\"details\\\":{\\\"openCount\\\":0,\\\"mergedCount\\\":0,\\\"declinedCount\\\":0,\\\"total\\\":0},\\\"open\\\":true},\\\"byInstanceType\\\":{}},\\\"build\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"failedBuildCount\\\":0,\\\"successfulBuildCount\\\":0,\\\"unknownBuildCount\\\":0},\\\"byInstanceType\\\":{}},\\\"review\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":null,\\\"dueDate\\\":null,\\\"overDue\\\":false,\\\"completed\\\":false},\\\"byInstanceType\\\":{}},\\\"deployment-environment\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"topEnvironments\\\":[],\\\"showProjects\\\":false,\\\"successfulCount\\\":0},\\\"byInstanceType\\\":{}},\\\"repository\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}},\\\"branch\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}}}},\\\"isStale\\\":false}}\",\n" +
                "    \"workratio\" : -1,\n" +
                "    \"watches\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/issue/NEXUS-722/watchers\",\n" +
                "      \"watchCount\" : 0,\n" +
                "      \"isWatching\" : false\n" +
                "    },\n" +
                "    \"priority\" : {\n" +
                "      \"name\" : \"Major\",\n" +
                "      \"id\" : \"3\"\n" +
                "    },\n" +
                "    \"labels\" : [\"${toDelete}\", \"label\" ],\n" +
                "    \"versions\" : [ ],\n" +
                "    \"issuelinks\" : [ ],\n" +
                "    \"assignee\" : {\n" +
                "      \"name\" : \"dhladky@redhat.com\",\n" +
                "      \"key\" : \"dhladky\"\n" +
                "    },\n" +
                "    \"components\" : [ ],\n" +
                "    \"description\" : \"h2. MCD: KIE deployment number 0 on Wed Jun 07 15:10:35 CEST 2023\\r\\n\\r\\n{*}Processed{*}: 15 components from {_}product-old-releases{_}.\\r\\n{*}Problems found{*}: 6\\r\\nh2. Details\\r\\n\\r\\n||Group||Artifact||Version||File||Error||\\r\\n|org.jboss.failed|failed-1|1.2.3|org/jboss/failed/failed-1/1.2.3/failed-1-1.2.3.pom|ParseError at [row,col]:[1,104]Message: XML document structures must start and end within the same entity.|\\r\\n \\r\\n\",\n" +
                "    \"timetracking\" : { },\n" +
                "    \"attachment\" : [ ],\n" +
                "    \"customfield_12316542\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/customFieldOption/14655\",\n" +
                "      \"value\" : \"False\",\n" +
                "      \"id\" : \"14655\",\n" +
                "      \"disabled\" : false\n" +
                "    },\n" +
                "    \"customfield_12316543\" : {\n" +
                "      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/customFieldOption/14657\",\n" +
                "      \"value\" : \"False\",\n" +
                "      \"id\" : \"14657\",\n" +
                "      \"disabled\" : false\n" +
                "    },\n" +
                "    \"customfield_12316544\" : \"None\",\n" +
                "    \"customfield_12310840\" : \"9223372036854775807\",\n" +
                "    \"summary\" : \"Maven Central Deploymet Plugin Validation\",\n" +
                "    \"subtasks\" : [ ],\n" +
                "    \"reporter\" : {\n" +
                "      \"name\" : \"dhladky@redhat.com\",\n" +
                "      \"key\" : \"dhladky\"\n" +
                "    },\n" +
                "    \"aggregateprogress\" : {\n" +
                "      \"progress\" : 0,\n" +
                "      \"total\" : 0\n" +
                "    },\n" +
                "    \"customfield_12311940\" : \"1|zbkl3o:\"\n" +
                "  }\n" +
                "}";


        String result = JiraTestReportCapability.fixProblematicArrays(content);

        assertEquals(content, result);
    }

    @Test
    public void convertVariables() {

        // test default values from the capability
        Map<String, Object> printVariables = Collections.emptyMap();
        MavenCentralDeployTaskWithJiraConfiguration mavenCentralDeployTaskConfiguration = new MavenCentralDeployTaskWithJiraConfiguration();
        JiraTestReportCapabilityConfiguration jiraTestReportCapabilityConfiguration = new JiraTestReportCapabilityConfiguration();

        jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().setComponents("some component");
        jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().setReporter("some reporter");
        jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().setProject("project");
        jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().setAssignee("some assignee");
        jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().setPriority("some priority");
        jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().setLabels("label1, label2");

        Map<String, Object> testResult = jiraTestReportCapability.convertVariables(printVariables,  mavenCentralDeployTaskConfiguration,  jiraTestReportCapabilityConfiguration  );

        assertEquals("some component", testResult.get(JiraTestReportCapabilityConfiguration.COMPONENTS));
        assertEquals("some reporter", testResult.get(JiraTestReportCapabilityConfiguration.REPORTER));
        assertEquals("project", testResult.get(JiraTestReportCapabilityConfiguration.PROJECT));
        assertEquals("some assignee", testResult.get(JiraTestReportCapabilityConfiguration.ASSIGNEE));
        assertEquals("some priority", testResult.get(JiraTestReportCapabilityConfiguration.PRIORITY));
        assertEquals("label1, label2", testResult.get(JiraTestReportCapabilityConfiguration.LABELS));



        // test we override default values with task specific content
        mavenCentralDeployTaskConfiguration.setComponents("another components");
        mavenCentralDeployTaskConfiguration.setReporter("another reporter");
        mavenCentralDeployTaskConfiguration.setProject("another project");
        mavenCentralDeployTaskConfiguration.setAssignee("another assignee");
        mavenCentralDeployTaskConfiguration.setPriority("another priority");
        mavenCentralDeployTaskConfiguration.setLabels("label3");

        testResult = jiraTestReportCapability.convertVariables(printVariables,  mavenCentralDeployTaskConfiguration,  jiraTestReportCapabilityConfiguration  );
        assertEquals("another components", testResult.get(JiraTestReportCapabilityConfiguration.COMPONENTS));
        assertEquals("another reporter", testResult.get(JiraTestReportCapabilityConfiguration.REPORTER));
        assertEquals("another project", testResult.get(JiraTestReportCapabilityConfiguration.PROJECT));
        assertEquals("another assignee", testResult.get(JiraTestReportCapabilityConfiguration.ASSIGNEE));
        assertEquals("another priority", testResult.get(JiraTestReportCapabilityConfiguration.PRIORITY));
        assertEquals("label3", testResult.get(JiraTestReportCapabilityConfiguration.LABELS));

        // we remove component from task local components while keeping different default and local project
        mavenCentralDeployTaskConfiguration.setComponents(null);
        testResult = jiraTestReportCapability.convertVariables(printVariables,  mavenCentralDeployTaskConfiguration,  jiraTestReportCapabilityConfiguration  );

        assertEquals( "", testResult.get(JiraTestReportCapabilityConfiguration.COMPONENTS));
        assertEquals("another reporter", testResult.get(JiraTestReportCapabilityConfiguration.REPORTER));
        assertEquals("another project", testResult.get(JiraTestReportCapabilityConfiguration.PROJECT));
        assertEquals("another assignee", testResult.get(JiraTestReportCapabilityConfiguration.ASSIGNEE));
        assertEquals("another priority", testResult.get(JiraTestReportCapabilityConfiguration.PRIORITY));
        assertEquals("label3", testResult.get(JiraTestReportCapabilityConfiguration.LABELS));
    }

}
