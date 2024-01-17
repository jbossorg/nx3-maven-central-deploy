package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;

/** Configuration for deployment task with Jira stuff defined. */
public class MavenCentralDeployTaskWithJiraConfiguration extends MavenCentralDeployTaskConfiguration {

    public static final String CREATE_TEST_TICKET = "create.test.ticket";

    /** If set to true, instead of deploying a Jira issue with fictive errors should be created.
     *
     * @param value true to test creating tickets
     */
    public void setCreateTestTicket(boolean value) {
        setBoolean(CREATE_TEST_TICKET, value);
    }

    /** If set to true, instead of deploying a Jira issue with fictive errors should be created.
     *
     * @return value
     */
    public boolean getCreateTestTicket() {
        return getBoolean(CREATE_TEST_TICKET, false);
    }

    /** Gets Jira configuration specific for this task
     *
     * @return Jira configuration
     */
    public JiraTestReportCapabilityConfiguration.JiraConfiguration getJiraConfiguration() {
        return new JiraTestReportCapabilityConfiguration.JiraConfiguration(asMap());
    }
}
