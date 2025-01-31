package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.validation.reporting.TestReportCapabilityConfigurationParent;
import org.jboss.nexus.validation.reporting.TestReportCapabilityDescriptorParent;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.*;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** This capability configures default information for all Jira Tasks
 *
 */
@SuppressWarnings("TextBlockMigration")
@Named(JiraTestReportCapabilityDescriptor.TYPE_ID)
@AvailabilityVersion(from = "1.0")
public class JiraTestReportCapabilityDescriptor extends TestReportCapabilityDescriptorParent {
	public static final String TYPE_ID = "nx3Deploy.jira.report";

	private static final CapabilityType CAPABILITY_TYPE = CapabilityType.capabilityType(TYPE_ID);


	private interface Messages
			extends MessageBundle {

		@DefaultMessage("Jira base URL")
		String jiraBaseUrlLabel();

		@DefaultMessage("Base URL of for any Jira operation, e.g https://issues.company.org")
		String jiraBaseUrlHelp();

		@DefaultMessage("Proxy host")
		String proxyHostLabel();

		@DefaultMessage("Host name or IP of the proxy server if your company uses one.")
		String proxyHostHelp();

		@DefaultMessage("Proxy port")
		String proxyPortLabel();

		@DefaultMessage("Port number of your proxy server if your company uses one. 3128 is the default.")
		String proxyPortHelp();

		@DefaultMessage("Username")
		String userNameLabel();

		@DefaultMessage("Username for authentication for Jira operations. Use only if your Jira server uses basic authentication.")
		String userNameHelp();

		@DefaultMessage("Password")
		String passwordLabel();

		@DefaultMessage("Password for authentication for Jira operations. Use only if your Jira server uses basic authentication.")
		String passwordHelp();

		@DefaultMessage("Authentication token")
		String tokenLabel();

		@DefaultMessage("Token for authenticating in Jira using personal access tokens (recommended by Atlassian).")
		String tokenHelp();

		@DefaultMessage("Project")
		String projectLabel();

		@DefaultMessage("Project ID or project key for creating the issues.")
		String projectHelp();

		@DefaultMessage("Summary")
		String summaryLabel();

		@DefaultMessage("Issue summary for the newly created issues. Velocity template variables are allowed.")
		String summaryHelp();

		@DefaultMessage("Description")
		String descriptionLabel();

		@DefaultMessage("Issue description. This template will be used for creating the error report.")
		String descriptionHelp();

		@DefaultMessage("Issue type")
		String issueTypeLabel();

		@DefaultMessage("ID or name of the issue type")
		String issueTypeHelp();

		@DefaultMessage("Components")
		String componentsLabel();

		@DefaultMessage("Comma separated list of components to be applied on the ticket. Velocity templating support.")
		String componentsHelp();

		@DefaultMessage("Labels")
		String labelsLabel();

		@DefaultMessage("Comma separated list of labels to be applied on the ticket. Velocity templating support. Beware: Jira does not allow spaces inside labels even if you enclose the label in quotes!")
		String labelsHelp();

		@DefaultMessage("Priority")
		String priorityLabel();

		@DefaultMessage("Priority of the issue. ID or name.")
		String priorityHelp();

		@DefaultMessage("Security")
		String securityLabel();

		@DefaultMessage("Security level of the created issue.")
		String securityHelp();

		@DefaultMessage("Assignee")
		String assigneeLabel();

		@DefaultMessage("The person to be assigned the newly created ticket. ")
		String assigneeHelp();

		@DefaultMessage("Reporter")
		String reporterLabel();

		@DefaultMessage("If there is a requirement of setting a specific reporter, use this field. ")
		String reporterHelp();


		@DefaultMessage("Bug")
		String defaultIssueType();

		@DefaultMessage("Maven Central Synchronization Error ${name} - run ${run}")
		String defaultIssueSummary();

		@DefaultMessage("Issue main template")
		String templateLabel();

		@DefaultMessage("Main template for creating the issue. For generating the actual report use Description field rather than this one. ")
		String templateHelp();


		@DefaultMessage("MCD - Jira Reports Default Configuration")
		String name();
	}

	private static final Messages messages = I18N.create(JiraTestReportCapabilityDescriptor.Messages.class);

	private static final String DEFAULT_TEMPLATE = "{\n" +
			"  \"fields\" : {\n" +
			"    \"labels\" : [${labels} ],\n" +
			"    \"components\" : [${components} ],\n" +
			"    \"description\" : \"${description}\",\n" +
			"    \"summary\" : \"${summary}\",\n" +
			"    \"project\" : {\n" +
			"      \"id\" : \"${project_id}\"\n" +
			"    },\n" +
			"    \"issuetype\" : {\n" +
			"      \"id\" : \"${issue_type_id}\"\n" +
			"    },\n" +
			"    \"priority\" : {\n" +
			"      \"id\" : \"${priority_id}\"\n" +
			"    },\n" +
			"    \"reporter\" : {\n" +
			"      \"id\" : \"${reporter_id}\"\n" +
			"    },\n" +
			"    \"assignee\" : {\n" +
			"      \"id\" : \"${assignee_id}\"\n" +
			"    }\n" +
			"  }\n" +
			"}";

	private static final String DEFAULT_DESCRIPTION = "h2. $name $run on $date\\r\\n\n" +
			"\\r\\n\n" +
			"{*}Processed{*}: $processed components from {_}$repository{_}.\\r\\n\n" +
			"{*}Problems found{*}: ${errors.size()}\\r\\n\n" +
			"h2. Details\\r\\n\n" +
			"\\r\\n\n" +
			"||Group||Artifact||Version||Error||\\r\\n\n" +
			"#set ($previous=\"\")\n" +
			"#foreach( $error in $errors )\n" +
			"#if( $foreach.count < 501 ) \n" +
			"|$error.component.group()|$error.component.name()|$error.component.version()|$error.problem|\\r\\n\n" +
			"#end\n" +
			"#end";

	@SuppressWarnings("rawtypes")
	static List<FormField> formFields = new ArrayList<>();

	static {
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.JIRA_BASE_URL, messages.jiraBaseUrlLabel(), messages.jiraBaseUrlHelp(), true));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.PROXY_HOST, messages.proxyHostLabel(), messages.proxyHostHelp(), false));
		formFields.add(new NumberTextFormField(JiraTestReportCapabilityConfiguration.PROXY_PORT, messages.proxyPortLabel(), messages.proxyPortHelp(), false  ).withInitialValue(3128));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.USER_NAME, messages.userNameLabel(), messages.userNameHelp(), false));
		formFields.add(new PasswordFormField(JiraTestReportCapabilityConfiguration.USER_PASSWORD, messages.passwordLabel(), messages.passwordHelp(), false ));
		formFields.add(new PasswordFormField(JiraTestReportCapabilityConfiguration.TOKEN, messages.tokenLabel(), messages.tokenHelp(), false));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.PROJECT, messages.projectLabel(), messages.projectHelp(), true));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.SUMMARY, messages.summaryLabel(), messages.summaryHelp(), true).withInitialValue(messages.defaultIssueSummary()));
		formFields.add(new TextAreaFormField(JiraTestReportCapabilityConfiguration.DESCRIPTION, messages.descriptionLabel(), messages.descriptionHelp(), true).withInitialValue(DEFAULT_DESCRIPTION));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.ISSUE_TYPE, messages.issueTypeLabel(), messages.issueTypeHelp(), true).withInitialValue(messages.defaultIssueType()));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.COMPONENTS, messages.componentsLabel(), messages.componentsHelp(), false));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.LABELS, messages.labelsLabel(), messages.labelsHelp(), false));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.PRIORITY, messages.priorityLabel(), messages.priorityHelp(), false));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.SECURITY, messages.securityLabel(), messages.securityHelp(), false));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.ASSIGNEE, messages.assigneeLabel(), messages.assigneeHelp(), false));
		formFields.add(new StringTextFormField(JiraTestReportCapabilityConfiguration.REPORTER, messages.reporterLabel(), messages.reporterHelp(), false));
		formFields.add(new TextAreaFormField(JiraTestReportCapabilityConfiguration.TEMPLATE, messages.templateLabel(), messages.templateHelp(), true).withInitialValue(DEFAULT_TEMPLATE));
	}


	public JiraTestReportCapabilityDescriptor() {
		super(formFields);
	}

	@Override
	public CapabilityType type() {
		return CAPABILITY_TYPE;
	}

	@Override
	public String name() {
		return messages.name();
	}

	@Override
	protected TestReportCapabilityConfigurationParent createConfig(Map<String, String> properties) {
		return new JiraTestReportCapabilityConfiguration(properties);
	}
}
