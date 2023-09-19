package org.jboss.nexus.validation.reporting.plaintext;

import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.MavenCentralDeployTaskDescriptor;
import org.jboss.nexus.TemplateRenderingHelper;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.jboss.nexus.validation.reporting.TestReportCapability;
import org.sonatype.nexus.scheduling.TaskInfo;

import javax.inject.Named;
import java.io.*;
import java.util.*;

@Named(PlainTextTestReportCapabilityDescriptor.TYPE_ID)
public class PlainTextTestReportCapability extends TestReportCapability<PlainTextTestReportCapabilityConfiguration> {



	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration,  List<FailedCheck> listOfFailures, Map<String, Object> printVariables) {
		PlainTextTestReportCapabilityConfiguration plainTextTestReportCapabilityConfiguration = mavenCentralDeploy.findConfigurationForPlugin(PlainTextTestReportCapabilityConfiguration.class) ;

		if(plainTextTestReportCapabilityConfiguration == null || StringUtils.isBlank(plainTextTestReportCapabilityConfiguration.getReportTemplate()))
			return; // feature is disabled or not configured

		Objects.requireNonNull(templateHelper);

		if (StringUtils.isNotBlank(plainTextTestReportCapabilityConfiguration.getOutputFileName())) {
			String outputFile = templateHelper.render(plainTextTestReportCapabilityConfiguration.getOutputFileName(), printVariables);

			boolean appendFile = plainTextTestReportCapabilityConfiguration.isAppendFile();

			File file = new File(outputFile);
			if(file.exists()) {
				if(!file.canWrite()) {
					String msg = "Can not write to a file " + outputFile + ". Fix the capability configuration/file permissions!";
					log.error(msg);
					throw new RuntimeException(msg);
				}
			} else {
				appendFile = false; // append to non-existing file causes an error
			}

			try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, appendFile))) {
				templateHelper.render(new StringReader(plainTextTestReportCapabilityConfiguration.getReportTemplate()), printWriter, printVariables);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

		} // not blank file name
	}

	@Override
	protected PlainTextTestReportCapabilityConfiguration createConfig(Map<String, String> properties)  {
		return new PlainTextTestReportCapabilityConfiguration(properties);
	}

	@Override
	public String status() {
		if(getConfig() == null)
			return "Capability is not configured.";

	   StringBuilder result = new StringBuilder();
		if(StringUtils.isEmpty(getConfig().getOutputFileName()))
			result.append("Output file is not configured!\n");

		if(StringUtils.isEmpty(getConfig().getReportTemplate()))
			result.append("The output template was not set!\n");


		if(taskSchedulerProvider == null || taskSchedulerProvider.get() == null) {
			result.append("Unable to find task scheduler!\n");

		} else {
			for(TaskInfo taskInfo : taskSchedulerProvider.get().listsTasks()) {
				if(taskInfo.getTypeId().equals(MavenCentralDeployTaskDescriptor.TYPE_ID)) {
					result.append("<b>------------------------------- ").append(taskInfo.getName()).append(" -------------------------------</b>\n");
					MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration = new MavenCentralDeployTaskConfiguration (taskInfo.getConfiguration());
					result.append("<b>Output file:</b> ");
					Map<String, Object> parameters = templateHelper.generateTemplateParameters(mavenCentralDeployTaskConfiguration, TemplateRenderingHelper.generateFictiveErrors(), 15);
					try {
						result.append(templateHelper.render(getConfig().getOutputFileName(), parameters));
					} catch (Exception e) {
						result.append("Template processing error: ").append(e.getMessage());
						if(e.getCause() != null) {
							result.append('\n').append("Cause: \n").append(e.getCause().getMessage());
						}
					}
					result.append('\n');
					result.append("<b>Example output:</b> \n");

					try {
						result.append(templateHelper.render(getConfig().getReportTemplate(), parameters));
					} catch (Exception e) {
						result.append("Template processing error: ").append(e.getMessage());
						if(e.getCause() != null) {
							result.append('\n').append("Cause: \n").append(e.getCause().getMessage());
						}
					}
					result.append('\n');
				}
			}
		}

		return result.toString().replaceAll("\n", "<br>");
	}

	@Override
	protected String renderDescription()  {
		return "Plaintext reporting of Maven Central Deployment.";
	}
}


