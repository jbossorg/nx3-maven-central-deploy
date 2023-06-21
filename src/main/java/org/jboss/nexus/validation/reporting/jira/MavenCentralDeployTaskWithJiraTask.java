package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.MavenCentralDeploy;
import org.jboss.nexus.MavenCentralDeployTask;
import org.jboss.nexus.TemplateRenderingHelper;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jboss.nexus.validation.reporting.jira.MavenCentralDeployTaskWithJiraDescriptor.TYPE_ID;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;

@Named(TYPE_ID)
@TaskLogging(NEXUS_LOG_ONLY)
public class MavenCentralDeployTaskWithJiraTask extends MavenCentralDeployTask {

    @Inject
    public MavenCentralDeployTaskWithJiraTask(MavenCentralDeploy mavenCentralDeploy, TemplateRenderingHelper templateRenderingHelper, JiraTestReportCapability jiraTestReportCapability) {
        super(mavenCentralDeploy);
        this.templateRenderingHelper = checkNotNull(templateRenderingHelper);
        this.jiraTestReportCapability = checkNotNull(jiraTestReportCapability);
    }

    private final TemplateRenderingHelper templateRenderingHelper;

    private final JiraTestReportCapability jiraTestReportCapability;

    @Override
    protected String execute() {
        MavenCentralDeployTaskWithJiraConfiguration configuration = (MavenCentralDeployTaskWithJiraConfiguration) getConfiguration();
        try {
            if (configuration.getCreateTestTicket()) {
                // prepare fictive content
                List<FailedCheck> errors = TemplateRenderingHelper.generateFictiveErrors();
                Map<String, Object> templateVariables = templateRenderingHelper.generateTemplateParameters(configuration, errors, 42);

                getConfiguration().setString(MavenCentralDeployTaskWithJiraConfiguration.LATEST_STATUS, "Test report created.");
                jiraTestReportCapability.createReport(configuration, errors, templateVariables);
                return "Test report created.";
            } else
                return super.execute();
        } catch (Exception e) {
            getConfiguration().setString(MavenCentralDeployTaskWithJiraConfiguration.LATEST_STATUS, "Error in deployment: "+e.getMessage());
            throw e;
        }
    }

    @Override
    public String getMessage() {
        return "Deploy selected artifacts to Maven Central with Jira specific configuration";
    }

    @Override
    protected TaskConfiguration createTaskConfiguration() {
        return new MavenCentralDeployTaskWithJiraConfiguration();
    }
}
