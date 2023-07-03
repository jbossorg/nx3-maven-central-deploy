package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.MavenCentralDeployTaskDescriptor;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.formfields.*;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jboss.nexus.validation.reporting.jira.MavenCentralDeployTaskWithJiraDescriptor.TYPE_ID;

@Named(TYPE_ID)
@Singleton
public class MavenCentralDeployTaskWithJiraDescriptor extends MavenCentralDeployTaskDescriptor {

    public static final String TYPE_ID = "mvn.central.deploy.with.jira";

    private interface Messages
            extends MessageBundle
    {
        @DefaultMessage("Create test issue")
        String createTestReportLabel();

        @DefaultMessage("If checked, instead of doing the actual deployment to Maven Central a Jira issue is being created using fictive errors to validate the issue configuration.")
        String createTestReportHelp();

        @DefaultMessage("Maven Central Deployment with Jira")
        String name();

    }
    private static final MavenCentralDeployTaskWithJiraDescriptor.Messages messages = I18N.create(MavenCentralDeployTaskWithJiraDescriptor.Messages.class);

    @SuppressWarnings("rawtypes")
    private static final FormField[] taskFieldsWithJira;




    static {
        List<String> ignoredJiraFields = new ArrayList<>();
        ignoredJiraFields.add(JiraTestReportCapabilityConfiguration.JIRA_BASE_URL);
        ignoredJiraFields.add(JiraTestReportCapabilityConfiguration.PROXY_HOST);
        ignoredJiraFields.add(JiraTestReportCapabilityConfiguration.PROXY_PORT);
        ignoredJiraFields.add(JiraTestReportCapabilityConfiguration.USER_NAME);
        ignoredJiraFields.add(JiraTestReportCapabilityConfiguration.USER_PASSWORD);
        ignoredJiraFields.add(JiraTestReportCapabilityConfiguration.TOKEN);


        @SuppressWarnings("rawtypes") List<FormField> fields = new ArrayList<>();

        fields.add(new CheckboxFormField(MavenCentralDeployTaskWithJiraConfiguration.CREATE_TEST_TICKET, messages.createTestReportLabel(), messages.createTestReportHelp(), false));


        Collections.addAll(fields, taskFields);

        fields.add(new TextAreaFormField("jira_header", "", "", false, null, true)
                .withInitialValue("---------------- Jira Configuration ----------------"));

        // re-create the mandatory fields from JiraTestReportCapabilityDescriptor, so they are always optional here. Default values exist in JiraTestReportCapabilityDescriptor
        JiraTestReportCapabilityDescriptor.formFields.stream()
                .filter(f -> ! ignoredJiraFields.contains(f.getId())).forEach(field -> {
                    if(field.isRequired()) {
                        // we must duplicate required fields, because we do not want them to be mandatory here
                        switch(field.getType()) {
                            case "text-area":
                                TextAreaFormField textAreaFormField = new TextAreaFormField(field.getId(), field.getLabel(), field.getHelpText(), false, field.getRegexValidation(), field.isReadOnly());
                                if(field.getInitialValue() != null) {
                                    textAreaFormField.withInitialValue(((TextAreaFormField)field).getInitialValue());
                                }
                                fields.add(textAreaFormField);
                                break;
                            case "string":
                                StringTextFormField stringTextFormField = new StringTextFormField(field.getId(), field.getLabel(), field.getHelpText(), false, field.getRegexValidation());
                                if(field.getInitialValue() != null)
                                    stringTextFormField.withInitialValue(((StringTextFormField)field).getInitialValue());
                                fields.add(stringTextFormField);
                                break;
                            case "number":
                                NumberTextFormField numberTextFormField = new NumberTextFormField(field.getId(), field.getLabel(), field.getHelpText(), false, field.getRegexValidation());
                                if(field.getInitialValue() != null)
                                    numberTextFormField.withInitialValue(((NumberTextFormField)field).getInitialValue());
                                fields.add(numberTextFormField);
                                break;
                            case "boolean":
                                CheckboxFormField checkboxFormField = new CheckboxFormField(field.getId(), field.getLabel(), field.getHelpText(), false);
                                if(field.getInitialValue() != null)
                                    checkboxFormField.withInitialValue(((CheckboxFormField)field).getInitialValue());
                                fields.add(checkboxFormField);
                                break;
                            default:
                                throw new RuntimeException("Programming error - Unexpected field type!");
                        }

                    } else
                        fields.add(field);
                });

        taskFieldsWithJira = fields.toArray(new FormField[0]);

    }



    public MavenCentralDeployTaskWithJiraDescriptor() {
        super(TYPE_ID, MavenCentralDeployTaskWithJiraTask.class, messages.name(), TaskDescriptorSupport.VISIBLE,
                TaskDescriptorSupport.EXPOSED, taskFieldsWithJira
        );
    }

    @Override
    public TaskConfiguration createTaskConfiguration() {
        return new MavenCentralDeployTaskWithJiraConfiguration();
    }
}

