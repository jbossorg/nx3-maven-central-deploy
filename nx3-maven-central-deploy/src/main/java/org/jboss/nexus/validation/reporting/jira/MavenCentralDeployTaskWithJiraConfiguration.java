package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;

/** Configuration for deployment task with Jira stuff defined. */
@SuppressWarnings("unused")
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


    /** Name or ID of the project to be used for the new tickets.
     *
     * @return name or ID
     */
    public String getProject() {
        return getString(JiraTestReportCapabilityConfiguration.PROJECT);
    }

    public void setProject(String project) {
        setString(JiraTestReportCapabilityConfiguration.PROJECT,  project);
    }

    /** Velocity template of the ticket summary
     *
     * @return template of the ticket summary
     */
    public String getSummary() {
        return getString(JiraTestReportCapabilityConfiguration.SUMMARY);
    }

    public void setSummary(String summary) {
        setString(JiraTestReportCapabilityConfiguration.SUMMARY,  summary);
    }

    /** Velocity template of the ticket description.
     *
     * @return velocity ticket of the description
     */
    public String getDescription() {
        return getString(JiraTestReportCapabilityConfiguration.DESCRIPTION);
    }

    public void setDescription(String description) {
        setString(JiraTestReportCapabilityConfiguration.DESCRIPTION,  description);
    }

    /** Gets the type of the issue
     *
     * @return issue type
     */
    public String getIssueType() {
        return getString(JiraTestReportCapabilityConfiguration.ISSUE_TYPE);
    }

    /** Sets the issue type.
     *
     * @param issueType issue type number or name
     */
    public void setIssueType(String issueType) {
        setString(JiraTestReportCapabilityConfiguration.ISSUE_TYPE,  issueType);
    }

    /** Comma separated list of labels. Velocity variables are allowed.
     *
     * @return null or list of labels
     */
    public String getLabels() {
        return getString(JiraTestReportCapabilityConfiguration.LABELS);
    }

    public void setLabels(String labels) {
        setString(JiraTestReportCapabilityConfiguration.LABELS,  labels);
    }

    /** ID or name of the priority.
     *
     * @return priority
     */
    public String getPriority() {
        return getString(JiraTestReportCapabilityConfiguration.PRIORITY);
    }

    /** Set the priority of the ticket
     *
     * @param priority priority
     */
    public void setPriority(String priority) {
         setString(JiraTestReportCapabilityConfiguration.PRIORITY,  priority);
    }

    /** ID or a name of the security policy (visibility of the ticket).
     *
     * @return security or null
     */
    public String getSecurity() {
        return getString(JiraTestReportCapabilityConfiguration.SECURITY);
    }

    /** Set the visibility of the ticket.
     *
     * @param security security
     */
    public void setSecurity(String security) {
        setString(JiraTestReportCapabilityConfiguration.SECURITY,  security);
    }

    /** Assignee of the ticket.
     *
     * @return assignee or null
     */
    public String getAssignee() {
        return getString(JiraTestReportCapabilityConfiguration.ASSIGNEE);
    }

    /** Set the assignee of the ticket.
     *
     * @param assignee assignee
     */
    public void setAssignee(String assignee) {
        setString(JiraTestReportCapabilityConfiguration.ASSIGNEE,  assignee);
    }

    /** The reporter of the ticket.
     *
     * @return reporter or null
     */
    public String getReporter() {
        return getString(JiraTestReportCapabilityConfiguration.REPORTER);
    }

    /** Set the reporter of the ticket.
     *
     * @param reporter reporter
     */
    public void setReporter(String reporter) {
        setString(JiraTestReportCapabilityConfiguration.REPORTER, reporter);
    }

    /** Components
     *
     * @return comma separated list of components
     */
    public String getComponents() {
        return getString(JiraTestReportCapabilityConfiguration.COMPONENTS);
    }

    /** Sets components.
     *
     * @param components comma separated list of components
     */
    public void setComponents(String components) {
        setString(JiraTestReportCapabilityConfiguration.COMPONENTS,  components);
    }

    /** Gets the template for create JSON.
     *
     * @return  Velocity template for the REST call to Jira
     */
    public String getTemplate() {
        return getString(JiraTestReportCapabilityConfiguration.TEMPLATE);
    }

    /** Sets the template for create JSON.
     *
     * @param template   Velocity template for the REST call to Jira
     */
    public void setTemplate(String template) {
        setString(JiraTestReportCapabilityConfiguration.TEMPLATE,  template);
    }

}
