package org.jboss.nexus.validation.reporting;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named(JiraTestReportCapabilityDescriptor.TYPE_ID)
public class JiraTestReportCapability extends TestReportCapability<JiraTestReportCapabilityConfiguration>{
	@Override
	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, List<FailedCheck> listOfFailures, long processed) {
		// TODO: 24.03.2023
	}

	@Override
	protected JiraTestReportCapabilityConfiguration createConfig(Map<String, String> properties) throws Exception {
		// TODO: 24.03.2023
		return null;
	}
}
