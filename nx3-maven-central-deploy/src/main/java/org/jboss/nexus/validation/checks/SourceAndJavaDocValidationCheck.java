package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.constants.FileExtensions;
import org.jboss.nexus.content.Asset;
import org.jboss.nexus.content.Component;
import org.jetbrains.annotations.NotNull;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Named
@Singleton
public class SourceAndJavaDocValidationCheck extends CentralValidation {
	private static final Set<String> requiresSourceAndJavadocExtensions = Arrays.stream((new String[]{FileExtensions.EXTENSION_JAR, FileExtensions.EXTENSION_WAR, FileExtensions.EXTENSION_EAR})).collect(Collectors.toSet());

	@Override
	public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<FailedCheck> listOfFailures) {
		if(log.isDebugEnabled())
			log.debug("SourceAndJavaDocValidationCheck: Validating component {}", component.toStringExternal());


		if(mavenCentralDeployTaskConfiguration.getDisableHasJavaDoc() && mavenCentralDeployTaskConfiguration.getDisableHasSourceCodes())  {
			log.debug(mavenCentralDeployTaskConfiguration.getId()+": javadoc and source code validation disabled.");
			return;
		}

		Set<String> javadocFiles = new HashSet<>();
		Set<String> sourceFiles = new HashSet<>();
		Set<String> signatureFiles = new HashSet<>();
		Set<String> checkedFiles = new HashSet<>();

		for(Asset asset : component.assetsInside()) {
			log.debug(asset.toString());

			int dot = asset.name().lastIndexOf('.');
			if(dot>-1) {
				String suffix = asset.name().substring(dot);

				if(asset.name().endsWith(FileExtensions.EXTENSION_SOURCES)) {
					sourceFiles.add(asset.name());
				} else if(asset.name().endsWith(FileExtensions.EXTENSION_ASC)) {
					signatureFiles.add(asset.name());
				} else if(asset.name().endsWith(FileExtensions.EXTENSION_JAVADOC)) {
					javadocFiles.add(asset.name());
				} else if(requiresSourceAndJavadocExtensions.contains(suffix)) {
					checkedFiles.add(asset.name());
				}
			}
		}

		for(String file : checkedFiles) {
			String unclassifiedJarName = component.name() + "-" + component.version() + FileExtensions.EXTENSION_JAR;

			if(file.endsWith(unclassifiedJarName)) {
				if (!mavenCentralDeployTaskConfiguration.getDisableHasJavaDoc() && !javadocFiles.contains(makeJavaDocName(file))) {
					listOfFailures.add(new FailedCheck(component, "JavaDoc is missing for " + file));
				}

				if (!mavenCentralDeployTaskConfiguration.getDisableHasSourceCodes() && !sourceFiles.contains(makeSourceCodeName(file))) {
					listOfFailures.add(new FailedCheck(component, "Source code is missing for " + file));
				}
			}

			if (!mavenCentralDeployTaskConfiguration.getDisableHasSignatureFile() && !signatureFiles.contains(makeSignatureFileName(file))) {
				listOfFailures.add(new FailedCheck(component, "Signature file is missing for " + file));
			}

		}
	}

	/** From the file name makes the name it should have if it is a javadoc file
	 *
	 * @param fileName file name
	 *
	 * @return file name of supposed JavaDoc file
	 */
	 static String makeJavaDocName(@NotNull String fileName) {
		return fileName.substring(0, fileName.length()-4)+FileExtensions.EXTENSION_JAVADOC;
	}

	/** From the file name makes the name it should have if it is a javadoc file
	 *
	 * @param fileName file name
	 *
	 * @return file name of supposed JavaDoc file
	 */
	 static String makeSourceCodeName(@NotNull String fileName) {
		return fileName.substring(0, fileName.length()-4)+FileExtensions.EXTENSION_SOURCES;
	}

	/** From the file name makes the name it should have if it is a javadoc file
	 *
	 * @param fileName file name
	 *
	 * @return file name of supposed JavaDoc file
	 */
	 static String makeSignatureFileName(@NotNull String fileName) {
		return fileName+FileExtensions.EXTENSION_ASC;
	}
}
