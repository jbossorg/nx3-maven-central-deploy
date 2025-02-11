package org.jboss.nexus.validation.reporting.jira;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.nexus.scheduling.TaskState;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("CdiInjectionPointsInspection")
@Named(JiraTestReportCapabilityDescriptor.TYPE_ID)
public class JiraTestReportCapability extends TestReportCapability<JiraTestReportCapabilityConfiguration> {

	@Inject
	public JiraTestReportCapability(JiraTestReportServerInformation jiraTestReportServerInformation) {
		this.jiraTestReportServerInformation = checkNotNull(jiraTestReportServerInformation);
	}


	private final JiraTestReportServerInformation jiraTestReportServerInformation;

	@Override
	protected JiraTestReportCapabilityConfiguration createConfig(Map<String, String> properties) {
		return new JiraTestReportCapabilityConfiguration(properties);
	}

	@Override
	protected void configure(JiraTestReportCapabilityConfiguration config) {
		super.configure(config);
		jiraTestReportServerInformation.setJiraConnectionInformation(getConfig().getDefaultJiraConfiguration().getJiraBaseUrl(), getConfig().getDefaultJiraConfiguration().getUserName(), getConfig().getDefaultJiraConfiguration().getToken(),getConfig().getDefaultJiraConfiguration().getPassword(), getConfig().getDefaultJiraConfiguration().getProxyHost(), getConfig().getDefaultJiraConfiguration().getProxyPort());
	}

	@Override
	public void onActivate(JiraTestReportCapabilityConfiguration config) {
		super.onActivate(config);
		jiraTestReportServerInformation.setJiraConnectionInformation(getConfig().getDefaultJiraConfiguration().getJiraBaseUrl(), getConfig().getDefaultJiraConfiguration().getUserName(), getConfig().getDefaultJiraConfiguration().getToken(),getConfig().getDefaultJiraConfiguration().getPassword(), getConfig().getDefaultJiraConfiguration().getProxyHost(), getConfig().getDefaultJiraConfiguration().getProxyPort());
	}

	@Nullable
	@Override
	protected String renderStatus() {
		if(isConfigured()) {
			Map<String, Object> values = new HashMap<>();
			values.put("baseURL", getConfig().getDefaultJiraConfiguration().getJiraBaseUrl());
			if(StringUtils.isNotBlank(getConfig().getDefaultJiraConfiguration().getJiraBaseUrl())) {
				values.put("proxy", getConfig().getDefaultJiraConfiguration().getProxyHost());
				values.put("proxy_port", getConfig().getDefaultJiraConfiguration().getProxyPort());
			}

			values.put("username", getConfig().getDefaultJiraConfiguration().getUserName());
			if(StringUtils.isNotBlank(getConfig().getDefaultJiraConfiguration().getToken())) {
				values.put("credentials", "user token") ;
			} else if(StringUtils.isNotBlank(getConfig().getDefaultJiraConfiguration().getPassword())) {
				values.put("credentials", "basic authentication") ;
			} else {
				values.put("credentials", "no user name nor password defined!") ;
			}

			if(context().isActive()) {
				try {
					String user = jiraTestReportServerInformation.findCurrentUserInformation();
					values.put("status", "ACTIVE");
					values.put("jiraDisplayUser", user);
				} catch (IOException e) {
					values.put("status", "BROKEN");
					values.put("error", e.getMessage());
				} catch (NullPointerException e) {
					values.put("status", "Jira Reporting is NOT OPERATIONAL");
					values.put("error", "Unknown response structure. A field in Jira response is missing.");
				}
			} else values.put("status", "DISABLED");

			return render("jira-status.vm", values);

		} else
			return "The capability is not configured!";
	}

	@Nullable
	@Override
	protected String renderDescription() {
		return "Jira reporting of Maven Central Deployment";
	}

	private String template;
	private String description;

	/** Converts the data to variables based on the following rules:<br>
	 * - direct variables from mavenCentralDeployTaskConfiguration have precedence <br>
	 * - if no direct variable exists and mavenCentralDeployTaskConfiguration is compatible with {@link MavenCentralDeployTaskWithJiraTask}, use the designated field from this.<br>
	 * - if still undecided, use common value from jiraTestReportCapabilityConfiguration
	 *
	 * @param mavenCentralDeployTaskConfiguration current's task configuration
	 * @param jiraTestReportCapabilityConfiguration default Jira configuration
	 * @param printVariables  list of default variables for configuration {@link org.jboss.nexus.TemplateRenderingHelper#render(String, Map)}
	 */
	Map<String, Object>  convertVariables(Map<String, Object> printVariables, MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, JiraTestReportCapabilityConfiguration jiraTestReportCapabilityConfiguration) {
		// default configuration from Jira plugin
		Map<String, Object> result = new HashMap<>();

		//  let us predefine the lists, that may cause error in json if not translated to a comma separated list
		result.put(JiraTestReportCapabilityConfiguration.LABELS, "");
		result.put(JiraTestReportCapabilityConfiguration.COMPONENTS, "");

		result.putAll(jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().asMap());

		// values from task configuration if possible
		result.putAll((mavenCentralDeployTaskConfiguration).asMap() );

		// if default configuration has component(s) set, but Jira specific task has a different project set (probably wrong component by default), remove it from the configuration
		if(! Objects.equals(jiraTestReportCapabilityConfiguration.getDefaultJiraConfiguration().getProject(), mavenCentralDeployTaskConfiguration.getString(JiraTestReportCapabilityConfiguration.PROJECT))
			&& StringUtils.isNotEmpty(mavenCentralDeployTaskConfiguration.getString(JiraTestReportCapabilityConfiguration.PROJECT))
		   && result.containsKey(JiraTestReportCapabilityConfiguration.COMPONENTS)
		   && !mavenCentralDeployTaskConfiguration.containsKey(JiraTestReportCapabilityConfiguration.COMPONENTS)
		) {
			result.put(JiraTestReportCapabilityConfiguration.COMPONENTS, "");
		}

		// variable overload of those values
		result.putAll(printVariables);

		// remove sensitive stuff from being accessible in the template and save the values for eventual use
		this.template = (String) result.remove(JiraTestReportCapabilityConfiguration.TEMPLATE); // let us prevent endless cycle of template rendering itself just in case
		this.description = (String) result.remove(JiraTestReportCapabilityConfiguration.DESCRIPTION);

		// remove security sensitive stuff
		result.remove(JiraTestReportCapabilityConfiguration.TOKEN);
		result.remove(JiraTestReportCapabilityConfiguration.USER_PASSWORD);

		return result;
	}


	@Override
	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, List<FailedCheck> listOfFailures, Map<String, Object> printVariables) {
		JiraTestReportCapabilityConfiguration configuration = mavenCentralDeploy.findConfigurationForPlugin(JiraTestReportCapabilityConfiguration.class) ;

		if(configuration == null)
			return; // feature is disabled or not configured

		if(
				// creating test Jira issues is considered troubleshooting so try to create the ticket as many times as necessary
				mavenCentralDeployTaskConfiguration instanceof MavenCentralDeployTaskWithJiraConfiguration && !((MavenCentralDeployTaskWithJiraConfiguration)mavenCentralDeployTaskConfiguration).getCreateTestTicket()

				&& mavenCentralDeployTaskConfiguration.hasLastRunState() && Objects.requireNonNull(mavenCentralDeployTaskConfiguration.getLastRunState()).getEndState().equals(TaskState.FAILED))
			return; // real deployment failed previously, so we do not want to report errors over and over.

		Objects.requireNonNull(templateHelper);

		Map<String, Object> processedVariables = convertVariables(printVariables, mavenCentralDeployTaskConfiguration, configuration);

		if(StringUtils.isBlank(template))
			throw new RuntimeException("Template for Jira ticket is not defined!");

		if(StringUtils.isBlank(description))
			throw new RuntimeException("The template for ticket description is not configured!");

		// we must translate the keys and values to IDs
		// project
		String project = (String) processedVariables.get(JiraTestReportCapabilityConfiguration.PROJECT);
		if(StringUtils.isEmpty(project))
			throw new RuntimeException("Not able to create the Jira report: Project is not defined!");
		processedVariables.put(JiraTestReportServerInformation.PROJECT_ID, jiraTestReportServerInformation.findProjectID(project));

		// issue type
		String issue = (String) processedVariables.get(JiraTestReportCapabilityConfiguration.ISSUE_TYPE);
		if(StringUtils.isEmpty(issue))
			throw new RuntimeException("Not able to create the Jira report: Issue type for it is not defined!");
		processedVariables.put(JiraTestReportServerInformation.ISSUE_TYPE_ID, jiraTestReportServerInformation.findIssueTypeID(issue));

		// priority (not required)
		String priority = (String) processedVariables.get(JiraTestReportCapabilityConfiguration.PRIORITY);
		if(StringUtils.isNotEmpty(priority)) {
			processedVariables.put(JiraTestReportServerInformation.PRIORITY_ID, jiraTestReportServerInformation.findPriorityID(priority));
		}

		// security (not required)
		String security = (String) processedVariables.get(JiraTestReportCapabilityConfiguration.SECURITY);
		if(StringUtils.isNotEmpty(security)) {
			processedVariables.put(JiraTestReportServerInformation.SECURITY_LEVEL_ID, jiraTestReportServerInformation.findSecurityLevelID(project, security));
		}

		// process labels
		String labels = (String) processedVariables.get(JiraTestReportCapabilityConfiguration.LABELS);
		if(StringUtils.isNotBlank(labels)) {
			String[] splitLabels = normalizeStrings(labels);
			processedVariables.put(JiraTestReportCapabilityConfiguration.LABELS, String.join(",", splitLabels));
		}

		// process components
		String components = (String) processedVariables.get(JiraTestReportCapabilityConfiguration.COMPONENTS);
		if(StringUtils.isNotBlank(components)) {
			String[] splitComponents = normalizeStrings(components);
			for(int i = 0; i < splitComponents.length; i++) {
				String component = splitComponents[i].substring(1, splitComponents[i].length()-1);
				splitComponents[i] = "\""+jiraTestReportServerInformation.findComponentID(project, component)+"\"";
			}

			String c = "{ \"id\" : " +   String.join(" }, { \"id\" : ", splitComponents) +" }";
			processedVariables.put(JiraTestReportCapabilityConfiguration.COMPONENTS, String.join(",", c));
		}


		try {
			// We allow CRLF in the description field, but we are not allowed to remain them in the field.
			description = templateHelper.render(description, processedVariables).replaceAll("(\\r)?\\n", "");
			processedVariables.put(JiraTestReportCapabilityConfiguration.DESCRIPTION, description);
		} catch (RuntimeException e) {
			String message = "Error processing Jira ticket description template: " + e.getMessage();
			log.error(message, e);
			throw new RuntimeException(message);
		}

		String jsonString;
		try {
			jsonString = templateHelper.render(template, processedVariables);
			jsonString = templateHelper.render(jsonString, processedVariables); // summary, some user defined variables and such may also use variables so let's re-apply
		} catch (RuntimeException e) {
			String message = "Error processing issue main Jira ticket template: " + e.getMessage();
			log.error(message, e);
			throw new RuntimeException(message);
		}

		jsonString = fixProblematicArrays(jsonString);

		try {
			// cleanup of the JSON
			JsonNode jsonNode = (new JsonMapper()).readTree(jsonString);
			removeNotTranslatedVelocityVariables(jsonNode);

			jsonString = jsonNode.toString();
		} catch (JsonProcessingException e) {
			log.error("Error creating issue JSON: "+e.getMessage());

			throw new RuntimeException(e);
		}

		try {
			HttpURLConnection urlConnection = jiraTestReportServerInformation.buildConnection("/rest/api/latest/issue");
			urlConnection.setRequestMethod("POST");
			urlConnection.setRequestProperty("Content-type", "application/json");
			urlConnection.setDoOutput(true);
			try(OutputStream outputStream = urlConnection.getOutputStream()) {
				outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
			}

			if(urlConnection.getResponseCode() == 201) {
				try(InputStream inputStream = JiraTestReportServerInformation.giveDecompressedInputStream(urlConnection)) {
					JsonNode jsonNode = (new ObjectMapper()).readTree(inputStream);
					String message = "Issue created: " + jsonNode.get("key");
					log.debug(message);
					mavenCentralDeployTaskConfiguration.setLatestStatus(message);
				}
			} else {
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(JiraTestReportServerInformation.giveDecompressedErrorStream(urlConnection)))) { // consume the response anyway
					String message = "Unable to create an issue in Jira: " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage();
					log.error(message);
					String details = "Jira error details: "+reader.lines().collect(Collectors.joining("\n"));
					if(StringUtils.isNotBlank(details)) {
						log.debug(details);
						message += "\n"+details;
					}
					throw new RuntimeException(message);
				}
			}
		} catch (IOException e) {
			String message = "Error creating Issue in Jira: " + e.getMessage();
			log.error(message);
			throw new RuntimeException(message);
		}
	}

	/** Splits comma separated list of strings and makes sure all are wrapped by double quote.
	 *
	 * @param string string to normalize
	 * @return tokenized array with quote-wrapped strings
	 */
	private static String[] normalizeStrings(@NotNull final String string) {
		String[] splitLabels = string.trim().split("\\s*,\\s*");
		for(int i = 0; i < splitLabels.length; i++ ) {
			if(!splitLabels[i].startsWith("\""))
				splitLabels[i] = "\""+ splitLabels[i];
			if(!splitLabels[i].endsWith("\""))
				splitLabels[i] += "\"";
		}
		return splitLabels;
	}

	/** Fixes problematic arrays aka "labels" : ["label1" "label2"]
	 *
	 * @param jsonString invalid json with problematic arrays
	 *
	 * @return fixed json
	 */
	static String fixProblematicArrays(String jsonString) {
		StringBuilder builder = new StringBuilder();

		try (StringReader stringReader = new StringReader(jsonString)) {
			boolean inArray = false;
			boolean inQuote = false;
			boolean firstInArray = true;
			boolean commaApplied = false;
			int character  = stringReader.read();
			do {

				switch (character) {
					case '{':
						inArray = false; // we are not fixing complicated stuff
						break;
					case '[':
						inArray = true;
						inQuote = false;
						firstInArray = true;
						commaApplied = false;
						break;
					case ']':
						inArray = false;
						inQuote = false;
						break;
					case ',':
						commaApplied = true;
						break;
					case '\"':
						if(inArray) {
							if(firstInArray) {
								firstInArray = false;
								inQuote = true;
							} else {
								if(inQuote) {
									inQuote = false;
									commaApplied = false;
								} else {
									if(!commaApplied) {
										builder.append(","); // add the missing comma
									}

									inQuote = true;
								}
							}

						}
						break;

				}
				builder.append((char)character);
				character = stringReader.read();
			} while (character > 0 );
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


		return builder.toString();
	}

	/** Method crawls through the jsonNode and records under it and removes the one,
	 * that contain unresolved Velocity variables (contain text ${.+})
	 *
	 * @param jsonNode json node to check for being removed
	 * @return true if the current node should be removed
	 */
	boolean removeNotTranslatedVelocityVariables(JsonNode jsonNode) {
		if(jsonNode.isTextual()) {
			if(jsonNode.asText().contains("${")) {
				return true;
			}
		}

		if(jsonNode.isEmpty())
			return false;

		boolean modified = false;
		Iterator<JsonNode> iterator = jsonNode.iterator();
		while (iterator.hasNext()) {
			JsonNode n = iterator.next();
			if(removeNotTranslatedVelocityVariables(n)) {
				iterator.remove();
				modified = true;
			}
		}

		// if we removed a node, and it resulted ind emptied structure, remove it as well
		return modified && jsonNode.isEmpty();
	}
}
