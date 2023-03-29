package org.jboss.nexus.validation.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.LogLimiter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.goodies.common.ComponentSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Class for deciphering IDs from configuration */
@Named
@Singleton
public class JiraTestReportServerInformation extends ComponentSupport {
	public JiraTestReportServerInformation() {
	}

	private String authentication;

	private String jiraBaseURL;

	private String proxyHost;
	private Integer proxyPort;

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

	private Map<String, ProjectInformation> projects = new HashMap<>();

	private Map<Integer, ProjectInformation> projectsByID = new HashMap<>();

	private Map<String, String> severityLevels = new HashMap<>();

	private Map<String, String> priorities = new HashMap<>();

	private Map<String, String> securityLevels = new HashMap<>();

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




	public String findSeverityLevelID(String severity) {
		return null; // todo
	}
		public String findUserID(String userName) {
		// fixme maybe not needed
		return null; // todo
	}


	public void tryJira() {
		try {
			URLConnection connection = buildConnection("/rest/api/latest/issue/NEXUS-180");

			JsonNode jsonNode = mapper.readTree(connection.getInputStream());

			log.debug(jsonNode.asText());


		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	String getAuthentication() {
		return authentication;
	}
}
