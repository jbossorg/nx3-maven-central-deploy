package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named
@Singleton
public class SourceAndJavaDocValidationCheck extends CentralValidation {
	@Override
	public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<Asset> assets, @NotNull List<FailedCheck> listOfFailures) {
		// TODO: 15.12.2022 implement
	}
}
