package org.jboss.nexus.validation.reporting.jira;

import org.apache.commons.lang3.StringUtils;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.TaskSupport;

import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

@Named(JiraReadKnownJiraIssueTask.TYPE)
@TaskLogging(NEXUS_LOG_ONLY)
public class JiraReadKnownJiraIssueTask extends TaskSupport {

	public static final String TYPE = "jira.read.known";

	private final JiraTestReportServerInformation jiraTestReportServerInformation;


	@Inject
	public JiraReadKnownJiraIssueTask(JiraTestReportServerInformation jiraTestReportServerInformation) {
		this.jiraTestReportServerInformation = checkNotNull(jiraTestReportServerInformation);
	}

	@Override
	protected Object execute() {

		JiraReadKnownJiraIssueTaskConfiguration taskConfiguration = new JiraReadKnownJiraIssueTaskConfiguration(getConfiguration());

		if(StringUtils.isBlank(taskConfiguration.getIssue())) {
			String msg = "Issue field was left empty.";
			taskConfiguration.setLatestResult(msg);
			throw new RuntimeException(msg);
		}

		try {
			jiraTestReportServerInformation.tryJiraIssue(taskConfiguration);
			getConfiguration().setString(JiraReadKnownJiraIssueTaskConfiguration.LATEST_RESULT, taskConfiguration.getLatestResult());
			return "OK";
		} catch (Exception e) {
			getConfiguration().setString(JiraReadKnownJiraIssueTaskConfiguration.LATEST_RESULT, "Error retrieving Jira issue: "+e.getMessage());
			throw e;
		}
	}

	@Override
	public String getMessage() {
		return "Investigating Jira issue "+getConfiguration().getString(JiraReadKnownJiraIssueTaskConfiguration.ISSUE)+"....";
	}

}
