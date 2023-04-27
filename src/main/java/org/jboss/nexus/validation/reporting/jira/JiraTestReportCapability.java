package org.jboss.nexus.validation.reporting.jira;

import org.jboss.nexus.MavenCentralDeployCapabilityParent;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Named(JiraTestReportCapabilityDescriptor.TYPE_ID)
public class JiraTestReportCapability extends MavenCentralDeployCapabilityParent<JiraTestReportCapabilityConfiguration> {

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
	protected String renderStatus() throws Exception {
		return super.renderStatus(); // TODO: 31.03.2023  Display all stored information from ServerInformation
	}

	@Nullable
	@Override
	protected String renderDescription() throws Exception {
		return super.renderDescription(); // TODO: 31.03.2023
	}
}
