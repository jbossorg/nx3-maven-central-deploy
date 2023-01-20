package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import java.util.List;

/**  pom.xml files are not allowed to have a dependency on any SNAPSHOT artifact. We do not need to check the transitive dependencies, 
 * but the direct ones are something we can easily check.
 */
public class SnapshotDependencyValidationCheck extends CentralValidation {
	@Override
	public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<Asset> assets, @NotNull List<FailedCheck> listOfFailures) {
		// TODO: 15.12.2022 Implement 
	}
}
