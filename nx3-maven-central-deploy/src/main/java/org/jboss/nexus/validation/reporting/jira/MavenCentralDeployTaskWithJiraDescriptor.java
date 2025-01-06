package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.DescriptorUtils;
import org.jboss.nexus.MavenCentralDeployTaskDescriptor;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
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
@AvailabilityVersion(from = "1.0")
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
        ArrayList<String> ignoredJiraFields = new ArrayList<>();
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
        taskFieldsWithJira = DescriptorUtils.combineDescriptors(fields, ignoredJiraFields, JiraTestReportCapabilityDescriptor.formFields);
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

