package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeploy;
import org.jboss.nexus.MavenCentralDeployDescriptor;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import static org.jboss.nexus.MavenCentralDeployTaskConfiguration.*;

public class TaskConfigurationGenerator
{
	public static TaskConfiguration defaultMavenCentralDeployTaskConfiguration() {
		TaskConfiguration taskConfiguration = new TaskConfiguration();
		taskConfiguration.setString(REPOSITORY, "some_repository");
		taskConfiguration.setId("ID");
		taskConfiguration.setTypeId(MavenCentralDeployDescriptor.TYPE_ID);

		return taskConfiguration;
	}
}
