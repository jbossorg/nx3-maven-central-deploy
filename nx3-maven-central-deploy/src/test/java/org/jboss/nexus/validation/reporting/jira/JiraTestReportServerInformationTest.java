package org.jboss.nexus.validation.reporting.jira;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraTestReportServerInformationTest {

	JiraTestReportServerInformation tested;

	@Mock
	private HttpURLConnection mockedURLConnection;


	@Before
	public void setup() {
		tested = spy(new JiraTestReportServerInformation(new JiraTestReportCapabilityDescriptor()));
		tested.setJiraConnectionInformation("https://issues.something.org", null, "someToken", null, null, null);

		try {
			doReturn(mockedURLConnection).when(tested).buildConnection(anyString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void buildConnectionWithSlash() throws IOException {
		tested = new JiraTestReportServerInformation(new JiraTestReportCapabilityDescriptor());
		tested.setJiraConnectionInformation("https://issues.something.org/", null, "someToken", null, null, null);

		URLConnection urlConnection = tested.buildConnection("/endpoint");

		assertEquals("issues.something.org", urlConnection.getURL().getHost());
		assertEquals("/endpoint", urlConnection.getURL().getPath());
	}

	@Test
	public void buildConnectionWithoutSlash() throws IOException {
		tested = new JiraTestReportServerInformation(new JiraTestReportCapabilityDescriptor());
		tested.setJiraConnectionInformation("https://issues.something.org", null, "someToken", null, null, null);

		URLConnection urlConnection = tested.buildConnection("/endpoint");

		assertEquals("issues.something.org", urlConnection.getURL().getHost());
		assertEquals("/endpoint", urlConnection.getURL().getPath());
	}


	//@Test
	public void findSecurityIDReal() {
		tested = new JiraTestReportServerInformation(new JiraTestReportCapabilityDescriptor());
		tested.setJiraConnectionInformation("https://issues.stage.redhat.com", null, "", null,"squid.corp.redhat.com", 3128); // fixme credentials remove!

		tested.findIssueTypeID("Bug");


		// TODO: 28.03.2023 remove this!!!!!!
	}


	private static final String jiraSecurityResponse = "{\n" +
			"  \"levels\" : [ {\n" +
			"    \"self\" : \"https://issues.organization.com/rest/api/2/securitylevel/101\",\n" +
			"    \"id\" : \"101\",\n" +
			"    \"description\" : \"Only Company employees and contractors\",\n" +
			"    \"name\" : \"Company Internal\"\n" +
			"  }, {\n" +
			"    \"self\" : \"https://issues.organizations.com/rest/api/2/securitylevel/303\",\n" +
			"    \"id\" : \"303\",\n" +
			"    \"description\" : \"Only persons responsible to resolve security issues\",\n" +
			"    \"name\" : \"Security Issue\"\n" +
			"  } ]\n" +
			"}";

	@Test
	public void findSecurityLevelIDNumeric() throws IOException {

		Integer result = tested.findSecurityLevelID("NEXUS", "123456");
		assertEquals((Integer)123456, result);

		verify(mockedURLConnection, never()).getInputStream(); // just the number should be used
	}

	@Test
	public void findSecurityLevelID() throws IOException {

		when(mockedURLConnection.getInputStream())
				.thenReturn(new ByteArrayInputStream(jiraProjectResponse.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(jiraSecurityResponse.getBytes(StandardCharsets.UTF_8)));

		Integer result = tested.findSecurityLevelID("NEXUS", "Company Internal");
		assertEquals((Integer)101, result);

		tested.findSecurityLevelID("NEXUS", "Company Internal");
		tested.findSecurityLevelID("NEXUS", "Security Issue");

		verify(mockedURLConnection, times(2)).getInputStream(); // should be cached
	}

	@Test
	public void findSecurityLevelIDMultipleProjects() throws IOException {
		final String projectAnotherResponse = "{\n" +
				"   \"id\": \"777\",\n" +
				"   \"key\": \"ANOTHER\",\n" +
				"   \"name\": \"Another Project\"\n" +
				"}";

		final String anotherSecurityResponse = "{\n" +
				"  \"levels\" : [ {\n" +
				"    \"self\" : \"https://issues.organization.com/rest/api/2/securitylevel/404\",\n" +
				"    \"id\" : \"404\",\n" +
				"    \"description\" : \"Only Company employees and contractors\",\n" +
				"    \"name\" : \"Company Internal\"\n" +
				"  }, {\n" +
				"    \"self\" : \"https://issues.organizations.com/rest/api/2/securitylevel/909\",\n" +
				"    \"id\" : \"909\",\n" +
				"    \"description\" : \"Only persons responsible to resolve security issues\",\n" +
				"    \"name\" : \"Security Issue\"\n" +
				"  } ]\n" +
				"}";

		when(mockedURLConnection.getInputStream())
				.thenReturn(new ByteArrayInputStream(jiraProjectResponse.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(jiraSecurityResponse.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(projectAnotherResponse.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(anotherSecurityResponse.getBytes(StandardCharsets.UTF_8)));

		Integer result = tested.findSecurityLevelID("NEXUS", "Company Internal");
		assertEquals((Integer)101, result);

		assertEquals((Integer) 101, tested.findSecurityLevelID("NEXUS", "Company Internal"));
		assertEquals((Integer) 303, tested.findSecurityLevelID("NEXUS", "Security Issue"));
		assertEquals((Integer) 404, tested.findSecurityLevelID("ANOTHER", "Company Internal"));
		assertEquals((Integer) 909, tested.findSecurityLevelID("ANOTHER", "Security Issue"));

		verify(mockedURLConnection, times(4)).getInputStream(); // should be cached
	}

	@Test(expected = RuntimeException.class)
	public void findSecurityLevelIDNotFound() throws IOException {

		when(mockedURLConnection.getInputStream())
				.thenReturn(new ByteArrayInputStream(jiraProjectResponse.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(jiraSecurityResponse.getBytes(StandardCharsets.UTF_8)));

		try {
			tested.findSecurityLevelID("NEXUS", "Red Hat Internal");
		} catch (RuntimeException e) {
			assertEquals("Security level Red Hat Internal was not found for project NEXUS!", e.getMessage());
			throw e;
		}
	}

	private static final String jiraProjectResponse = "{\n" +
			"   \"id\": \"1234\",\n" +
			"   \"key\": \"KEY\",\n" +
			"   \"name\": \"Jira Project\"\n" +
			"}";

	@Test
	public void findProjectID() throws IOException {


		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(jiraProjectResponse.getBytes(StandardCharsets.UTF_8)));

		int id = tested.findProjectID("KEY");
		assertEquals(1234, id);

		id = tested.findProjectID("KEY");
		assertEquals(1234, id); // from cache

		verify(mockedURLConnection).getInputStream(); // called just once
	}

	@Test(expected = RuntimeException.class)
	public void findProjectIDNotExist() throws IOException {
		when(mockedURLConnection.getInputStream()).thenThrow(new FileNotFoundException("Not found!"));

		try {
			tested.findProjectID("KEY");
		} catch (RuntimeException e) {
			assertEquals("Project was not found: KEY", e.getMessage());
			throw e;
		}
	}

	@Test(expected = RuntimeException.class)
	public void findProjectIDIOError() throws IOException {
		when(mockedURLConnection.getInputStream()).thenThrow(new IOException("Network error!"));

		try {
			tested.findProjectID("KEY");
		} catch (RuntimeException e) {
			assertEquals("Error connecting to Jira server: Network error!", e.getMessage());
			throw e;
		}
	}


	@Test
	public void setJiraConnectionInformationTokenAuthentication() {
		tested.setJiraConnectionInformation("https://issues.stage.something.org", null, "token", null,null, null);
		assertEquals("Bearer token", tested.getAuthentication());
	}

	@Test
	public void setJiraConnectionInformationBasicAuthentication() {
		tested.setJiraConnectionInformation("https://issues.stage.something.org", "username", null, "password",null, null);
		assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", tested.getAuthentication());
	}

	@Test
	public void setJiraConnectionInformationNoAuthentication() {
		tested.setJiraConnectionInformation("https://issues.stage.something.org", "username", null, null,null, null);
		assertNull(tested.getAuthentication());

		tested.setJiraConnectionInformation("https://issues.stage.something.org", null, null, "password",null, null);
		assertNull(tested.getAuthentication());
	}

	private static final String ISSUE_TYPE_RESPONSE = "[ {\n" +
			"  \"self\" : \"https://issues.somecompany.org/rest/api/latest/issuetype/2\",\n" +
			"  \"id\" : \"2\",\n" +
			"  \"description\" : \"Feature requests from customers and/or users\",\n" +
			"  \"iconUrl\" : \"https://issues.somecompany.org/secure/viewavatar?size=xsmall&avatarId=13271&avatarType=issuetype\",\n" +
			"  \"name\" : \"Feature Request\",\n" +
			"  \"subtask\" : false,\n" +
			"  \"avatarId\" : 13271\n" +
			"}, {\n" +
			"  \"self\" : \"https://issues.somecompany.org/rest/api/latest/issuetype/13\",\n" +
			"  \"id\" : \"13\",\n" +
			"  \"description\" : \"An enhancement or refactoring of existing functionality\",\n" +
			"  \"iconUrl\" : \"https://issues.somecompany.org/secure/viewavatar?size=xsmall&avatarId=13269&avatarType=issuetype\",\n" +
			"  \"name\" : \"Enhancement\",\n" +
			"  \"subtask\" : false,\n" +
			"  \"avatarId\" : 13269\n" +
			"}, {\n" +
			"  \"self\" : \"https://issues.somecompany.org/rest/api/latest/issuetype/17\",\n" +
			"  \"id\" : \"17\",\n" +
			"  \"description\" : \"Created by Jira Software - do not edit or delete. Issue type for a user story.\",\n" +
			"  \"iconUrl\" : \"https://issues.somecompany.org/secure/viewavatar?size=xsmall&avatarId=13275&avatarType=issuetype\",\n" +
			"  \"name\" : \"Story\",\n" +
			"  \"subtask\" : false,\n" +
			"  \"avatarId\" : 13275\n" +
			"},  {\n" +
			"  \"self\" : \"https://issues.somecompany.org/rest/api/latest/issuetype/16\",\n" +
			"  \"id\" : \"16\",\n" +
			"  \"description\" : \"Created by Jira Software - do not edit or delete. Issue type for a big user story that needs to be broken down.\",\n" +
			"  \"iconUrl\" : \"https://issues.somecompany.org/secure/viewavatar?size=xsmall&avatarId=13267&avatarType=issuetype\",\n" +
			"  \"name\" : \"Epic\",\n" +
			"  \"subtask\" : false,\n" +
			"  \"avatarId\" : 13267\n" +
			"},  {\n" +
			"  \"self\" : \"https://issues.somecompany.org/rest/api/latest/issuetype/1\",\n" +
			"  \"id\" : \"1\",\n" +
			"  \"description\" : \"A problem which impairs or prevents the functions of the product.\",\n" +
			"  \"iconUrl\" : \"https://issues.somecompany.org/secure/viewavatar?size=xsmall&avatarId=13263&avatarType=issuetype\",\n" +
			"  \"name\" : \"Bug\",\n" +
			"  \"subtask\" : false,\n" +
			"  \"avatarId\" : 13263\n" +
			"} ]";

	@Test
	public void findIssueTypeIDNumber() throws IOException {
		Integer result = tested.findIssueTypeID("1001");

		assertEquals((Integer)1001, result);
		verify(mockedURLConnection, never()).getInputStream();
	}

	@Test
	public void findIssueTypeID() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(ISSUE_TYPE_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		Integer result = tested.findIssueTypeID("Story");
		assertEquals((Integer) 17, result);

		assertEquals((Integer) 1, (Integer)tested.findIssueTypeID("Bug"));
		assertEquals((Integer) 16, (Integer) tested.findIssueTypeID("Epic"));

		verify(mockedURLConnection).getInputStream(); // called just once
	}

	@Test
	public void findIssueTypeIDWithRefresh() throws IOException {
		when(mockedURLConnection.getInputStream())
				.thenReturn(new ByteArrayInputStream(ISSUE_TYPE_RESPONSE.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(ISSUE_TYPE_RESPONSE.replace("Bug", "Error in Code").getBytes(StandardCharsets.UTF_8)));

		tested.findPriorityID("Bug"); // initiate the "old" issue type

		String result = tested.findPriorityID("Error in Code");
		assertEquals("1", result);

		verify(mockedURLConnection, times(2)).getInputStream();
	}

	@Test(expected = RuntimeException.class)
	public void findIssueTypeIDWithRefreshFailed() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(ISSUE_TYPE_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		try {
			tested.findIssueTypeID("Something Wrong");
		} catch (RuntimeException e) {
			assertEquals("Issue type Something Wrong was not found!", e.getMessage());

			verify(mockedURLConnection).getInputStream(); // called just once
			throw e;
		}
	}


	private static final String PRIORITY_RESPONSE = "[\n" +
			" \t{\"id\": \"100\", \"name\": \"critical\"},\n" +
			"\t{\"id\": \"200\", \"name\": \"moderate\"},\n" +
			"  {\"id\": \"300\", \"name\": \"low\"}\n" +
			"]";

	@Test
	public void findPriorityIDNumber() throws IOException {
		String result = tested.findPriorityID("1001");

		assertEquals("1001", result);
		verify(mockedURLConnection, never()).getInputStream();
	}

	@Test
	public void findPriorityID() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		String result = tested.findPriorityID("moderate");
		assertEquals("200", result);

		assertEquals("100", tested.findPriorityID("critical"));
		assertEquals("300", tested.findPriorityID("low"));

		verify(mockedURLConnection).getInputStream(); // called just once
	}

	@Test
	public void findPriorityIDWithRefresh() throws IOException {
		when(mockedURLConnection.getInputStream())
				.thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.getBytes(StandardCharsets.UTF_8)))
				.thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.replace("critical", "serious").getBytes(StandardCharsets.UTF_8)));

		tested.findPriorityID("low"); // initiate the "old" priorities

		String result = tested.findPriorityID("serious");
		assertEquals("100", result);

		verify(mockedURLConnection, times(2)).getInputStream();

	}

	@Test(expected = RuntimeException.class)
	public void findPriorityIDWithRefreshFailed() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		try {
			tested.findPriorityID("serious");
		} catch (RuntimeException e) {
			assertEquals("Priority serious was not found!", e.getMessage());

			verify(mockedURLConnection).getInputStream(); // called just once
			throw e;
		}
	}


	private static final String ISSUE_RESPONSE = "{\n" +
			"  \"expand\" : \"renderedFields,names,schema,operations,editmeta,changelog,versionedRepresentations\",\n" +
			"  \"id\" : \"13456\",\n" +
			"  \"self\" : \"https://issues.company.org/rest/api/latest/issue/13456\",\n" +
			"  \"key\" : \"NEXUS-631\",\n" +
			"  \"fields\" : {\n" +
			"    \"customfield_12322440\" : null,\n" +
			"    \"issuetype\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/issuetype/16\",\n" +
			"      \"id\" : \"16\",\n" +
			"      \"description\" : \"Created by Jira Software - do not edit or delete. Issue type for a big user story that needs to be broken down.\",\n" +
			"      \"iconUrl\" : \"https://issues.company.org/secure/viewavatar?size=xsmall&avatarId=13267&avatarType=issuetype\",\n" +
			"      \"name\" : \"Epic\",\n" +
			"      \"subtask\" : false,\n" +
			"      \"avatarId\" : 13267\n" +
			"    },\n" +
			"    \"customfield_12322441\" : null,\n" +
			"    \"customfield_12322244\" : null,\n" +
			"    \"customfield_12318341\" : null,\n" +
			"    \"timespent\" : null,\n" +
			"    \"customfield_12320940\" : null,\n" +
			"    \"project\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/project/12318029\",\n" +
			"      \"id\" : \"12318029\",\n" +
			"      \"key\" : \"NEXUS\",\n" +
			"      \"name\" : \"repository.jboss.org/nexus\",\n" +
			"      \"projectTypeKey\" : \"software\",\n" +
			"      \"avatarUrls\" : {\n" +
			"        \"48x48\" : \"https://issues.company.org/secure/projectavatar?pid=12318029&avatarId=17362\",\n" +
			"        \"24x24\" : \"https://issues.company.org/secure/projectavatar?size=small&pid=12318029&avatarId=17362\",\n" +
			"        \"16x16\" : \"https://issues.company.org/secure/projectavatar?size=xsmall&pid=12318029&avatarId=17362\",\n" +
			"        \"32x32\" : \"https://issues.company.org/secure/projectavatar?size=medium&pid=12318029&avatarId=17362\"\n" +
			"      },\n" +
			"      \"projectCategory\" : {\n" +
			"        \"self\" : \"https://issues.company.org/rest/api/2/projectCategory/10100\",\n" +
			"        \"id\" : \"10100\",\n" +
			"        \"description\" : \"Projects related to miscellaneous technologies.\",\n" +
			"        \"name\" : \"Other\"\n" +
			"      }\n" +
			"    },\n" +
			"    \"fixVersions\" : [ {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/version/12398271\",\n" +
			"      \"id\" : \"12398271\",\n" +
			"      \"name\" : \"Q4CY23\",\n" +
			"      \"archived\" : false,\n" +
			"      \"released\" : false\n" +
			"    } ],\n" +
			"    \"customfield_12320944\" : null,\n" +
			"    \"aggregatetimespent\" : null,\n" +
			"    \"resolution\" : null,\n" +
			"    \"customfield_12310220\" : null,\n" +
			"    \"customfield_12314740\" : \"{summaryBean=com.atlassian.jira.plugin.devstatus.rest.SummaryBean@755573b5[summary={pullrequest=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@57ace7d1[overall=PullRequestOverallBean{stateCount=0, state='OPEN', details=PullRequestOverallDetails{openCount=0, mergedCount=0, declinedCount=0}},byInstanceType={}], build=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@3320e761[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BuildOverallBean@3020cbb[failedBuildCount=0,successfulBuildCount=0,unknownBuildCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], review=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@2bbf5793[overall=com.atlassian.jira.plugin.devstatus.summary.beans.ReviewsOverallBean@711e987c[stateCount=0,state=<null>,dueDate=<null>,overDue=false,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], deployment-environment=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@45b172d1[overall=com.atlassian.jira.plugin.devstatus.summary.beans.DeploymentOverallBean@56c5c908[topEnvironments=[],showProjects=false,successfulCount=0,count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], repository=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@7970f8c[overall=com.atlassian.jira.plugin.devstatus.summary.beans.CommitOverallBean@24faa7a[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}], branch=com.atlassian.jira.plugin.devstatus.rest.SummaryItemBean@4ca67718[overall=com.atlassian.jira.plugin.devstatus.summary.beans.BranchOverallBean@34536a0d[count=0,lastUpdated=<null>,lastUpdatedTimestamp=<null>],byInstanceType={}]},errors=[],configErrors=[]], devSummaryJson={\\\"cachedValue\\\":{\\\"errors\\\":[],\\\"configErrors\\\":[],\\\"summary\\\":{\\\"pullrequest\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":\\\"OPEN\\\",\\\"details\\\":{\\\"openCount\\\":0,\\\"mergedCount\\\":0,\\\"declinedCount\\\":0,\\\"total\\\":0},\\\"open\\\":true},\\\"byInstanceType\\\":{}},\\\"build\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"failedBuildCount\\\":0,\\\"successfulBuildCount\\\":0,\\\"unknownBuildCount\\\":0},\\\"byInstanceType\\\":{}},\\\"review\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"stateCount\\\":0,\\\"state\\\":null,\\\"dueDate\\\":null,\\\"overDue\\\":false,\\\"completed\\\":false},\\\"byInstanceType\\\":{}},\\\"deployment-environment\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null,\\\"topEnvironments\\\":[],\\\"showProjects\\\":false,\\\"successfulCount\\\":0},\\\"byInstanceType\\\":{}},\\\"repository\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}},\\\"branch\\\":{\\\"overall\\\":{\\\"count\\\":0,\\\"lastUpdated\\\":null},\\\"byInstanceType\\\":{}}}},\\\"isStale\\\":true}}\",\n" +
			"    \"resolutiondate\" : null,\n" +
			"    \"workratio\" : -1,\n" +
			"    \"customfield_12317379\" : null,\n" +
			"    \"customfield_12316840\" : null,\n" +
			"    \"customfield_12315950\" : null,\n" +
			"    \"customfield_12316841\" : null,\n" +
			"    \"customfield_12310940\" : [ \"com.atlassian.greenhopper.service.sprint.Sprint@146a4479[id=48248,rapidViewId=3390,state=ACTIVE,name=MWES 2022-11-01,startDate=2022-11-01T12:01:00.000Z,endDate=2022-11-15T12:01:00.000Z,completeDate=<null>,activatedDate=2022-11-01T15:10:23.904Z,sequence=48248,goal=,autoStartStop=false,synced=false]\" ],\n" +
			"    \"customfield_12319040\" : null,\n" +
			"    \"lastViewed\" : null,\n" +
			"    \"watches\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/issue/NEXUS-631/watchers\",\n" +
			"      \"watchCount\" : 0,\n" +
			"      \"isWatching\" : false\n" +
			"    },\n" +
			"    \"customfield_12317140\" : \"0\",\n" +
			"    \"customfield_12317141\" : \"<div> <progress  style=\\\"vertical-align:middle; max-width: 60%;\\\" value=\\\"0\\\" min=\\\"0\\\" max=\\\"100\\\">0%</progress> 0% </div>\",\n" +
			"    \"created\" : \"2022-09-19T08:21:07.597+0000\",\n" +
			"    \"customfield_12321240\" : null,\n" +
			"    \"customfield_12313140\" : null,\n" +
			"    \"priority\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/priority/3\",\n" +
			"      \"iconUrl\" : \"https://issues.company.org/images/icons/priorities/major.svg\",\n" +
			"      \"name\" : \"Major\",\n" +
			"      \"id\" : \"3\"\n" +
			"    },\n" +
			"    \"labels\" : [ ],\n" +
			"    \"customfield_12313940\" : null,\n" +
			"    \"customfield_12311640\" : null,\n" +
			"    \"customfield_12320947\" : null,\n" +
			"    \"customfield_12320946\" : null,\n" +
			"    \"customfield_12313541\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/customFieldOption/12685\",\n" +
			"      \"value\" : \"Infrastructure Working Group\",\n" +
			"      \"id\" : \"12685\",\n" +
			"      \"disabled\" : false\n" +
			"    },\n" +
			"    \"aggregatetimeoriginalestimate\" : null,\n" +
			"    \"timeestimate\" : null,\n" +
			"    \"versions\" : [ ],\n" +
			"    \"customfield_12310031\" : null,\n" +
			"    \"issuelinks\" : [ ],\n" +
			"    \"components\" : [ {\n" +
			"      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/component/12372671\",\n" +
			"      \"id\" : \"12372671\",\n" +
			"      \"name\" : \"Maven Central Plugin\",\n" +
			"      \"description\" : \"Maven Central Deployment Plugin\"\n" +
			"    }, {\n" +
			"      \"self\" : \"https://issues.stage.redhat.com/rest/api/2/component/12372672\",\n" +
			"      \"id\" : \"12372672\",\n" +
			"      \"name\" : \"SSO Plugin\",\n" +
			"      \"description\" : \"Nexus 3 SSO Plugin\"\n" +
			"    } ],\n" +
			"    \"assignee\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/user?username=dhladky%40redhat.com\",\n" +
			"      \"name\" : \"dhladky@redhat.com\",\n" +
			"      \"key\" : \"dhladky\",\n" +
			"      \"emailAddress\" : \"e-mail@company.org\",\n" +
			"      \"avatarUrls\" : {\n" +
			"        \"48x48\" : \"https://issues.company.org/secure/useravatar?ownerId=dhladky&avatarId=25507\",\n" +
			"        \"24x24\" : \"https://issues.company.org/secure/useravatar?size=small&ownerId=dhladky&avatarId=25507\",\n" +
			"        \"16x16\" : \"https://issues.company.org/secure/useravatar?size=xsmall&ownerId=dhladky&avatarId=25507\",\n" +
			"        \"32x32\" : \"https://issues.company.org/secure/useravatar?size=medium&ownerId=dhladky&avatarId=25507\"\n" +
			"      },\n" +
			"      \"displayName\" : \"David Hladky\",\n" +
			"      \"active\" : true,\n" +
			"      \"timeZone\" : \"Europe/Prague\"\n" +
			"    },\n" +
			"    \"updated\" : \"2022-11-09T09:06:47.181+0000\",\n" +
			"    \"customfield_12313942\" : null,\n" +
			"    \"customfield_12313941\" : null,\n" +
			"    \"status\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/status/10020\",\n" +
			"      \"description\" : \"The team is planning to do this work and it has a priority set\",\n" +
			"      \"iconUrl\" : \"https://issues.company.org/\",\n" +
			"      \"name\" : \"To Do\",\n" +
			"      \"id\" : \"10020\",\n" +
			"      \"statusCategory\" : {\n" +
			"        \"self\" : \"https://issues.company.org/rest/api/2/statuscategory/2\",\n" +
			"        \"id\" : 2,\n" +
			"        \"key\" : \"new\",\n" +
			"        \"colorName\" : \"default\",\n" +
			"        \"name\" : \"To Do\"\n" +
			"      }\n" +
			"    },\n" +
			"    \"timeoriginalestimate\" : null,\n" +
			"    \"description\" : \"Nexus 2 has reach end of support and so we need to migrate to the newer product line (Nexus 3). Things to consider\\r\\n\\r\\n- replacing staging suite\\r\\n- SSO\\r\\n- high availability\\r\\n- Maven Central synchronization\\r\\n- Maven Central statistics\\r\\n- splitting people to projects\\r\\n\",\n" +
			"    \"customfield_12314040\" : null,\n" +
			"    \"timetracking\" : { },\n" +
			"    \"security\" : {\n" +
			"        \"self\" : \"https://issues.stage.redhat.com/rest/api/2/securitylevel/10291\",\n" +
			"        \"id\" : \"12569\",\n" +
			"        \"description\" : \"Only Red Hat employees and contractors\",\n" +
			"        \"name\" : \"Red Hat Internal\"\n" +
			"      },\n"+
			"    \"customfield_12320842\" : null,\n" +
			"    \"archiveddate\" : null,\n" +
			"    \"customfield_12310440\" : null,\n" +
			"    \"customfield_12310243\" : null,\n" +
			"    \"attachment\" : [ ],\n" +
			"    \"aggregatetimeestimate\" : null,\n" +
			"    \"customfield_12316542\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/customFieldOption/14655\",\n" +
			"      \"value\" : \"False\",\n" +
			"      \"id\" : \"14655\",\n" +
			"      \"disabled\" : false\n" +
			"    },\n" +
			"    \"customfield_12316543\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/customFieldOption/14657\",\n" +
			"      \"value\" : \"False\",\n" +
			"      \"id\" : \"14657\",\n" +
			"      \"disabled\" : false\n" +
			"    },\n" +
			"    \"customfield_12317313\" : null,\n" +
			"    \"customfield_12310840\" : \"9223372036854775807\",\n" +
			"    \"customfield_12316544\" : \"None\",\n" +
			"    \"summary\" : \"Migration of repository.jboss.org from Nexus 2 to Nexus 3\",\n" +
			"    \"creator\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/user?username=dhladky%40redhat.com\",\n" +
			"      \"name\" : \"dhladky@redhat.com\",\n" +
			"      \"key\" : \"dhladky\",\n" +
			"      \"emailAddress\" : \"e-mail@company.org\",\n" +
			"      \"avatarUrls\" : {\n" +
			"        \"48x48\" : \"https://issues.company.org/secure/useravatar?ownerId=dhladky&avatarId=25507\",\n" +
			"        \"24x24\" : \"https://issues.company.org/secure/useravatar?size=small&ownerId=dhladky&avatarId=25507\",\n" +
			"        \"16x16\" : \"https://issues.company.org/secure/useravatar?size=xsmall&ownerId=dhladky&avatarId=25507\",\n" +
			"        \"32x32\" : \"https://issues.company.org/secure/useravatar?size=medium&ownerId=dhladky&avatarId=25507\"\n" +
			"      },\n" +
			"      \"displayName\" : \"David Hladky\",\n" +
			"      \"active\" : true,\n" +
			"      \"timeZone\" : \"Europe/Prague\"\n" +
			"    },\n" +
			"    \"subtasks\" : [ ],\n" +
			"    \"customfield_12321140\" : null,\n" +
			"    \"reporter\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/user?username=dhladky%40redhat.com\",\n" +
			"      \"name\" : \"dhladky@redhat.com\",\n" +
			"      \"key\" : \"dhladky\",\n" +
			"      \"emailAddress\" : \"e-mail@company.org\",\n" +
			"      \"avatarUrls\" : {\n" +
			"        \"48x48\" : \"https://issues.company.org/secure/useravatar?ownerId=dhladky&avatarId=25507\",\n" +
			"        \"24x24\" : \"https://issues.company.org/secure/useravatar?size=small&ownerId=dhladky&avatarId=25507\",\n" +
			"        \"16x16\" : \"https://issues.company.org/secure/useravatar?size=xsmall&ownerId=dhladky&avatarId=25507\",\n" +
			"        \"32x32\" : \"https://issues.company.org/secure/useravatar?size=medium&ownerId=dhladky&avatarId=25507\"\n" +
			"      },\n" +
			"      \"displayName\" : \"David Hladky\",\n" +
			"      \"active\" : true,\n" +
			"      \"timeZone\" : \"Europe/Prague\"\n" +
			"    },\n" +
			"    \"customfield_12320850\" : null,\n" +
			"    \"customfield_12310092\" : null,\n" +
			"    \"aggregateprogress\" : {\n" +
			"      \"progress\" : 0,\n" +
			"      \"total\" : 0\n" +
			"    },\n" +
			"    \"customfield_12310010\" : null,\n" +
			"    \"customfield_12311143\" : \"ghx-label-9\",\n" +
			"    \"customfield_12310211\" : null,\n" +
			"    \"customfield_12311141\" : \"Migration of repository.jboss.org from Nexus 2 to Nexus 3\",\n" +
			"    \"customfield_12313441\" : \"\",\n" +
			"    \"customfield_12315740\" : null,\n" +
			"    \"customfield_12315542\" : null,\n" +
			"    \"customfield_12313440\" : \"0.0\",\n" +
			"    \"customfield_12311142\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/customFieldOption/10450\",\n" +
			"      \"value\" : \"To Do\",\n" +
			"      \"id\" : \"10450\",\n" +
			"      \"disabled\" : false\n" +
			"    },\n" +
			"    \"customfield_12313240\" : null,\n" +
			"    \"duedate\" : null,\n" +
			"    \"customfield_12311140\" : null,\n" +
			"    \"customfield_12319742\" : null,\n" +
			"    \"progress\" : {\n" +
			"      \"progress\" : 0,\n" +
			"      \"total\" : 0\n" +
			"    },\n" +
			"    \"comment\" : {\n" +
			"      \"comments\" : [ ],\n" +
			"      \"maxResults\" : 0,\n" +
			"      \"total\" : 0,\n" +
			"      \"startAt\" : 0\n" +
			"    },\n" +
			"    \"votes\" : {\n" +
			"      \"self\" : \"https://issues.company.org/rest/api/2/issue/NEXUS-631/votes\",\n" +
			"      \"votes\" : 0,\n" +
			"      \"hasVoted\" : false\n" +
			"    },\n" +
			"    \"worklog\" : {\n" +
			"      \"startAt\" : 0,\n" +
			"      \"maxResults\" : 20,\n" +
			"      \"total\" : 0,\n" +
			"      \"worklogs\" : [ ]\n" +
			"    },\n" +
			"    \"customfield_12319743\" : null,\n" +
			"    \"customfield_12310213\" : null,\n" +
			"    \"archivedby\" : null,\n" +
			"    \"customfield_12311940\" : \"1|z9ltvs:\"\n" +
			"  }\n" +
			"}";


	@Test(expected = RuntimeException.class)
	public void tryJiraIssueMissingIssue() {
		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();

		try {
			tested.tryJiraIssue(configuration);
		} catch (RuntimeException e) {
			assertEquals("Issue was not configured!", e.getMessage());
			throw e;
		}
	}

	@Test(expected = RuntimeException.class)
	public void tryJiraIssueMissingJiraConfiguration() {
		tested = new JiraTestReportServerInformation(new JiraTestReportCapabilityDescriptor());
		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();
		configuration.setIssue("ISSUE-333");

		try {
			tested.tryJiraIssue(configuration);
		} catch (RuntimeException e) {
			assertEquals("Missing link to Jira: Activate and configure MCD - Jira Reports Default Configuration!", e.getMessage());
			throw e;
		}
	}

	@Test
	public void tryJiraIssueWipeOutNulls() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(ISSUE_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();
		configuration.setIssue("ISSUE-333");

		configuration.setWipeNullFields(false);
		tested.tryJiraIssue(configuration);
		assertTrue(configuration.getLatestResult().contains(": null"));


		configuration.setWipeNullFields(true);
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(ISSUE_RESPONSE.getBytes(StandardCharsets.UTF_8)));
		tested.tryJiraIssue(configuration);
		assertFalse(configuration.getLatestResult().contains(": null"));
	}

	@Test
	public void tryJiraIssueLabelsNoLabels() throws IOException {
		String json = "{\"fields\": {" +
				"\"fixVersions\" : []" +
				"}}";
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();
		configuration.setIssue("ISSUE-333");
		configuration.setUseVelocityVariables(true);

		configuration.setWipeNullFields(false);
		tested.tryJiraIssue(configuration);

		assertFalse( configuration.getLatestResult().contains("${labels}") );
		assertFalse( configuration.getLatestResult().contains("\"labels\"") );
	}

	@Test
	public void tryJiraIssueLabelsPresentButEmpty() throws IOException {
		String json = "{\"fields\": {" +
				"\"fixVersions\" : [  ]," +
				"\"labels\" : [       ]" +
				"}}";
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();
		configuration.setIssue("ISSUE-333");
		configuration.setUseVelocityVariables(true);

		configuration.setWipeNullFields(false);
		tested.tryJiraIssue(configuration);

		assertTrue( configuration.getLatestResult().contains("${labels}") );
		assertTrue( configuration.getLatestResult().contains("\"labels\"") );
	}

	@Test
	public void tryJiraIssueLabelsPresentMultiple() throws IOException {
		String json = "{\"fields\": {" +
				"\"fixVersions\" : [  ]," +
				"\"labels\" : [ \"label1\", \"label2\"      ]" +
				"}}";
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();
		configuration.setIssue("ISSUE-333");
		configuration.setUseVelocityVariables(true);

		configuration.setWipeNullFields(false);
		tested.tryJiraIssue(configuration);

		assertTrue( configuration.getLatestResult().contains("\"label1\", \"label2\" ${labels}") );
		assertTrue( configuration.getLatestResult().contains("\"labels\"") );
	}


	@Test
	public void tryJiraIssueUseVelocityVariables() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(ISSUE_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		JiraReadKnownJiraIssueTaskConfiguration configuration = new JiraReadKnownJiraIssueTaskConfiguration();
		configuration.setIssue("ISSUE-333");

		configuration.setUseVelocityVariables(false);
		tested.tryJiraIssue(configuration);

		// FIXME: 27.04.2023 proper variable names here!
		final String[] variables = {"${project_id}", "${issue_type_id}", "${priority_id}", "${reporter}", "${assignee}", "${summary}", "${description}", "${security_level_id}", "${labels}", "${components}"};

		final String result = configuration.getLatestResult();
		assertTrue(Arrays.stream(variables).noneMatch(result::contains));

		configuration.setUseVelocityVariables(true);
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(ISSUE_RESPONSE.getBytes(StandardCharsets.UTF_8)));
		tested.tryJiraIssue(configuration);

		final String result2 = configuration.getLatestResult();
		assertTrue(Arrays.stream(variables).allMatch(result2::contains));
	}
}