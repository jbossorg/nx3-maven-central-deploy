package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import static org.jboss.nexus.MavenCentralDeployTaskConfiguration.*;

public class TaskConfigurationGenerator
{
	public static TaskConfiguration defaultMavenCentralDeployTaskConfiguration() {
		TaskConfiguration taskConfiguration = new TaskConfiguration();
		taskConfiguration.setString(REPOSITORY, "some_repository");
		taskConfiguration.setId("ID");
		taskConfiguration.setTypeId(MavenCentralDeployTaskDescriptor.TYPE_ID);

		return taskConfiguration;
	}
}
