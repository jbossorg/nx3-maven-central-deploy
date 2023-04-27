package org.jboss.nexus.validation.reporting.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.LogLimiter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.goodies.common.ComponentSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/** Class for deciphering IDs from configuration */
@Named
@Singleton
public class JiraTestReportServerInformation extends ComponentSupport {
	@Inject
	public JiraTestReportServerInformation(JiraTestReportCapabilityDescriptor jiraTestReportCapabilityDescriptor) {
		jiraConfigurationTaskName = checkNotNull(jiraTestReportCapabilityDescriptor).name();
	}

	private String authentication;

	private String jiraBaseURL;

	private String proxyHost;
	private Integer proxyPort;

	private final String jiraConfigurationTaskName;


	private LogLimiter logLimiter = new LogLimiter(LogLimiter.HOUR ,log); // once per hour

	/** Sets a custom log limiter (for testing)
	 *
	 * @param logLimiter log limiter
	 */
	void setLogLimiter(@NotNull LogLimiter logLimiter) {
		this.logLimiter = logLimiter;
	}

	/** Information about Jira project */
	public static class ProjectInformation {
		public ProjectInformation(int id, String key, String name) {
			this.id = id;
			this.key = key;
			this.name = name;
		}

		public final int id;
		public final String key;
		public final String name;
	}

	private final Map<String, ProjectInformation> projects = new HashMap<>();

	private final Map<Integer, ProjectInformation> projectsByID = new HashMap<>();

	/** project ID -> Component name -> Component ID   */
	private final Map<Integer, Map<String, Integer>> components = new HashMap<>();


	private final Map<String, String> priorities = new HashMap<>();

	private Map<String, String> users = new HashMap<>();

	private final ObjectMapper mapper = new ObjectMapper();

	/** Sets the credentials and URL to be used when reaching to Jira.
	 *
	 * @param jiraBaseURL Jira base URL
	 * @param userName username
	 * @param token Jira access token
	 * @param password password if the basic authentication should be used
	 * @param proxyHost host of the proxy server if needed
	 * @param proxyPort port of the proxy server. If not supplied, 3128 is default.
	 */
	public void setJiraConnectionInformation(@NotNull String jiraBaseURL, @Nullable String userName, @Nullable String token, @Nullable String password, @Nullable String proxyHost, @Nullable Integer proxyPort)  {
		jiraBaseURL = jiraBaseURL.trim();

		if(StringUtils.isNotBlank(userName))
			userName = userName.trim();

		if(StringUtils.isNotBlank(token))
			token = token.trim();

		if(StringUtils.isNotBlank(password))
			password = password.trim();

		if(jiraBaseURL.endsWith("/")) {
			this.jiraBaseURL = jiraBaseURL.substring(0, jiraBaseURL.length()-1);
		} else
			this.jiraBaseURL = jiraBaseURL;


		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;

		if(StringUtils.isNotBlank(token)) {
			authentication = "Bearer " + token;
		} else if(StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
			authentication = "Basic " + Base64.encodeBase64String((userName + ':' + password).getBytes(StandardCharsets.UTF_8));
		} else {
			authentication = null;
			log.warn("Neither authentication token nor username and password provided. Access to Jira will be anonymous.");
		}
	}


	/** Prepares connection object to be used to get data
	 *
	 * @param endpoint endpoint to be accessed. It should be in form /rest/api
	 * @return connection prepared to be opened
	 * @throws IOException if there is a problem with createing the connection
	 */
	URLConnection buildConnection(String endpoint) throws IOException {
		URL url = new URL(this.jiraBaseURL + endpoint);

		URLConnection connection;
		if(StringUtils.isBlank(proxyHost)) {
			connection = url.openConnection();
		} else {
			Proxy proxy = new Proxy(Proxy.Type.HTTP,  InetSocketAddress.createUnresolved(proxyHost, proxyPort == null ? 3128 : proxyPort));
			connection = url.openConnection(proxy);
		}
		if(StringUtils.isNotBlank(authentication))
			connection.addRequestProperty("Authorization", authentication);

		connection.addRequestProperty("Accept", "application/json");

		return connection;
	}

	/** Finds project ID by its key (or ID). It loads the project information from the Jira server.
	 *
	 * @param projectKey key or ID of the project
	 * @return id of the project
	 *
	 * @throws RuntimeException if the project is not found or there were some connectivity problems.
	 */
	public int findProjectID(String projectKey) {
		ProjectInformation projectInformation = projects.get(projectKey);
		if (projectInformation != null)
			return projectInformation.id;

		try {
			URLConnection connection = buildConnection("/rest/api/latest/project/"+URLEncoder.encode(projectKey, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20"));

			try (InputStream inputStream = connection.getInputStream()) {
				JsonNode jsonNode = mapper.readTree(inputStream);

				if(jsonNode.isEmpty()) {
					String msg = "Unable to get information about project. Response from the server is empty.";
					logLimiter.error(msg);
					throw new RuntimeException(msg);
				}

				String key, projectName;
				int projectId;

				if((projectId = jsonNode.get("id").asInt()) < 1) {
					String msg = "Project " + projectKey + " can not be found in Jira";
					logLimiter.error(msg);
					throw new RuntimeException(msg);
				} else if (StringUtils.isBlank(key = jsonNode.get("key").asText()) ) {
					String msg = "Project key of " + key + " was not found!";
					logLimiter.error(msg);
					throw new RuntimeException(msg);
				} else if (StringUtils.isBlank(projectName = jsonNode.get("name").asText()) ) {
					String msg = "Project name of " + projectKey + " was not found!";
					logLimiter.error(msg);
					throw new RuntimeException(msg);
				}

				ProjectInformation result = new ProjectInformation(projectId, key, projectName);
				projects.put(projectKey, result);
				projectsByID.put(result.id, result);
				return result.id;
			}
		} catch (MalformedURLException e) {
			String msg = "Malformed Jira URL when retrieving Jira project ID: " + e.getMessage();
			logLimiter.error(msg);
			throw new RuntimeException(msg, e);
		} catch (FileNotFoundException e) {
			String msg = "Project was not found: " + projectKey;
			logLimiter.warn(msg);
			throw new RuntimeException(msg, e);
		} catch (IOException e) {
			String msg = "Error connecting to Jira server: " + e.getMessage();
			logLimiter.error(msg);
			throw new RuntimeException(msg, e);
		}
	}

	/** Finds security ID.
	 *
	 * @param security security ID or security name
	 * @return
	 */
	public String findSecurityLevelID(String security) {
		if(StringUtils.isNumeric(security))
			return security;

		return null; // todo
	}

	/** Tries to get the priority id given the priority name or ID.
	 *
	 * @param priority if number, it is considered to be the priority ID itself. Otherwise, it is being considered a name and the ID is being searched for.
	 *
	 * @return priority ID
	 *
	 * @throws RuntimeException if there is an error communicating with the Jira server or the priority is not found.
	 */
	public String findPriorityID(String priority) {
		if(StringUtils.isNumeric(priority)) {
			return priority;
		}

		String priorityID = priorities.get(priority);
		if(priorityID != null)
			return priorityID;


		String priorityNotFoundMessage = "Priority " + priority + " was not found!";
		if(!priorities.isEmpty()) {
			if(!logLimiter.warn("Priority " + priority + " was not found. I am trying to refresh the priority list.")) {
				throw new RuntimeException(priorityNotFoundMessage);
			}

			// maybe if we refresh the list....
			priorities.clear();
		}

		// read the priorities
		log.debug("Reading priorities from the server");

		try {
			URLConnection connection = buildConnection("/rest/api/latest/priority");

			try (InputStream inputStream = connection.getInputStream()) {
				JsonNode jsonNode = mapper.readTree(inputStream);

				if(jsonNode.isEmpty()) {
					String msg = "Unable to get information about priorities. Response from the server is empty.";
					logLimiter.error(msg);
					throw new RuntimeException(msg);
				}

				jsonNode.elements().forEachRemaining(node -> priorities.put(node.get("name").asText(), node.get("id").asText()));

			}
		} catch (IOException e) {
			String msg = "Error connecting to Jira server: " + e.getMessage();
			logLimiter.error(msg);
			throw new RuntimeException(msg, e);
		}

		priorityID = priorities.get(priority);
		if(priorityID != null)
			return priorityID;

		logLimiter.error(priorityNotFoundMessage);
		throw new RuntimeException(priorityNotFoundMessage);
	}


	/** Tries to get the component id given the priority name or ID.
	 *
	 * @param project project name or ID
	 * @param component if number, it is considered to be the priority ID itself. Otherwise, it is being considered a name and the ID is being searched for.
	 *
	 * @return compoenent ID
	 *
	 * @throws RuntimeException if there is an error communicating with the Jira server or the priority is not found.
	 */
	public Integer findComponentID(String project, String component) {
		if(StringUtils.isNumeric(component)) {
			return Integer.valueOf(component);
		}

		Integer projectID = findProjectID(project);

		Map<String, Integer> projectComponents = components.get(projectID);

		Integer componentID = null;

		if(projectComponents != null) {
			componentID = projectComponents.get(component);
		} else {
			projectComponents = new HashMap<>();
			components.put(projectID, projectComponents);
		}

		if(componentID != null)
			return componentID;

		String componentNotFoundMessage = "Component " + component + " was not found in project "+ project +"!";
		if(!projectComponents.isEmpty()) {
			if(!logLimiter.warn("Component " + component + " was not found. I am trying to refresh the component list.")) {
				throw new RuntimeException(componentNotFoundMessage);
			}

			// maybe if we refresh the list....
			projectComponents.clear();
		}

		// read the priorities
		log.debug("Reading components from the server");

		try {
			URLConnection connection = buildConnection("/rest/api/latest/project/"+projectID+"/components");

			try (InputStream inputStream = connection.getInputStream()) {
				JsonNode jsonNode = mapper.readTree(inputStream);

				if(!jsonNode.isEmpty()) {
					final Map<String, Integer> comps = projectComponents;
					jsonNode.elements().forEachRemaining(node -> comps.put(node.get("name").asText(), node.get("id").asInt()));
				}
			}
		} catch (IOException e) {
			String msg = "Error connecting to Jira server: " + e.getMessage();
			logLimiter.error(msg);
			throw new RuntimeException(msg, e);
		}

		componentID = projectComponents.get(component);
		if(componentID != null)
			return componentID;

		logLimiter.error(componentNotFoundMessage);
		throw new RuntimeException(componentNotFoundMessage);
	}


	public String findSeverityLevelID(String severity) {
		return null; // todo
	}

	public String findUserID(String userName) {
		// fixme maybe not needed
		return null; // todo
	}

	private static final String[] rootRemovedFields = {"id", "self", "key", "expand"};
	private static final String[] fieldsRemovedFields = {"updated", "timespent", "lastViewed", "created", "updated", "votes", "worklog", "progress", "creator", "status", "comment", "archivedby" };
	private static final String[] projectRemovedFields = {"self", "projectTypeKey", "avatarUrls", "projectCategory"};
	private static final String[] issueTypeRemovedFields = {"self", "description", "iconUrl", "avatarId"};
	private static final String[] priorityRemovedFields = {"self", "iconUrl"};
	private static final String[] personRemovedFields = {"self", "emailAddress", "avatarUrls", "displayName", "active", "timeZone"};

	public void tryJiraIssue(JiraReadKnownJiraIssueTaskConfiguration jiraReadKnownJiraIssueTaskConfiguration) {
		if(StringUtils.isBlank(jiraBaseURL)) {
			String message = "Missing link to Jira: Activate and configure "+this.jiraConfigurationTaskName+"!";
			log.error(message);
			jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
			throw new RuntimeException(message);
		}

		String issue = jiraReadKnownJiraIssueTaskConfiguration.getIssue();
		if(StringUtils.isBlank(issue)) {
			String msg = "Issue was not configured!";
			log.error(msg);
			throw new RuntimeException(msg);
		}

		try {
			URLConnection connection = buildConnection("/rest/api/latest/issue/"+ issue);
			JsonNode jsonNode = mapper.readTree(connection.getInputStream());
			ObjectNode objectNode = jsonNode.deepCopy();

			for(String toRemove : rootRemovedFields) {
				objectNode.remove(toRemove);
			}

			ObjectNode fieldNode = (ObjectNode) objectNode.get("fields");
			for(String toRemove : fieldsRemovedFields) {
				fieldNode.remove(toRemove);
			}

			// add project

			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.remove("project");
				ObjectNode project = mapper.createObjectNode();
				project.put("id", "${project-id}"); // FIXME: 24.04.2023 proper value
				//noinspection SpellCheckingInspection
				fieldNode.set("project", project);
			} else {
				ObjectNode project = (ObjectNode) fieldNode.get("project");
				if(project.isMissingNode()) {
					log.error("Missing node - project!");
					String message = "Unexpected format of returned JSON - missing project!";
					jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
					throw new RuntimeException(message);
				}

				for(String toRemove : projectRemovedFields) {
					project.remove(toRemove);
				}
			}

			// add issue type
			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.remove("issuetype");
				ObjectNode issueType = mapper.createObjectNode();
				issueType.put("id", "${issue-type-id}"); // FIXME: 24.04.2023 proper value
				//noinspection SpellCheckingInspection
				fieldNode.set("issuetype", issueType);
			} else {
				ObjectNode issueType = (ObjectNode) fieldNode.get("issuetype");
				if(issueType.isMissingNode()) {
					log.error("Missing node - issue type!");
					String message = "Unexpected format of returned JSON - missing issue type!";
					jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
					throw new RuntimeException(message);
				}

				for(String toRemove : issueTypeRemovedFields) {
					issueType.remove(toRemove);
				}
			}

			// add priority
			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.remove("priority");
				ObjectNode priority = mapper.createObjectNode();
				priority.put("id", "${priority-id}"); // FIXME: 24.04.2023 proper value
				fieldNode.set("priority", priority);
			} else {
				ObjectNode priority = (ObjectNode) fieldNode.get("priority");
				if(priority.isMissingNode()) {
					log.error("Missing node - priority!");
					String message = "Unexpected format of returned JSON - missing priority!";
					jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
					throw new RuntimeException(message);
				}

				for(String toRemove : priorityRemovedFields) {
					priority.remove(toRemove);
				}
			}

			// add labels
			// TODO: 24.04.2023 resolve adding labels from variable

			// add reporter
			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.remove("reporter");
				ObjectNode reporter = mapper.createObjectNode();
				reporter.put("id", "${reporter-id}"); // FIXME: 24.04.2023 proper value
				fieldNode.set("reporter", reporter);
			} else {
				ObjectNode priority = (ObjectNode) fieldNode.get("reporter");
				if(priority.isMissingNode()) {
					log.error("Missing node - reporter!");
					String message = "Unexpected format of returned JSON - missing reporter!";
					jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
					throw new RuntimeException(message);
				}

				for(String toRemove : personRemovedFields) {
					priority.remove(toRemove);
				}
			}

			// add assignee
			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.remove("assignee");
				ObjectNode assignee = mapper.createObjectNode();
				assignee.put("id", "${assignee-id}"); // FIXME: 24.04.2023 proper value
				fieldNode.set("assignee", assignee);
			} else {
				ObjectNode assignee = (ObjectNode) fieldNode.get("assignee");
				if(assignee.isMissingNode()) {
					log.error("Missing node - assignee!");
					String message = "Unexpected format of returned JSON - missing assignee!";
					jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
					throw new RuntimeException(message);
				}

				for(String toRemove : personRemovedFields) {
					assignee.remove(toRemove);
				}
			}

			// add summary
			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.put("summary", "${summary}"); // FIXME: 24.04.2023 proper value
			}

			// add description
			if(jiraReadKnownJiraIssueTaskConfiguration.getUseVelocityVariables()) {
				fieldNode.put("description", "${description}"); // FIXME: 24.04.2023 proper value
			}

			// clean field null values
			if(jiraReadKnownJiraIssueTaskConfiguration.getWipeNullFields()) {
				Iterator<JsonNode> it = fieldNode.elements();
				while (it.hasNext()) {
					JsonNode node = it.next();
					if (node.isNull())
						it.remove();
				}
			}

			//---------------------------------- this is the END -------------------------------
			jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(objectNode.toPrettyString());
		} catch (FileNotFoundException e) {
			String message = "Examining issue "+issue+": Issue was not found in Jira. Check the issue number and your permissions in Jira.";
			log.warn(message);
			jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
			throw new RuntimeException(message);
		} catch (IOException e) {
			String message = "Examining issue "+issue+": "+e.getMessage();
			log.warn(message);
			jiraReadKnownJiraIssueTaskConfiguration.setLatestResult(message);
			throw new RuntimeException(message);
		}
	}


	String getAuthentication() {
		return authentication;
	}
}
