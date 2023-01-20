package org.jboss.nexus.validation.reporting;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;

import java.util.List;

/** Interface to identify test reports
 *
 */
public interface TestReportCapability {
	/** Creates the report for the given errors based on the task configuration. The method is void as the actual reporting
	 * is usually creating a file somewhere or integrating with some external system such as Jira.
	 *
	 * @param mavenCentralDeployTaskConfiguration task configuration
	 * @param listOfFailures list of failures to report
	 * @param processed number of processed components (total)
	 */
	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, List<FailedCheck> listOfFailures, long processed);
}
