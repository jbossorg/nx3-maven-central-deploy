package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Asset;
import org.jboss.nexus.content.Component;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.jboss.nexus.constants.FileExtensions.EXTENSION_POM;

/** <p>Class for  verification of the content of pom.xml files, such as present license, scm and such.
 *  While many errors would normally be avoided in xml using
 * xml schema, it is not mandatory in Maven so we need to check the structure as well.
 * Following things must be in pom.xml to be eligible for Maven Central (even though they are optional for
 * Maven build:</p>
 *
 * &bull; Correct GAV coordinates (and version must not be SNAPSHOT)<br>
 * &bull; name, project description and url of the organization<br>
 * &bull; License information<br>
 * &bull; Developer information<br>
 * &bull; Source of the source codes (scm)
 */
@Singleton
@Named
public class PomXMLValidationCheck extends CentralValidation {

	@Inject
	public PomXMLValidationCheck() {
		super();
	}

	@Override
	public void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<FailedCheck> listOfFailures) {
		if(log.isDebugEnabled())
			log.debug("PomXMLValidationCheck: Validating component {}", component.toStringExternal());

		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		for(Asset asset : component.assetsInside() ) {
			if(asset.name().endsWith(EXTENSION_POM)) {
				try (InputStream bis = asset.openContentInputStream()) {
					int level = 0;
					XMLEventReader reader = xmlInputFactory.createXMLEventReader(bis);

					boolean hasProject = false,
						 hasLicense = false,
						 licensesSection = false,
						 developersSection = false,
						 parentSection = false,
						 hasSCM = false,
						 hasGroup = false,
						 hasArtifact = false,
						 hasVersion = false,
						 hasSnapshotVersion = false,
						 hasDeveloperInfo = false,
						 hasProjectName = false,
						 hasProjectDescription = false,
						 hasProjectURL = false,
						 hasParentSection = false;


					while (reader.hasNext()) {
						XMLEvent event = reader.nextEvent();

						if (event.isStartElement()) {
							level++;
							StartElement startElement = event.asStartElement();
							switch (startElement.getName().getLocalPart()) {
								case "project":
									hasProject = checkLevel(listOfFailures, component, asset.name(), event.getLocation(), "project", level, 1);
									break;
								case "scm":
									hasSCM = checkLevel(listOfFailures, component, asset.name(), event.getLocation(), "source code source (scm)", level, 2);
									break;
								case "license":
									// license is in the expected place in xml (level and inside licenses block)
									if(checkLevel(listOfFailures, component, asset.name(), event.getLocation(), "license", level, 3))
										hasLicense |= licensesSection; // the right section is also required
									break;
								case "licenses":
									licensesSection |= level==2;
									break;
								case "developer":
									// developer is in the expected place in xml (level and inside licenses block)
									if(checkLevel(listOfFailures, component, asset.name(), event.getLocation(), "developer", level, 3))
										hasDeveloperInfo |= developersSection; // the right section is also required
									break;
								case "developers":
									developersSection = checkLevel(listOfFailures, component, asset.name(), event.getLocation(), "developers section", level, 2);
									break;
								case "organization":
									// this can be considered developer info as well
									hasDeveloperInfo = checkLevelMultipleLevels(listOfFailures, component, asset.name(), event.getLocation(), "organization", level, 2, 4);
									break;
								case "name":
									if(level == 2) // element named name appears in many tags, but we need the one on level 2
										hasProjectName = true;
									break;
								case "description":
									hasProjectDescription = checkLevel(listOfFailures, component, asset.name(), event.getLocation(), "description", level, 2);
									break;
								case "url":
									if(level == 2) // url tag may be elsewhere, which is all right so no complaining
										hasProjectURL = true;
									break;
								case "parent":
									parentSection = level == 2; // parent section may appear elsewhere, but it is probably not a problem
									hasParentSection |= parentSection;
									break;
								case "groupId":
									if(level==2 || level == 3 && parentSection)
										hasGroup = true;
									break;
								case "artifactId":
									if(level == 2)
										hasArtifact = true;
								case "version":
									XMLEvent versionEvent = reader.peek();
									if(versionEvent.isCharacters()) {
										if(versionEvent.asCharacters().getData().endsWith("-SNAPSHOT")) {
											hasSnapshotVersion = true;
										}
										if(level==2 || level == 3 && parentSection) {
											hasVersion = true;
										}
									} else {
											String msg = asset.name() + " at [" + versionEvent.getLocation().getLineNumber()+":"+versionEvent.getLocation().getColumnNumber()   + "]: the version element should contain a string!";
											log.info("Failed PomXMLValidationCheck: "+msg);
											listOfFailures.add(new FailedCheck(component, msg));
									}

									break;
							}
						} else if (event.isEndElement()) {
							level--;
							EndElement endElement = event.asEndElement();
							switch (endElement.getName().getLocalPart()) {
								case "licenses":
									licensesSection = false;
									break;
								case "developers":
									developersSection = false;
									break;
								case "parent":
									parentSection = false;
									break;
							}

						}

					}

					if(! mavenCentralDeployTaskConfiguration.getDisableHasProject() && !hasProject) {
						String msg = asset.name() + " does not have required project root!";
						log.info("Failed PomXMLValidationCheck: "+msg);
						listOfFailures.add(new FailedCheck(component, msg));
					}


					if(hasProject) {
						// if there is no project it does not make any sense to report anything else from here

						if (!hasParentSection && !mavenCentralDeployTaskConfiguration.getDisableHasSCM() && !hasSCM) {
							String msg = asset.name() + " does not have source code repository specified (scm)!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!hasParentSection && !mavenCentralDeployTaskConfiguration.getDisableHasLicense() && !hasLicense) {
							String msg = asset.name() + " does not have any license specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!hasParentSection && !mavenCentralDeployTaskConfiguration.getDisableHasProjectName() && !hasProjectName) {
							String msg = asset.name() + " does not have the project name specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!hasParentSection && !mavenCentralDeployTaskConfiguration.getDisableHasDeveloperInfo() && !hasDeveloperInfo) {
							String msg = asset.name() + " does not have any developer information specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!hasParentSection && !mavenCentralDeployTaskConfiguration.getDisableHasProjectDescription() && !hasProjectDescription) {
							String msg = asset.name() + " does not have the project description specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!hasParentSection && !mavenCentralDeployTaskConfiguration.getDisableHasProjectURL() && !hasProjectURL) {
							String msg = asset.name() + " does not have the project URL specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!mavenCentralDeployTaskConfiguration.getDisableHasGroup() && !hasGroup) {
							String msg = asset.name() + " does not have the project group specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!mavenCentralDeployTaskConfiguration.getDisableHasArtifact() && !hasArtifact) {
							String msg = asset.name() + " does not have the artifact specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!mavenCentralDeployTaskConfiguration.getDisableHasVersion() && !hasVersion) {
							String msg = asset.name() + " does not have the project version specified!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
						if (!mavenCentralDeployTaskConfiguration.getDisableHasSnapshotVersion() && hasSnapshotVersion) {
							String msg = asset.name() + " contains a dependency on a SNAPSHOT artifact!";
							log.info("Failed PomXMLValidationCheck: " + msg);
							listOfFailures.add(new FailedCheck(component, msg));
						}
					}

				} catch (IOException e) {
					String msg = "Program error: error getting file content " + asset.name()+": "+e.getMessage();
					log.error(msg);
					listOfFailures.add(new FailedCheck(component, msg));
				} catch (XMLStreamException e) {
					String msg = asset.name() + " parsing error: " + e.getMessage().replace("\n", "");
					log.info("Failed PomXMLValidationCheck: "+msg);
					listOfFailures.add(new FailedCheck(component, msg));
				}

			}
		}
	}

	private static StringBuilder errorMessage(String assetName, Location location) {
		StringBuilder result = new StringBuilder("pom.xml validation failed: ");
		result.append(assetName).append(" at [").append(location.getLineNumber()).append(",").append(location.getColumnNumber())
			 .append("]: ");
		return result;

	}

	/** Verifies the level of the specified tag
	 *
	 * @param failedCheckList list to report problems to
	 * @param component component being analyzed
	 * @param assetName name of the asset for the possible error message
	 * @param location location of the targeted entity
	 * @param testedTag what is being tested (human-readable)
	 * @param currentLevel current level in xml
	 * @param expectedLevel expected level in xml
	 *
	 * @return true if it is OK
	 */
	boolean checkLevel(List<FailedCheck> failedCheckList, Component component, String assetName, Location location, String testedTag, int currentLevel, int expectedLevel) {
		if(currentLevel != expectedLevel) {
			StringBuilder messageBuilder = errorMessage(assetName, location);
			messageBuilder.append(testedTag).append(" appeared outside its expected location in xml.");
			failedCheckList.add(new FailedCheck(component, messageBuilder.toString()));
			return false;
		}
		return true;
	}


	/** Verifies the level of the specified tag
	 *
	 * @param failedCheckList list to report problems to
	 * @param component component being analyzed
	 * @param assetName name of the asset for the possible error message
	 * @param location location of the targeted entity
	 * @param testedTag what is being tested (human-readable)
	 * @param currentLevel current level in xml
	 * @param expectedLevels expected levels in xml
	 *
	 * @return true if it is OK
	 */
	@SuppressWarnings("SameParameterValue")
	boolean checkLevelMultipleLevels(List<FailedCheck> failedCheckList, Component component, String assetName, Location location, String testedTag, int currentLevel, int... expectedLevels) {
        for (int expectedLevel : expectedLevels) {
            if (expectedLevel == currentLevel) {
                return true; // one of the expected levels found
            }
        }

		StringBuilder messageBuilder = errorMessage(assetName, location);
		messageBuilder.append(testedTag).append(" appeared outside its expected location in xml.");
		failedCheckList.add(new FailedCheck(component, messageBuilder.toString()));
		return false;
	}

}
