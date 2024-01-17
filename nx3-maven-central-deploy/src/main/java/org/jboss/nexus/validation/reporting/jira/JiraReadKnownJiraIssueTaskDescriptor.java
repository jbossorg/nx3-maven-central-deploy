package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.MavenCentralDeployTaskDescriptor;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;

@Named(JiraReadKnownJiraIssueTask.TYPE)
@Singleton
public class JiraReadKnownJiraIssueTaskDescriptor extends TaskDescriptorSupport implements Taggable {

	public JiraReadKnownJiraIssueTaskDescriptor() {
		super(JiraReadKnownJiraIssueTask.TYPE, JiraReadKnownJiraIssueTask.class, messages.name(), true, true,
				new StringTextFormField(JiraReadKnownJiraIssueTaskConfiguration.ISSUE, messages.issueLabel(), messages.issueHelp(), true),
				new CheckboxFormField(JiraReadKnownJiraIssueTaskConfiguration.USE_VELOCITY_VARIABLES, messages.useVelocityVariablesLabel(), messages.useVelocityVariablesHelp(), false),
				new CheckboxFormField(JiraReadKnownJiraIssueTaskConfiguration.WIPE_NULL_FIELDS, messages.wipeOutNullFieldsLabel(), messages.wipeOutNullFieldsHelp(), false).withInitialValue(true),
				new TextAreaFormField(JiraReadKnownJiraIssueTaskConfiguration.LATEST_RESULT, messages.latestResultLabel(), messages.latestResultHelp(), false, null, true),
				new TextAreaFormField(JiraReadKnownJiraIssueTaskConfiguration.DESCRIPTION, messages.descriptionLabel(), messages.descriptionHelp(), false, null, true)
        );
	}

	private interface Messages
			extends MessageBundle {
		@DefaultMessage("Issue")
		String issueLabel();

		@DefaultMessage("Choose an existing issue from Jira you would like to investigate.")
		String issueHelp();

		@DefaultMessage("Latest result")
		String latestResultLabel();

		@DefaultMessage("The last response from Jira, if everything was OK a JSON object with the information about the Jira issue you chose in Issue field.")
		String latestResultHelp();

		@DefaultMessage("Use Velocity variables")
		String useVelocityVariablesLabel();


		@DefaultMessage("Wipe out empty fields")
		String wipeOutNullFieldsLabel();

		@DefaultMessage("If checked, the method removes all empty fields from the issue.")
		String wipeOutNullFieldsHelp();

		@DefaultMessage("If checked, in the generated report the known fields (like project or issue type) will be replaced by Velocity variables, that are provided by the deployment tasks.")
		String useVelocityVariablesHelp();

		@DefaultMessage("Description")
		String descriptionLabel();

		@DefaultMessage("Description, that was analyzed from the analyzed ticket.")
		String descriptionHelp();

		@DefaultMessage("MCD: Investigate existing Jira issue!")
		String name();
	}

	private static final JiraReadKnownJiraIssueTaskDescriptor.Messages messages = I18N.create(JiraReadKnownJiraIssueTaskDescriptor.Messages.class);

	@Override
	public Set<Tag> getTags() {
		return Collections.singleton(Tag.categoryTag(MavenCentralDeployTaskDescriptor.CATEGORY));
	}

	@Override
	public boolean allowConcurrentRun() {
		// basically only manual schedule makes any sense
		return false;
	}


	@Override
	public TaskConfiguration createTaskConfiguration() {
		return new JiraReadKnownJiraIssueTaskConfiguration();
	}
}
