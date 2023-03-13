package org.jboss.nexus.validation.reporting;

import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.MavenCentralDeployTaskDescriptor;
import org.jboss.nexus.TemplateRenderingHelper;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Named(PlainTextTestReportCapabilityDescriptor.TYPE_ID)
public class PlainTextTestReportCapability extends TestReportCapability<PlainTextTestReportCapabilityConfiguration>  {

	@Inject
	private TemplateRenderingHelper templateHelper;

	@Inject
	private Provider<TaskScheduler> taskSchedulerProvider;

	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration,  List<FailedCheck> listOfFailures, long processed) {
		PlainTextTestReportCapabilityConfiguration plainTextTestReportCapabilityConfiguration = mavenCentralDeploy.findConfigurationForPlugin(PlainTextTestReportCapabilityConfiguration.class) ;

		if(plainTextTestReportCapabilityConfiguration == null || StringUtils.isBlank(plainTextTestReportCapabilityConfiguration.getReportTemplate()))
			return; // feature is disabled or not configured

		Objects.requireNonNull(templateHelper);

		if (StringUtils.isNotBlank(plainTextTestReportCapabilityConfiguration.getOutputFileName())) {
			Map<String, Object> parameters = templateHelper.generateTemplateParameters(mavenCentralDeployTaskConfiguration);
			String outputFile = templateHelper.render(plainTextTestReportCapabilityConfiguration.getOutputFileName(), parameters);


			////////////////////
			Comparator<FailedCheck> failedCheckComparator = Comparator.comparing((FailedCheck o) -> o.getComponent().requireGroup())
				 .thenComparing(o -> o.getComponent().name())
				 .thenComparing(o -> o.getComponent().requireVersion());

			// Add parameters for the template processing
			List<FailedCheck> errors = listOfFailures.stream().sorted(failedCheckComparator).collect(Collectors.toList());
			parameters.put("errors", errors);
			parameters.put("repository", mavenCentralDeployTaskConfiguration.getRepository());
			parameters.put("processed", processed);

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
				templateHelper.render(new StringReader(plainTextTestReportCapabilityConfiguration.getReportTemplate()), printWriter, parameters);
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
					Map<String, Object> parameters = templateHelper.generateTemplateParameters(mavenCentralDeployTaskConfiguration);
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

					List<FailedCheck> errors = TemplateRenderingHelper.generateFictiveErrors();
					parameters.put("errors", errors);
					parameters.put("repository", mavenCentralDeployTaskConfiguration.getRepository());
					parameters.put("processed", 15);

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
		return "Plaintext reporting of MC Deployment.";
	}
}


