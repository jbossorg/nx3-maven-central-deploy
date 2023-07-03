package org.jboss.nexus.validation.reporting.jira;

import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.validation.reporting.TestReportCapabilityConfigurationParent;
import java.util.HashMap;
import java.util.Map;

public class JiraTestReportCapabilityConfiguration extends TestReportCapabilityConfigurationParent {

	public static final String JIRA_BASE_URL = "jira.base.url";

	public static final String PROXY_HOST = "proxy.host";

	public static final String PROXY_PORT = "proxy.port";

	public static final String USER_NAME = "user.name";

	public static final String USER_PASSWORD = "user.password";

	public static final String TOKEN = "token";

	public static final String PROJECT = "project";

	public static final String SUMMARY = "summary";
	
	public static final String DESCRIPTION = "description";

	public static final String ISSUE_TYPE = "issueType";

	public static final String LABELS = "labels";
	
	public static final String PRIORITY = "priority";
	
	public static final String SECURITY = "security";
	
	public static final String ASSIGNEE = "assignee";
	
	public static final String REPORTER = "reporter";
	
	public static final String COMPONENTS = "components";

	public static final String TEMPLATE = "template";


	@SuppressWarnings("unused")
	public JiraTestReportCapabilityConfiguration() {
		defaultJiraConfiguration = new JiraConfiguration();
	}
	
	public JiraTestReportCapabilityConfiguration(Map<String, String> properties) {
		defaultJiraConfiguration = new JiraConfiguration(properties);
	}

	public static class JiraConfiguration {
		
		public JiraConfiguration() {}
		
		public JiraConfiguration(Map<String, String> properties) {
			setJiraBaseUrl(properties.get(JiraTestReportCapabilityConfiguration.JIRA_BASE_URL));
			setProxyHost(properties.get(JiraTestReportCapabilityConfiguration.PROXY_HOST));

			String proxyPort = properties.get(JiraTestReportCapabilityConfiguration.PROXY_PORT);
			if(proxyPort != null)
				setProxyPort(Integer.valueOf(proxyPort));

			setUserName(properties.get(JiraTestReportCapabilityConfiguration.USER_NAME));
			setPassword(properties.get(JiraTestReportCapabilityConfiguration.USER_PASSWORD));
			setToken(properties.get(JiraTestReportCapabilityConfiguration.TOKEN));
			setProject(properties.get(JiraTestReportCapabilityConfiguration.PROJECT));
			setSummary(properties.get(JiraTestReportCapabilityConfiguration.SUMMARY));
			setDescription(properties.get(JiraTestReportCapabilityConfiguration.DESCRIPTION));
			setIssueType(properties.get(JiraTestReportCapabilityConfiguration.ISSUE_TYPE));
			setComponents(properties.get(JiraTestReportCapabilityConfiguration.COMPONENTS));
			setLabels(properties.get(JiraTestReportCapabilityConfiguration.LABELS));
			setPriority(properties.get(JiraTestReportCapabilityConfiguration.PRIORITY));
			setSecurity(properties.get(JiraTestReportCapabilityConfiguration.SECURITY));
			setAssignee(properties.get(JiraTestReportCapabilityConfiguration.ASSIGNEE));
			setReporter(properties.get(JiraTestReportCapabilityConfiguration.REPORTER));
			setTemplate(properties.get(JiraTestReportCapabilityConfiguration.TEMPLATE));
		}


		public Map<String, String> asMap() {
			HashMap<String, String> result = new HashMap<>();

			if(StringUtils.isNotBlank(getJiraBaseUrl()))
				result.put(JiraTestReportCapabilityConfiguration.JIRA_BASE_URL, getJiraBaseUrl());

			if(StringUtils.isNotBlank(getProxyHost()))
				result.put(JiraTestReportCapabilityConfiguration.PROXY_HOST, getProxyHost());

			if(getProxyPort() != null)
				 result.put(JiraTestReportCapabilityConfiguration.PROXY_PORT, getProxyPort().toString());

			if(StringUtils.isNotBlank(getUserName()))
				result.put(JiraTestReportCapabilityConfiguration.USER_NAME, getUserName());

			if(StringUtils.isNotBlank(getPassword()))
				result.put(JiraTestReportCapabilityConfiguration.USER_PASSWORD, getPassword());

			if(StringUtils.isNotBlank(getToken()))
				result.put(JiraTestReportCapabilityConfiguration.TOKEN, getToken());

			if(StringUtils.isNotBlank(getProject()))
				result.put(JiraTestReportCapabilityConfiguration.PROJECT, getProject());

			if(StringUtils.isNotBlank(getSummary()))
				result.put(JiraTestReportCapabilityConfiguration.SUMMARY, getSummary());

			if(StringUtils.isNotBlank(getDescription()))
				result.put(JiraTestReportCapabilityConfiguration.DESCRIPTION, getDescription());

			if(StringUtils.isNotBlank(getIssueType()))
				result.put(JiraTestReportCapabilityConfiguration.ISSUE_TYPE, getIssueType());

			if(StringUtils.isNotBlank(getComponents()))
				result.put(JiraTestReportCapabilityConfiguration.COMPONENTS, getComponents());

			if(StringUtils.isNotBlank(getLabels()))
				result.put(JiraTestReportCapabilityConfiguration.LABELS, getLabels());

			if(StringUtils.isNotBlank(getPriority()))
				result.put(JiraTestReportCapabilityConfiguration.PRIORITY, getPriority());

			if(StringUtils.isNotBlank(getSecurity()))
				result.put(JiraTestReportCapabilityConfiguration.SECURITY, getSecurity());

			if(StringUtils.isNotBlank(getAssignee()))
				result.put(JiraTestReportCapabilityConfiguration.ASSIGNEE, getAssignee());

			if(StringUtils.isNotBlank(getReporter()))
				result.put(JiraTestReportCapabilityConfiguration.REPORTER, getReporter());

			if(StringUtils.isNotBlank(getTemplate()))
				result.put(JiraTestReportCapabilityConfiguration.TEMPLATE, getTemplate());

			return result;

		}

		private String jiraBaseUrl;

		private String proxyHost;

		private Integer proxyPort;

		private String userName;

		private String password;

		private String token;

		private String project;

		private String summary;

		private String description;

		private String issueType;

		private String labels;

		private String priority;

		private String security;

		private String assignee;

		private String reporter;
		
		private String components;

		private String template;

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

		/** Gets the type of the issue
		 *
		 * @return issue type
		 */
		public String getIssueType() {
			return issueType;
		}

		/** Sets the issue type.
		 *
		 * @param issueType issue type number or name
		 */
		public void setIssueType(String issueType) {
			this.issueType = issueType;
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

		/** Components
		 * 
		 * @return comma separated list of components
		 */
		public String getComponents() {
			return components;
		}

		/** Sets components.
		 * 
		 * @param components comma separated list of components
		 */
		public void setComponents(String components) {
			this.components = components;
		}

		/** Gets the template for create JSON.
		 *
		 * @return  Velocity template for the REST call to Jira
		 */
		public String getTemplate() {
			return template;
		}

		/** Sets the template for create JSON.
		 *
		 * @param template   Velocity template for the REST call to Jira
		 */
		public void setTemplate(String template) {
			this.template = template;
		}
	}
	
	private final JiraConfiguration defaultJiraConfiguration;

	/** Returns default Jira configuration
	 *
	 * @return configuration
	 */
	public JiraConfiguration getDefaultJiraConfiguration() {
		return defaultJiraConfiguration;
	}
}
