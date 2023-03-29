package org.jboss.nexus.validation.reporting;

public class JiraTestReportCapabilityConfiguration extends TestReportCapabilityConfigurationParent{

	public static final String JIRA_BASE_URL = "jira.base.url";

	public static final String PROXY_HOST = "proxy.host";

	public static final String PROXY_PORT = "proxy.port";

	public static final String USER_NAME = "user.name";

	public static final String USER_PASSWORD = "user.password";

	public static final String TOKEN = "token";

	public static final String PROJECT = "project";

	public static final String SEVERITY = "severity";
	
	public static final String SUMMARY = "summary";
	
	public static final String DESCRIPTION = "description";
	
	public static final String LABELS = "labels";
	
	public static final String PRIORITY = "priority";
	
	public static final String SECURITY = "security";
	
	public static final String ASSIGNEE = "assignee";
	
	public static final String REPORTER = "reporter";
	
	public static final String COMPONENTS = "components";
	
	public static final String ISSUE_TYPE= "issue.type";
	

	public static class IssueDescription {

		private String jiraBaseUrl;

		private String proxyHost;

		private Integer proxyPort;

		private String userName;

		private String password;

		private String token;

		private String project;

		private String summary;

		private String description;

		private String labels;

		private String severity;

		private String priority;

		private String security;

		private String assignee;

		private String reporter;

		/** Gets base URL of Jira.
		 *
		 * @return Jira URL in form http(s)://issues.something.org
		 */
		public String getJiraBaseUrl() {
			return jiraBaseUrl;
		}

		/** Sets base Jira URL
		 *
		 * @param jiraBaseUrl Jira URL in form http(s)://issues.something.org
		 */
		public void setJiraBaseUrl(String jiraBaseUrl) {
			this.jiraBaseUrl = jiraBaseUrl;
		}

		/** Gets the proxy host or IP address.
		 *
		 * @return null, host or IP address of the proxy
		 */
		public String getProxyHost() {
			return proxyHost;
		}

		/** Sets host of the proxy server if needed.
		 *
		 * @param proxyHost null, host or IP address of the proxy
		 */
		public void setProxyHost(String proxyHost) {
			this.proxyHost = proxyHost;
		}

		/** Gets the port number of the proxy server.
		 *
		 * @return null or the port number
		 */
		public Integer getProxyPort() {
			return proxyPort;
		}


		/** Sets port number of the proxy server.
		 *
		 * @param proxyPort null or the port number
		 */
		public void setProxyPort(Integer proxyPort) {
			this.proxyPort = proxyPort;
		}

		/** Username of the account, that will be used in the communication with Jira.
		 *
		 * @return username
		 */
		public String getUserName() {
			return userName;
		}


		public void setUserName(String userName) {
			this.userName = userName;
		}

		/** Access token of the user to be used in the communication in Jira
		 *
		 * @return access token
		 */
		public String getToken() {
			return token;
		}


		public void setToken(String token) {
			this.token = token;
		}


		/** Gets the password of the user for basic authentication.
		 *
		 * @return password or null
		 */
		public String getPassword() {
			return password;
		}

		/** Sets the password of the user for basic authentication.
		 *
		 * @param password password or null
		 */
		public void setPassword(String password) {
			this.password = password;
		}

		/** Name or ID of the project to be used for the new tickets.
		 *
		 * @return name or ID
		 */
		public String getProject() {
			return project;
		}

		public void setProject(String project) {
			this.project = project;
		}

		/** Velocity template of the ticket summary
		 *
		 * @return template of the ticket summary
		 */
		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		/** Velocity template of the ticket description.
		 *
		 * @return velocity ticket of the description
		 */
		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		/** Comma separated list of labels. Velocity variables are allowed.
		 *
		 * @return null or list of labels
		 */
		public String getLabels() {
			return labels;
		}

		public void setLabels(String labels) {
			this.labels = labels;
		}

		/** ID or name of the severity field.
		 *
		 * @return ID or name of the security field
		 */
		public String getSeverity() {
			return severity;
		}

		/** Set the severity of the ticket.
		 *
		 * @param severity severity ID or name
		 */
		public void setSeverity(String severity) {
			this.severity = severity;
		}

		/** ID or name of the priority.
		 *
		 * @return priority
		 */
		public String getPriority() {
			return priority;
		}

		/** Set the priority of the ticket
		 *
		 * @param priority priority
		 */
		public void setPriority(String priority) {
			this.priority = priority;
		}

		/** ID or a name of the security policy (visibility of the ticket).
		 *
		 * @return security or null
		 */
		public String getSecurity() {
			return security;
		}

		/** Set the visibility of the ticket.
		 *
		 * @param security security
		 */
		public void setSecurity(String security) {
			this.security = security;
		}

		/** Assignee of the ticket.
		 *
		 * @return assignee or null
		 */
		public String getAssignee() {
			return assignee;
		}

		/** Set the assignee of the ticket.
		 *
		 * @param assignee assignee
		 */
		public void setAssignee(String assignee) {
			this.assignee = assignee;
		}

		/** The reporter of the ticket.
		 *
		 * @return reporter or null
		 */
		public String getReporter() {
			return reporter;
		}

		/** Set the reporter of the ticket.
		 *
		 * @param reporter reporter
		 */
		public void setReporter(String reporter) {
			this.reporter = reporter;
		}
	}
	
	
	
}
