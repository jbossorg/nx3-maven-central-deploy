package org.jboss.nexus.validation.reporting;

import org.jboss.nexus.MavenCentralDeployCapabilityConfigurationParent;
import org.jboss.nexus.MavenCentralDeployCapabilityParent;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.TemplateRenderingHelper;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.nexus.scheduling.TaskScheduler;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;

/** Class to extend on for the reports.
 */
public abstract class TestReportCapability<ConfT extends MavenCentralDeployCapabilityConfigurationParent> extends MavenCentralDeployCapabilityParent<ConfT> {


	@Inject
	protected TemplateRenderingHelper templateHelper;

	@Inject
	protected Provider<TaskScheduler> taskSchedulerProvider;

	/** Creates the report for the given errors based on the task configuration. The method is void as the actual reporting
	 * is usually creating a file somewhere or integrating with some external system such as Jira.
	 *
	 * @param mavenCentralDeployTaskConfiguration task configuration
	 * @param listOfFailures list of failures to report
	 * @param printVariables modifiable copy of variables for Velocity processing
	 */
	public abstract void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, List<FailedCheck> listOfFailures, Map<String, Object> printVariables);

}
