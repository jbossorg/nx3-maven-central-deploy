package org.jboss.nexus.validation.reporting;

import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.nexus.repository.storage.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@Singleton
public class PlainTextTestReportCapability implements TestReportCapability {

	public void createReport(MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration,  List<FailedCheck> listOfFailures, long processed) {
		if (StringUtils.isNotBlank(mavenCentralDeployTaskConfiguration.getPlainTextReportOutputFile())) {
			StringBuilder response = new StringBuilder(new Date().toString()).append(" processed ").append(processed).append(" components from ").append(mavenCentralDeployTaskConfiguration.getRepository()).append('.');
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

			try(PrintWriter printWriter = new PrintWriter(new FileOutputStream(mavenCentralDeployTaskConfiguration.getPlainTextReportOutputFile(), true))) {
				printWriter.write(response.toString());
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} // not blank file name
	}

}


