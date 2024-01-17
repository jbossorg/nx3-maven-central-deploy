package org.jboss.nexus.validation.reporting.jira;

import org.sonatype.nexus.scheduling.TaskConfiguration;

/**
 * Configuration for parsing the ticket from Jira
 */
public class JiraReadKnownJiraIssueTaskConfiguration extends TaskConfiguration {

	public static final String LATEST_RESULT = "latestResult";
	public static final String DESCRIPTION = "description";

	public static final String ISSUE = "issue";

	public static final String USE_VELOCITY_VARIABLES = "useVelocityVariables";

	public static final String WIPE_NULL_FIELDS = "wipeNullFields";


	public JiraReadKnownJiraIssueTaskConfiguration() {}

	public JiraReadKnownJiraIssueTaskConfiguration(TaskConfiguration taskConfiguration) {
		super(taskConfiguration);
	}


	/** Sets issue to be investigated by this task.
	 *
	 * @param issue  issue type
	 */
	public void setIssue(String issue) {
		setString(ISSUE, issue);
	}

	/** Gets issue to be investigated by this task.
	 *
	 * @return issue type
	 */
	public String getIssue() {
		return getString(ISSUE);
	}

	/** Result of the latest call of this task.
	 *
	 * @return ideally nicely formatted JSON of the issue
	 */
	public String getLatestResult() {
		return getString(LATEST_RESULT);
	}

	/** Sets latest result.
	 *
	 * @param latestResult sets result of the task processing
	 */
	public void setLatestResult(String latestResult) {
		setString(LATEST_RESULT, latestResult);
	}

	/** Sets the information, whether JSON should be Velocity variables or the actual field values.
	 *
	 * @param useVelocityVariables true for variables
	 */
	public void setUseVelocityVariables(boolean useVelocityVariables) {
		setBoolean(USE_VELOCITY_VARIABLES, useVelocityVariables);
	}

	/** Gets the information, whether JSON should be Velocity variables or the actual field values.
	 *
	 * @return true for variables
	 */
	public boolean getUseVelocityVariables() {
		return getBoolean(USE_VELOCITY_VARIABLES, false);
	}


	/** Setter. If set, the empty fields should be removed from JSON.
	 *
	 * @param wipeNullFields  if true, remove null fields from json
	 */
	public void setWipeNullFields(boolean wipeNullFields) {
		setBoolean(WIPE_NULL_FIELDS, wipeNullFields);
	}

	/** Getter. If set, the empty fields should be removed from JSON.
	 *
	 * @return if true, remove null fields from json
	 */
	public boolean getWipeNullFields() {
		return getBoolean(WIPE_NULL_FIELDS, true);
	}


	/** Returns description gained during issue analysis.
	 *
	 * @return description
	 */
	public String getDescription() {
		return getString(DESCRIPTION);
	}


	/** Set description gained during issue analysis
	 *
	 * @param description the description to save
	 */
	public void setDescription(String description) {
		setString(DESCRIPTION, description);
	}

}
