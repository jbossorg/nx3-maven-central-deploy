package org.jboss.nexus.validation.reporting;

import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.TemplateRenderingHelper;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.nexus.repository.storage.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Named(PlainTextTestReportCapabilityDescriptor.TYPE_ID)
@Singleton
public class PlainTextTestReportCapability extends TestReportCapability<PlainTextTestReportCapabilityConfiguration>  {

	@Inject
	private TemplateRenderingHelper templateHelper;

	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration,  List<FailedCheck> listOfFailures, long processed) {
		PlainTextTestReportCapabilityConfiguration plainTextTestReportCapabilityConfiguration = Objects.requireNonNull(mavenCentralDeploy.findConfigurationForPlugin(PlainTextTestReportCapabilityConfiguration.class), "Wrong injection of MavenCentralDeploy!") ;
		Objects.requireNonNull(templateHelper);

		if (StringUtils.isNotBlank(plainTextTestReportCapabilityConfiguration.getOutputFileName())) {
			String outputFile = templateHelper.render(plainTextTestReportCapabilityConfiguration.getOutputFileName(), templateHelper.generateTemplateParameters(mavenCentralDeployTaskConfiguration));

			StringBuilder response = new StringBuilder(mavenCentralDeployTaskConfiguration.getName()).append(" deployment number ").append(mavenCentralDeployTaskConfiguration.getRunNumber()).append(" on ").append(new Date());
			response.append("\nProcessed ").append(processed).append(" components from ").append(mavenCentralDeployTaskConfiguration.getRepository()).append('.');
			if (listOfFailures.isEmpty()) {
				response.append('\n');
			} else {
				response.append("\n---------------------------------------");
				response.append("\nProblems found: ").append(listOfFailures.size());
				Map<Component, List<FailedCheck>> groupedErrors = listOfFailures.stream().collect(Collectors.groupingBy(FailedCheck::getComponent));
				boolean first = true;
				for (Component component : groupedErrors.keySet().stream().sorted(Comparator.comparing(Component::requireGroup).thenComparing(c -> c.name()).thenComparing(Component::requireVersion)).collect(Collectors.toList())) {
					response.append('\n').append(component.toStringExternal()).append("\n--------------------------------");
					for (FailedCheck fail : groupedErrors.get(component)) {
						response.append("\n- ").append(fail.getProblem());
					}
				}
			}

			boolean appendFile = plainTextTestReportCapabilityConfiguration.isAppendFile();

			File file = new File(outputFile);
			if(file.exists()) {
				if(!file.canWrite()) {
					String msg = "Can not write to a file " + outputFile + ". Fix the capability configuration/file permissions!";
					log.error(msg);
					throw new RuntimeException(msg);
				}
			} else
				appendFile = false;

			try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, appendFile))) {
				printWriter.write(response.toString());
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

		} // not blank file name
	}

	@Override
	protected PlainTextTestReportCapabilityConfiguration createConfig(Map<String, String> properties)  {
		return new PlainTextTestReportCapabilityConfiguration(properties);
	}
}


