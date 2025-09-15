package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Asset;
import org.jboss.nexus.content.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.nexus.testutils.Utils.mockedAsset;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@SuppressWarnings("TextBlockMigration")
@RunWith(MockitoJUnitRunner.class)
public class PomXMLValidationCheckTest {
	private PomXMLValidationCheck tested;

	@Mock
	private Component component;
	private List<FailedCheck> failedCheckList;

	private MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration;

	private Asset testAsset;

	@Before
	public void setup() {
		testAsset = mockedAsset("some/SomeProject.pom");

		tested = new PomXMLValidationCheck();

		failedCheckList = new ArrayList<>();

		List<Asset> listOfAssets = new ArrayList<>();
		listOfAssets.add(testAsset);

		when(component.assetsInside()).thenReturn(listOfAssets);
		mavenCentralDeployTaskConfiguration = new MavenCentralDeployTaskConfiguration(TaskConfigurationGenerator.defaultMavenCentralDeployTaskConfiguration());
	}

	/**
	 * Prepares the blob to return the right input stream
	 *
	 * @param xml the content of the XML file
	 */
	private void prepareInputStream(@NotNull String xml) {
		try {
			when(testAsset.openContentInputStream()).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean errorExist(String error) {
		return failedCheckList.stream().anyMatch(err -> err.getProblem().equals(error));
	}

	@Test
	public void validateComponentValid() {
		prepareInputStream(
			 "<project>" +
				  "   <name>nam</name>" +
				  "   <description>des</description>" +
				  "   <url>http://localhost</url>" +
				  "   <developers>  " +
				  "      <developer>  " +
				  "         <organization>Some Organization</organization>" +
				  "      </developer>" +
				  "   </developers>" +

				  "   <groupId>group</groupId>" +
				  "   <artifactId>artifact</artifactId>" +
				  "   <version>version</version>" +
				  "   <scm>" +
				  "   </scm>" +
				  "   <licenses>" +
				  "       <license>" +
				  "       </license>" +
				  "       <license/>" +
				  "   </licenses>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(failedCheckList.isEmpty());
	}

	@Test
	public void validateComponentLicenseInWrongPlace() {
		prepareInputStream(
			 "<project>" +
				  "   <scm>" +
				  "   </scm>" +
				  "   <licenses>" +
				  "       <license>" +
				  "       </license>" +
				  "   </licenses>" +
				  "   <license/>" + // <-- it should not be here
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue( errorExist("pom.xml validation failed: some/SomeProject.pom at [1,100]: license appeared outside its expected location in xml."));
		assertFalse( errorExist("some/SomeProject.pom does not have any license specified!"));
	}

	@Test
	public void validateComponentMissingLicense() {
		prepareInputStream(
			 "<project>" +
				  "   <scm>" +
				  "   </scm>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue( errorExist("some/SomeProject.pom does not have any license specified!"));
	}

	@Test
	public void validateComponentInvalidXML() {
		prepareInputStream(
			 "<project>" +
				  "   <scm>" +
				  "   </scm>" +
				  "   <licenses>" +
				  "       <license>" +
				  "       </license>" +
				  "       <license/>" +
				  "   </licenses>"); // missing end tag

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(errorExist("some/SomeProject.pom parsing error: ParseError at [row,col]:[1,104]" +
				  "Message: XML document structures must start and end within the same entity."));
	}


	@Test
	public void validateComponentSeveralEntities() {
		final String[] Level2entities = {
			 "name",
			 "description",
			 "url",
			 "artifactId"
		};

		final String xmlTemplate =
			 "<project>" +
				  "   <xxx></xxx>" +
			 "</project>";

		final String[] errors = {
			 "some/SomeProject.pom does not have the project name specified!",
			 "some/SomeProject.pom does not have the project description specified!",
			 "some/SomeProject.pom does not have the project URL specified!",
			 "some/SomeProject.pom does not have the artifact specified!"
		};

		int i = 0;
		for(String entity : Level2entities) {
			failedCheckList.clear();
			prepareInputStream("<project/>");
			tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
			assertTrue(errorExist(errors[i]));


			failedCheckList.clear();
			prepareInputStream(
				 xmlTemplate.replace("xxx", entity));

			tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
			assertFalse(errorExist(errors[i++]));
		}
	}

	@Test
	public void validateComponentGroup() {
		prepareInputStream(
	   "<project>" +
			 "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		String groupError = "some/SomeProject.pom does not have the project group specified!";
		assertTrue(errorExist(groupError));

		prepareInputStream(
	   "<project>" +
				  "<groupId></groupId>" +
			 "</project>");

		failedCheckList.clear();
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertFalse("Direct group", errorExist(groupError));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				   "<parent>" +
				  "      <groupId>ff</groupId>" +
				   "</parent>" +
				  "</project>");

		failedCheckList.clear();
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertFalse("Group in parent", errorExist(groupError));
	}

	@Test
	public void validateComponentVersion() {
		prepareInputStream(
	   "<project>" +
			 "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		String versionError = "some/SomeProject.pom does not have the project version specified!";
		assertTrue(errorExist(versionError));

		prepareInputStream(
	   "<project>" +
				  "<version>version</version>" +
			 "</project>");

		failedCheckList.clear();
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertFalse("Direct group", errorExist(versionError));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				   "<parent>" +
				  "      <version>version</version>" +
				   "</parent>" +
				  "</project>");

		failedCheckList.clear();
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertFalse("Group in parent", errorExist(versionError));
	}


	@Test
	public void validateComponentGAVMissingWhileBeingInDependencies() {
		prepareInputStream(
			 "<project>" +
				  "   <parent>" +
				  "   </parent>" +
				  "   <dependencies>" +
				  "       <dependency>" +
				  "            <groupId>group</groupId>" +
				  "            <artifactId>artifact</artifactId>" +
				  "            <version>version</version>" +
				  "       </dependency>" +
				  "   </dependencies>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertTrue( errorExist("some/SomeProject.pom does not have the project group specified!"));
		assertTrue( errorExist("some/SomeProject.pom does not have the artifact specified!"));
		assertTrue( errorExist("some/SomeProject.pom does not have the project version specified!"));
	}
	@Test
	public void validateComponentSnapshotDependency() {
		prepareInputStream(
			 "<project>" +
				  "<parent>" +
				  "      <version>something-SNAPSHOT</version>" +
				  "</parent>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertTrue("Parent SNAPSHOT", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				  "<parent>" +
				  "      <version>something</version>" +
				  "</parent>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertFalse("Parent SNAPSHOT OK", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				  "   <version>something-SNAPSHOT</version>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertTrue("Direct SNAPSHOT", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				  "   <version>something</version>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertFalse("Not snapshot", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));
	}


	@Test
	public void validateComponentSnapshotVersionNotString() {
		prepareInputStream(
			 "<project>" +
				  "<parent>" +
				  "      <version><entity/></version>" +
				  "</parent>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertTrue("Parent SNAPSHOT", errorExist("some/SomeProject.pom at [1:42]: the version element should contain a string!"));
	}

	@Test
	public void validateComponentSnapshotInDependencies() {
		prepareInputStream(
			 "<project>" +
				  "   <parent>" +
				  "   </parent>" +
				  "   <dependencies>" +
				  "       <dependency>" +
				  "            <groupId>group</groupId>" +
				  "            <artifactId>artifact</artifactId>" +
				  "            <version>version-SNAPSHOT</version>" +
				  "       </dependency>" +
				  "   </dependencies>" +
				  "</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);
		assertTrue( errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));
	}

	@Test
	public void validateComponentProjectMissing() {
		prepareInputStream("<notProject />");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(errorExist("some/SomeProject.pom does not have required project root!"));
		assertFalse(errorExist("some/SomeProject.pom does not have source code repository specified (scm)!"));
		assertFalse(errorExist("some/SomeProject.pom does not have any license specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project name specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have any developer information specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project description specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project URL specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project group specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the artifact specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project version specified!"));
	}

	@Test
	public void validateComponentAllButProjectMissing() {
		prepareInputStream("<project />");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertFalse(errorExist("some/SomeProject.pom does not have required project root!"));
		assertTrue(errorExist("some/SomeProject.pom does not have source code repository specified (scm)!"));
		assertTrue(errorExist("some/SomeProject.pom does not have any license specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the project name specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have any developer information specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the project description specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the project URL specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the project group specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the artifact specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the project version specified!"));
	}
	@Test
	public void validateComponentAllButProjectMissingWithParentClass() {
		prepareInputStream("<project>" +
				"<parent>" +
				"<groupId>org.jboss.nexus.test</groupId>" +
				"<artifactId>test-parent</artifactId>" +
				"<version>test-parent</version>" +
				"</parent>" + // let us still fail to deliver version
				"</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertFalse(errorExist("some/SomeProject.pom does not have required project root!"));
		assertFalse(errorExist("some/SomeProject.pom does not have source code repository specified (scm)!"));
		assertFalse(errorExist("some/SomeProject.pom does not have any license specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project name specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have any developer information specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project description specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project URL specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project group specified!"));
		assertTrue(errorExist("some/SomeProject.pom does not have the artifact specified!"));
		assertFalse(errorExist("some/SomeProject.pom does not have the project version specified!"));
	}
	@Test
	public void validateComponentDisableTest() {

		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SCM, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_LICENSE, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT_NAME, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_DEVELOPER_INFO, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT_DESCRIPTION, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT_URL, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_GROUP, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_ARTIFACT, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_VERSION, true);
		prepareInputStream("<project />");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(failedCheckList.isEmpty());

	}


	@Test
	public void validateJ2CLPomXML() {
		prepareInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
				"  <parent>\n" +
				"    <artifactId>mapper-parent</artifactId>\n" +
				"    <groupId>org.kie.j2cl.tools.xml.mapper</groupId>\n" +
				"    <version>0.7</version>\n" +
				"  </parent>\n" +
				"  <modelVersion>4.0.0</modelVersion>\n" +
				"  <artifactId>processor</artifactId>\n" +
				"  <name>GWT/J2CL compatible JAXB-like XML marshallers Generator</name>\n" +
				"  <description>GWT/J2CL compatible JAXB-like XML marshallers Generator</description>\n" +
				"  <url>https://github.com/kiegroup/j2cl-tools</url>\n" +
				"  <developers>\n" +
				"    <developer>\n" +
				"      <name>All developers are listed in the KIE GitHub organization</name>\n" +
				"      <url>https://github.com/orgs/kiegroup/people</url>\n" +
				"    </developer>\n" +
				"  </developers>\n" +
				"  <licenses>\n" +
				"    <license>\n" +
				"      <name>The Apache Software License, Version 2.0</name>\n" +
				"      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>\n" +
				"    </license>\n" +
				"  </licenses>\n" +
				"  <scm>\n" +
				"    <connection>scm:git:git://github.com/kiegroup/j2cl-tools.git</connection>\n" +
				"    <developerConnection>scm:git:ssh://github.com/kiegroup/j2cl-tools.git</developerConnection>\n" +
				"    <url>https://github.com/kiegroup/j2cl-tools/tree/master</url>\n" +
				"  </scm>\n" +
				"  <organization>\n" +
				"    <name>JBoss by Red Hat</name>\n" +
				"    <url>http://www.jboss.org/</url>\n" +
				"  </organization>\n" +
				"  <build>\n" +
				"    <resources>\n" +
				"      <resource>\n" +
				"        <directory>src/main/resources</directory>\n" +
				"      </resource>\n" +
				"    </resources>\n" +
				"    <plugins>\n" +
				"      <plugin>\n" +
				"        <artifactId>maven-source-plugin</artifactId>\n" +
				"        <executions>\n" +
				"          <execution>\n" +
				"            <id>attach-sources</id>\n" +
				"            <phase>package</phase>\n" +
				"            <goals>\n" +
				"              <goal>jar</goal>\n" +
				"            </goals>\n" +
				"          </execution>\n" +
				"        </executions>\n" +
				"      </plugin>\n" +
				"      <plugin>\n" +
				"        <artifactId>maven-shade-plugin</artifactId>\n" +
				"        <executions>\n" +
				"          <execution>\n" +
				"            <phase>package</phase>\n" +
				"            <goals>\n" +
				"              <goal>shade</goal>\n" +
				"            </goals>\n" +
				"            <configuration>\n" +
				"              <minimizeJar>true</minimizeJar>\n" +
				"              <filters>\n" +
				"                <filter>\n" +
				"                  <artifact>*:*:*:*</artifact>\n" +
				"                  <excludes>\n" +
				"                    <exclude>**/*.java</exclude>\n" +
				"                  </excludes>\n" +
				"                </filter>\n" +
				"              </filters>\n" +
				"            </configuration>\n" +
				"          </execution>\n" +
				"        </executions>\n" +
				"      </plugin>\n" +
				"    </plugins>\n" +
				"  </build>\n" +
				"  <profiles>\n" +
				"    <profile>\n" +
				"      <id>release</id>\n" +
				"      <build>\n" +
				"        <plugins>\n" +
				"          <plugin>\n" +
				"            <artifactId>maven-source-plugin</artifactId>\n" +
				"            <executions>\n" +
				"              <execution>\n" +
				"                <id>attach-sources</id>\n" +
				"                <goals>\n" +
				"                  <goal>jar-no-fork</goal>\n" +
				"                </goals>\n" +
				"              </execution>\n" +
				"            </executions>\n" +
				"          </plugin>\n" +
				"          <plugin>\n" +
				"            <artifactId>maven-javadoc-plugin</artifactId>\n" +
				"            <executions>\n" +
				"              <execution>\n" +
				"                <id>attach-javadocs</id>\n" +
				"                <goals>\n" +
				"                  <goal>jar</goal>\n" +
				"                </goals>\n" +
				"              </execution>\n" +
				"            </executions>\n" +
				"          </plugin>\n" +
				"          <plugin>\n" +
				"            <artifactId>maven-gpg-plugin</artifactId>\n" +
				"            <executions>\n" +
				"              <execution>\n" +
				"                <id>sign-artifacts</id>\n" +
				"                <phase>verify</phase>\n" +
				"                <goals>\n" +
				"                  <goal>sign</goal>\n" +
				"                </goals>\n" +
				"              </execution>\n" +
				"            </executions>\n" +
				"          </plugin>\n" +
				"          <plugin>\n" +
				"            <artifactId>maven-deploy-plugin</artifactId>\n" +
				"            <configuration>\n" +
				"              <skip>false</skip>\n" +
				"            </configuration>\n" +
				"          </plugin>\n" +
				"        </plugins>\n" +
				"      </build>\n" +
				"    </profile>\n" +
				"  </profiles>\n" +
				"</project>\n");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(failedCheckList.isEmpty());
	}

	@Test
	public void validateWildflyPom() {
		prepareInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!--\n" +
				"  ~ Copyright The WildFly Authors\n" +
				"  ~ SPDX-License-Identifier: Apache-2.0\n" +
				"  -->\n" +
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
				"    <modelVersion>4.0.0</modelVersion>\n" +
				"\n" +
				"    <parent>\n" +
				"        <groupId>org.wildfly.core</groupId>\n" +
				"        <artifactId>wildfly-core-parent</artifactId>\n" +
				"        <!--\n" +
				"        Maintain separation between the artifact id and the version to help prevent\n" +
				"        merge conflicts between commits changing the GA and those changing the V.\n" +
				"        -->\n" +
				"        <version>29.0.0.Final</version>\n" +
				"    </parent>\n" +
				"\n" +
				"    <groupId>org.wildfly.core</groupId>\n" +
				"    <artifactId>wildfly-core-component-matrix-builder</artifactId>\n" +
				"\n" +
				"    <packaging>pom</packaging>\n" +
				"\n" +
				"    <name>WildFly Core: Component matrix builder</name>\n" +
				"    <description>WildFly Core: Dependency Component matrix BOM Builder</description>\n" +
				"\n" +
				"    <build>\n" +
				"        <plugins>\n" +
				"            <plugin>\n" +
				"                <groupId>org.wildfly.plugins</groupId>\n" +
				"                <artifactId>wildfly-component-matrix-plugin</artifactId>\n" +
				"                <executions>\n" +
				"                    <execution>\n" +
				"                        <id>build-bom</id>\n" +
				"                        <goals>\n" +
				"                            <goal>build-bom</goal>\n" +
				"                        </goals>\n" +
				"                        <configuration>\n" +
				"                            <parent>\n" +
				"                                <groupId>org.jboss</groupId>\n" +
				"                                <artifactId>jboss-parent</artifactId>\n" +
				"                                <relativePath/>\n" +
				"                            </parent>\n" +
				"                            <bomGroupId>${project.groupId}</bomGroupId>\n" +
				"                            <bomArtifactId>wildfly-core-component-matrix</bomArtifactId>\n" +
				"                            <bomVersion>${project.version}</bomVersion>\n" +
				"                            <bomName>WildFly Core: Component Matrix</bomName>\n" +
				"                            <bomDescription>WildFly Core: Component Matrix</bomDescription>\n" +
				"                            <inheritExclusions>true</inheritExclusions>\n" +
				"                            <licenses>true</licenses>\n" +
				"                        </configuration>\n" +
				"                    </execution>\n" +
				"                </executions>\n" +
				"            </plugin>\n" +
				"        </plugins>\n" +
				"    </build>\n" +
				"\n" +
				"    <profiles>\n" +
				"        <!--\n" +
				"             This module attaches an artifact (bom-pom.xml) under a different\n" +
				"             Maven GA from the module's GA.\n" +
				"             1) Disable the jboss-parent pom's jboss-release profule use of maven-gpg-plugin\n" +
				"             for this one as it can't handle that; we need to use wildfly-maven-gpg-plugin\n" +
				"             to deal with this.\n" +
				"             2) Work around the nxrm3-maven-plugin's staging-deploy mojo's inability to directly\n" +
				"             POST these artifacts to the remote repository.\n" +
				"        -->\n" +
				"        <profile>\n" +
				"            <id>jboss-release</id>\n" +
				"            <build>\n" +
				"                <plugins>\n" +
				"                    <plugin>\n" +
				"                        <groupId>org.apache.maven.plugins</groupId>\n" +
				"                        <artifactId>maven-gpg-plugin</artifactId>\n" +
				"                        <executions>\n" +
				"                            <execution>\n" +
				"                                <id>gpg-sign</id>\n" +
				"                                <phase>none</phase>\n" +
				"                            </execution>\n" +
				"                        </executions>\n" +
				"                    </plugin>\n" +
				"                    <plugin>\n" +
				"                        <groupId>org.wildfly</groupId>\n" +
				"                        <artifactId>wildfly-maven-gpg-plugin</artifactId>\n" +
				"                        <executions>\n" +
				"                            <execution>\n" +
				"                                <id>gpg-sign</id>\n" +
				"                                <goals>\n" +
				"                                    <goal>sign</goal>\n" +
				"                                </goals>\n" +
				"                            </execution>\n" +
				"                        </executions>\n" +
				"                    </plugin>\n" +
				"                    <plugin>\n" +
				"                        <groupId>org.sonatype.plugins</groupId>\n" +
				"                        <artifactId>nxrm3-maven-plugin</artifactId>\n" +
				"                        <extensions>true</extensions>\n" +
				"                        <executions>\n" +
				"                            <!--\n" +
				"                                 The staging-deploy mojo can't deal with directly deploying this project's artifacts\n" +
				"                                 to a remote repo, because different artifacts have different maven GAs.\n" +
				"                                 So, we configure it to stage locally, and then in a subsequent execution we upload.\n" +
				"                                 The local staging writes a proper repo file layout, which the upload mojo\n" +
				"                                 uses to determine the GAV info for what it uploads.\n" +
				"                            -->\n" +
				"                            <execution>\n" +
				"                                <id>nexus-deploy</id>\n" +
				"                                <configuration>\n" +
				"                                    <stageLocally>true</stageLocally>\n" +
				"                                    <altStagingDirectory>${project.build.directory}/nexus-staging</altStagingDirectory>\n" +
				"                                </configuration>\n" +
				"                            </execution>\n" +
				"                            <execution>\n" +
				"                                <id>nexus-upload</id>\n" +
				"                                <phase>deploy</phase>\n" +
				"                                <goals>\n" +
				"                                    <goal>upload</goal>\n" +
				"                                </goals>\n" +
				"                                <configuration>\n" +
				"                                    <altStagingDirectory>${project.build.directory}/nexus-staging</altStagingDirectory>\n" +
				"                                    <repository>${nexus.repository.staging}</repository>\n" +
				"                                    <tag>${nexus.staging.tag}</tag>\n" +
				"                                </configuration>\n" +
				"                            </execution>\n" +
				"                        </executions>\n" +
				"                    </plugin>\n" +
				"                </plugins>\n" +
				"            </build>\n" +
				"        </profile>\n" +
				"    </profiles>\n" +
				"\n" +
				"</project>\n");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(failedCheckList.isEmpty());
	}


	@Test
	public void validateJgroupsPom() {
		prepareInputStream("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
				"    <modelVersion>4.0.0</modelVersion>\n" +
				"    <groupId>org.jgroups</groupId>\n" +
				"    <artifactId>jgroups</artifactId>\n" +
				"    <packaging>jar</packaging>\n" +
				"    <name>JGroups</name>\n" +
				"    <version>5.4.9.Final</version>\n" +
				"    <description>Reliable cluster communication toolkit</description>\n" +
				"    <url>http://www.jgroups.org</url>\n" +
				"\n" +
				"    <properties>\n" +
				"        <codename>Alpe d'Huez</codename>\n" +
				"        <maven.compiler.source>11</maven.compiler.source>\n" +
				"        <maven.compiler.target>11</maven.compiler.target>\n" +
				"        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
				"        <!-- nexus-staging-maven-plugin -->\n" +
				"        <autoReleaseAfterClose>true</autoReleaseAfterClose>\n" +
				"        <nexus.server.id>jboss-releases-repository</nexus.server.id>\n" +
				"        <nexus.server.url>https://repository.jboss.org/nexus/repository/releases/</nexus.server.url>\n" +
				"        <nexus.snapshot.server.id>jboss-snapshots-repository</nexus.snapshot.server.id>\n" +
				"        <nexus.snapshot.server.url>https://repository.jboss.org/nexus/repository/snapshots/</nexus.snapshot.server.url>\n" +
				"        <insecure.repositories>ERROR</insecure.repositories>\n" +
				"        <log4j2.version>2.25.0</log4j2.version>\n" +
				"    </properties>\n" +
				"\n" +
				"    <organization>\n" +
				"        <name>JBoss, a division of Red Hat</name>\n" +
				"        <url>http://www.jboss.org</url>\n" +
				"    </organization>\n" +
				"\n" +
				"    <developers>\n" +
				"        <developer>\n" +
				"            <name>Bela Ban</name>\n" +
				"            <email>belaban@gmail.com</email>\n" +
				"        </developer>\n" +
				"    </developers>\n" +
				"\n" +
				"    <licenses>\n" +
				"        <license>\n" +
				"            <name>Apache License 2.0</name>\n" +
				"            <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>\n" +
				"        </license>\n" +
				"    </licenses>\n" +
				"\n" +
				"    <scm>\n" +
				"        <connection>scm:git:git://github.com/belaban/JGroups.git</connection>\n" +
				"        <developerConnection>scm:git:ssh://git@github.com/belaban/JGroups.git</developerConnection>\n" +
				"        <url>https://github.com/belaban/JGroups</url>\n" +
				"        <tag>HEAD</tag>\n" +
				"    </scm>\n" +
				"\n" +
				"    <issueManagement>\n" +
				"        <system>Jira</system>\n" +
				"        <url>https://issues.redhat.com/browse/JGRP</url>\n" +
				"    </issueManagement>\n" +
				"\n" +
				"    <distributionManagement>\n" +
				"        <repository>\n" +
				"            <id>${nexus.server.id}</id>\n" +
				"            <name>JBoss Releases Repository</name>\n" +
				"            <url>${nexus.server.url}</url>\n" +
				"        </repository>\n" +
				"        <snapshotRepository>\n" +
				"            <id>${nexus.snapshot.server.id}</id>\n" +
				"            <url>${nexus.snapshot.server.url}</url>\n" +
				"        </snapshotRepository>\n" +
				"    </distributionManagement>\n" +
				"\n" +
				"    <repositories>\n" +
				"        <repository>\n" +
				"            <id>jboss-public-repository-group</id>\n" +
				"            <name>JBoss Public Maven Repository Group</name>\n" +
				"            <url>https://repository.jboss.org/nexus/content/groups/public/</url>\n" +
				"            <layout>default</layout>\n" +
				"            <releases>\n" +
				"                <enabled>true</enabled>\n" +
				"                <updatePolicy>never</updatePolicy>\n" +
				"            </releases>\n" +
				"            <snapshots>\n" +
				"                <enabled>false</enabled>\n" +
				"            </snapshots>\n" +
				"        </repository>\n" +
				"\n" +
				"        <repository>\n" +
				"            <id>jboss-snapshots-repository</id>\n" +
				"            <name>JBoss Nexus snapshots repository</name>\n" +
				"            <url>https://repository.jboss.org/nexus/repository/snapshots/</url>\n" +
				"            <layout>default</layout>\n" +
				"            <releases>\n" +
				"                <enabled>false</enabled>\n" +
				"                <updatePolicy>never</updatePolicy>\n" +
				"            </releases>\n" +
				"            <snapshots>\n" +
				"                <enabled>true</enabled>\n" +
				"                <updatePolicy>always</updatePolicy>\n" +
				"            </snapshots>\n" +
				"        </repository>\n" +
				"    </repositories>\n" +
				"\n" +
				"    <dependencies>\n" +
				"        <dependency>\n" +
				"            <groupId>org.apache.logging.log4j</groupId>\n" +
				"            <artifactId>log4j-core</artifactId>\n" +
				"            <version>${log4j2.version}</version>\n" +
				"            <optional>true</optional>\n" +
				"        </dependency>\n" +
				"        <dependency>\n" +
				"            <groupId>org.slf4j</groupId>\n" +
				"            <artifactId>slf4j-nop</artifactId>\n" +
				"            <version>2.0.7</version>\n" +
				"            <optional>true</optional>\n" +
				"        </dependency>\n" +
				"\n" +
				"        <!-- Test dependencies -->\n" +
				"        <dependency>\n" +
				"            <groupId>org.testng</groupId>\n" +
				"            <artifactId>testng</artifactId>\n" +
				"            <version>7.8.0</version>\n" +
				"            <scope>test</scope>\n" +
				"        </dependency>\n" +
				"        <dependency>\n" +
				"            <groupId>org.jboss.byteman</groupId>\n" +
				"            <artifactId>byteman-bmunit</artifactId>\n" +
				"            <version>4.0.25</version>\n" +
				"            <scope>test</scope>\n" +
				"            <optional>true</optional>\n" +
				"        </dependency>\n" +
				"        <dependency>\n" +
				"            <groupId>org.jboss.byteman</groupId>\n" +
				"            <artifactId>byteman</artifactId>\n" +
				"            <version>4.0.25</version>\n" +
				"            <optional>true</optional>\n" +
				"        </dependency>\n" +
				"    </dependencies>\n" +
				"\n" +
				"    <build>\n" +
				"        <sourceDirectory>src</sourceDirectory>\n" +
				"        <resources>\n" +
				"            <resource>\n" +
				"                <directory>conf</directory>\n" +
				"                <includes>\n" +
				"                    <include>*.xml</include>\n" +
				"                    <include>jg-messages*.properties</include>\n" +
				"                    <include>JGROUPS_VERSION.properties</include>\n" +
				"                </includes>\n" +
				"                <filtering>true</filtering>\n" +
				"                <excludes>\n" +
				"                    <exclude>*-service.xml</exclude>\n" +
				"                    <exclude>log4*.xml</exclude>\n" +
				"                </excludes>\n" +
				"            </resource>\n" +
				"            <resource>\n" +
				"                <directory>conf/scripts/ProtPerf</directory>\n" +
				"                <includes>\n" +
				"                    <include>*.btm</include>\n" +
				"                </includes>\n" +
				"            </resource>\n" +
				"            <resource>\n" +
				"                <directory>${project.build.directory}/schema</directory>\n" +
				"                <includes>\n" +
				"                    <include>*.xsd</include>\n" +
				"                </includes>\n" +
				"            </resource>\n" +
				"            <resource>\n" +
				"               <directory>${project.basedir}</directory>\n" +
				"               <includes>\n" +
				"                  <include>LICENSE</include>\n" +
				"                  <include>README</include>\n" +
				"               </includes>\n" +
				"            </resource>\n" +
				"        </resources>\n" +
				"        <testResources>\n" +
				"            <testResource>\n" +
				"                <directory>conf</directory>\n" +
				"                <includes>\n" +
				"                    <include>*.jks</include>\n" +
				"                </includes>\n" +
				"            </testResource>\n" +
				"        </testResources>\n" +
				"        <plugins>\n" +
				"\n" +
				"            <plugin>\n" +
				"                <groupId>org.sonatype.plugins</groupId>\n" +
				"                <artifactId>nxrm3-maven-plugin</artifactId>\n" +
				"                <version>1.0.7</version>\n" +
				"                <extensions>true</extensions>\n" +
				"                <configuration>\n" +
				"                    <nexusUrl>https://repository.jboss.org/nexus</nexusUrl>\n" +
				"\n" +
				"                    <!-- The server \"id\" element from settings to use authentication from settings.xml-->\n" +
				"                    <serverId>jboss-releases-repository</serverId>\n" +
				"\n" +
				"                    <!-- Change to repository you will be deploying to -->\n" +
				"                    <repository>releases</repository>\n" +
				"\n" +
				"                    <!-- Skip the staging deploy mojo -->\n" +
				"                    <skipNexusStagingDeployMojo>true</skipNexusStagingDeployMojo>\n" +
				"                </configuration>\n" +
				"\n" +
				"                <!-- replacing the standard Maven Deployment Plugin -->\n" +
				"                <executions>\n" +
				"                    <execution>\n" +
				"                        <id>default-deploy</id>\n" +
				"                        <phase>deploy</phase>\n" +
				"                        <goals>\n" +
				"                            <goal>deploy</goal>\n" +
				"                        </goals>\n" +
				"                    </execution>\n" +
				"                </executions>\n" +
				"            </plugin>\n" +
				"\n" +
				"            <plugin>\n" +
				"                <groupId>org.apache.maven.plugins</groupId>\n" +
				"                <artifactId>maven-compiler-plugin</artifactId>\n" +
				"                <version>3.13.0</version>\n" +
				"                <configuration>\n" +
				"                    <useIncrementalCompilation>false</useIncrementalCompilation>\n" +
				"                    <excludes>\n" +
				"                        <exclude>org/jgroups/util/JUnitXMLReporter.java</exclude>\n" +
				"                    </excludes>\n" +
				"                </configuration>\n" +
				"            </plugin>\n" +
				"            <plugin>\n" +
				"                <groupId>org.apache.maven.plugins</groupId>\n" +
				"                <artifactId>maven-enforcer-plugin</artifactId>\n" +
				"                <version>3.5.0</version>\n" +
				"                <executions>\n" +
				"                    <execution>\n" +
				"                        <id>enforce-java</id>\n" +
				"                        <goals>\n" +
				"                            <goal>enforce</goal>\n" +
				"                        </goals>\n" +
				"                    </execution>\n" +
				"                </executions>\n" +
				"                <configuration>\n" +
				"                    <rules>\n" +
				"                        <bannedRepositories>\n" +
				"                            <level>${insecure.repositories}</level>\n" +
				"                            <bannedRepositories>http://*</bannedRepositories>\n" +
				"                            <bannedPluginRepositories>http://*</bannedPluginRepositories>\n" +
				"                        </bannedRepositories>\n" +
				"                        <requireJavaVersion>\n" +
				"                            <!-- require Java 11 or higher -->\n" +
				"                            <version>[11,)</version>\n" +
				"                        </requireJavaVersion>\n" +
				"                        <requireMavenVersion>\n" +
				"                            <version>3.0.4</version>\n" +
				"                        </requireMavenVersion>\n" +
				"                    </rules>\n" +
				"                </configuration>\n" +
				"            </plugin>\n" +
				"           <plugin>\n" +
				"              <groupId>org.codehaus.mojo</groupId>\n" +
				"              <artifactId>build-helper-maven-plugin</artifactId>\n" +
				"              <executions>\n" +
				"                 <execution>\n" +
				"                    <id>add-source</id>\n" +
				"                    <phase>validate</phase>\n" +
				"                    <goals>\n" +
				"                       <goal>add-source</goal>\n" +
				"                    </goals>\n" +
				"                    <configuration>\n" +
				"                       <sources>\n" +
				"                          <!-- These tests have to go in the main jar -->\n" +
				"                          <source>tests/other</source>\n" +
				"                          <source>tests/perf</source>\n" +
				"                       </sources>\n" +
				"                    </configuration>\n" +
				"                 </execution>\n" +
				"                 <execution>\n" +
				"                    <id>add-test-source</id>\n" +
				"                    <phase>validate</phase>\n" +
				"                    <goals>\n" +
				"                       <goal>add-test-source</goal>\n" +
				"                    </goals>\n" +
				"                    <configuration>\n" +
				"                       <sources>\n" +
				"                          <source>tests/byteman</source>\n" +
				"                          <source>tests/junit</source>\n" +
				"                          <source>tests/junit-functional</source>\n" +
				"                          <!-- tests/other and tests/perf are included in the normal sources -->\n" +
				"                          <source>tests/stress</source>\n" +
				"                       </sources>\n" +
				"                    </configuration>\n" +
				"                 </execution>\n" +
				"              </executions>\n" +
				"           </plugin>\n" +
				"           <plugin>\n" +
				"                <artifactId>maven-antrun-plugin</artifactId>\n" +
				"                <executions>\n" +
				"                    <execution>\n" +
				"                        <id>compile</id>\n" +
				"                        <phase>compile</phase>\n" +
				"                        <goals>\n" +
				"                            <goal>run</goal>\n" +
				"                        </goals>\n" +
				"                        <configuration>\n" +
				"                            <target>\n" +
				"                                <property name=\"compile_classpath\" refid=\"maven.compile.classpath\" />\n" +
				"                                <property name=\"plugin_classpath\" refid=\"maven.plugin.classpath\" />\n" +
				"                                <delete dir=\"${project.build.directory}/schema\" failonerror=\"false\" />\n" +
				"                                <mkdir dir=\"${project.build.directory}/schema\" />\n" +
				"                                <java classname=\"org.jgroups.util.XMLSchemaGenerator\" fork=\"true\">\n" +
				"                                    <classpath>\n" +
				"                                        <pathelement path=\"${compile_classpath}\" />\n" +
				"                                        <pathelement path=\"${plugin_classpath}\" />\n" +
				"                                    </classpath>\n" +
				"                                    <arg line=\"-Dlog4j2.disable.jmx=true -o ${project.build.directory}/schema\" />\n" +
				"                                </java>\n" +
				"                                <copy todir=\"${project.build.directory}/schema\">\n" +
				"                                    <fileset dir=\"conf\">\n" +
				"                                        <include name=\"*.xsd\" />\n" +
				"                                    </fileset>\n" +
				"                                </copy>\n" +
				"                            </target>\n" +
				"                        </configuration>\n" +
				"                    </execution>\n" +
				"                </executions>\n" +
				"            </plugin>\n" +
				"            <!-- Make sure we generate src jars too -->\n" +
				"            <plugin>\n" +
				"               <groupId>org.apache.maven.plugins</groupId>\n" +
				"               <artifactId>maven-source-plugin</artifactId>\n" +
				"               <inherited>true</inherited>\n" +
				"                <configuration>\n" +
				"                    <excludes>\n" +
				"                        <exclude>\n" +
				"                            JGROUPS_VERSION.properties\n" +
				"                        </exclude>\n" +
				"                    </excludes>\n" +
				"                    <archive>\n" +
				"                        <manifest>\n" +
				"                            <addDefaultEntries>true</addDefaultEntries>\n" +
				"                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>\n" +
				"                            <addBuildEnvironmentEntries>true</addBuildEnvironmentEntries>\n" +
				"                        </manifest>\n" +
				"                    </archive>\n" +
				"                </configuration>\n" +
				"                <executions>\n" +
				"                    <execution>\n" +
				"                        <id>attach-sources</id>\n" +
				"                        <phase>verify</phase>\n" +
				"                        <goals>\n" +
				"                            <goal>jar-no-fork</goal>\n" +
				"                        </goals>\n" +
				"                    </execution>\n" +
				"                </executions>\n" +
				"            </plugin>\n" +
				"\n" +
				"            <plugin>\n" +
				"                <groupId>org.apache.maven.plugins</groupId>\n" +
				"                <artifactId>maven-jar-plugin</artifactId>\n" +
				"                <configuration>\n" +
				"                    <archive>\n" +
				"                        <manifest>\n" +
				"                            <addDefaultEntries>true</addDefaultEntries>\n" +
				"                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>\n" +
				"                            <addBuildEnvironmentEntries>true</addBuildEnvironmentEntries>\n" +
				"                            <!--addClasspath>true</addClasspath-->\n" +
				"                            <!-- classpathPrefix>libs/</classpathPrefix-->\n" +
				"                            <mainClass>org.jgroups.Version</mainClass>\n" +
				"                        </manifest>\n" +
				"                        <manifestEntries>\n" +
				"                            <Automatic-Module-Name>org.jgroups</Automatic-Module-Name>\n" +
				"                        </manifestEntries>\n" +
				"                    </archive>\n" +
				"                </configuration>\n" +
				"            </plugin>\n" +
				"\n" +
				"            <plugin>\n" +
				"                <groupId>org.apache.maven.plugins</groupId>\n" +
				"                <artifactId>maven-release-plugin</artifactId>\n" +
				"                <configuration>\n" +
				"                    <arguments>-DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true</arguments>\n" +
				"                    <preparationGoals>clean</preparationGoals>\n" +
				"                </configuration>\n" +
				"\n" +
				"            </plugin>\n" +
				"\n" +
				"            <!-- Disable default tests: they won't run since they are lacking configuration -->\n" +
				"            <plugin>\n" +
				"                <groupId>org.apache.maven.plugins</groupId>\n" +
				"                <artifactId>maven-surefire-plugin</artifactId>\n" +
				"                <!--configuration>\n" +
				"                    <skip>true</skip>\n" +
				"                    <skipTests>true</skipTests>\n" +
				"                </configuration-->\n" +
				"                <executions>\n" +
				"                    <execution>\n" +
				"                        <phase>test</phase>\n" +
				"                    </execution>\n" +
				"                </executions>\n" +
				"            </plugin>\n" +
				"        </plugins>\n" +
				"        <pluginManagement>\n" +
				"            <plugins>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-release-plugin</artifactId>\n" +
				"                    <version>3.1.1</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.sonatype.plugins</groupId>\n" +
				"                    <artifactId>nexus-staging-maven-plugin</artifactId>\n" +
				"                    <version>1.7.0</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-antrun-plugin</artifactId>\n" +
				"                    <version>3.1.0</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-clean-plugin</artifactId>\n" +
				"                    <version>3.5.0</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-deploy-plugin</artifactId>\n" +
				"                    <version>3.1.4</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-install-plugin</artifactId>\n" +
				"                    <version>3.1.4</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-jar-plugin</artifactId>\n" +
				"                    <version>3.3.0</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-resources-plugin</artifactId>\n" +
				"                    <version>3.3.1</version>\n" +
				"                    <configuration>\n" +
				"                        <propertiesEncoding>ISO-8859-1</propertiesEncoding>\n" +
				"                    </configuration>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-source-plugin</artifactId>\n" +
				"                    <version>3.3.1</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-surefire-plugin</artifactId>\n" +
				"                    <version>2.22.2</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-failsafe-plugin</artifactId>\n" +
				"                    <version>3.5.3</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-dependency-plugin</artifactId>\n" +
				"                    <version>3.8.1</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.apache.maven.plugins</groupId>\n" +
				"                    <artifactId>maven-help-plugin</artifactId>\n" +
				"                    <version>3.5.1</version>\n" +
				"                </plugin>\n" +
				"                <plugin>\n" +
				"                    <groupId>org.codehaus.mojo</groupId>\n" +
				"                    <artifactId>build-helper-maven-plugin</artifactId>\n" +
				"                    <version>3.6.1</version>\n" +
				"                </plugin>\n" +
				"\n" +
				"                <plugin>\n" +
				"                    <groupId>org.asciidoctor</groupId>\n" +
				"                    <artifactId>asciidoctor-maven-plugin</artifactId>\n" +
				"                    <version>3.2.0</version>\n" +
				"                    <dependencies>\n" +
				"                        <dependency>\n" +
				"                            <groupId>org.asciidoctor</groupId>\n" +
				"                            <artifactId>asciidoctorj-diagram</artifactId>\n" +
				"                            <version>3.0.1</version>\n" +
				"                        </dependency>\n" +
				"                    </dependencies>\n" +
				"                </plugin>\n" +
				"                <!--This plugin's configuration is used to store Eclipse m2e settings only. It has no influence on the Maven build itself.-->\n" +
				"                <plugin>\n" +
				"                    <groupId>org.eclipse.m2e</groupId>\n" +
				"                    <artifactId>lifecycle-mapping</artifactId>\n" +
				"                    <version>1.0.0</version>\n" +
				"                    <configuration>\n" +
				"                        <lifecycleMappingMetadata>\n" +
				"                            <pluginExecutions>\n" +
				"                                <pluginExecution>\n" +
				"                                    <pluginExecutionFilter>\n" +
				"                                        <groupId>org.apache.maven.plugins</groupId>\n" +
				"                                        <artifactId>maven-antrun-plugin</artifactId>\n" +
				"                                        <versionRange>[1.3,)</versionRange>\n" +
				"                                        <goals>\n" +
				"                                            <goal>run</goal>\n" +
				"                                        </goals>\n" +
				"                                    </pluginExecutionFilter>\n" +
				"                                    <action>\n" +
				"                                        <ignore />\n" +
				"                                    </action>\n" +
				"                                </pluginExecution>\n" +
				"                            </pluginExecutions>\n" +
				"                        </lifecycleMappingMetadata>\n" +
				"                    </configuration>\n" +
				"                </plugin>\n" +
				"            </plugins>\n" +
				"        </pluginManagement>\n" +
				"    </build>\n" +
				"\n" +
				"    <profiles>\n" +
				"        <profile>\n" +
				"            <!-- Profile to generate the manual. -->\n" +
				"            <id>manual</id>\n" +
				"            <properties>\n" +
				"                <skipTests>true</skipTests>\n" +
				"            </properties>\n" +
				"            <build>\n" +
				"                <plugins>\n" +
				"                    <!-- Generate the tables with the protocol properties. -->\n" +
				"                    <plugin>\n" +
				"                        <groupId>org.codehaus.mojo</groupId>\n" +
				"                        <artifactId>exec-maven-plugin</artifactId>\n" +
				"                        <executions>\n" +
				"                            <execution>\n" +
				"                                <id>generate-protocol-properties</id>\n" +
				"                                <phase>prepare-package</phase>\n" +
				"                                <goals>\n" +
				"                                    <goal>exec</goal>\n" +
				"                                </goals>\n" +
				"                            </execution>\n" +
				"                        </executions>\n" +
				"                        <configuration>\n" +
				"                            <executable>java</executable>\n" +
				"                            <arguments>\n" +
				"                                <argument>-classpath</argument>\n" +
				"                                <classpath />\n" +
				"                                <argument>org.jgroups.util.PropertiesToAsciidoc</argument>\n" +
				"                                <argument>${basedir}/doc/manual/protocols-template.adoc</argument>\n" +
				"                                <argument>${basedir}/doc/manual/installation-template.adoc</argument>\n" +
				"                            </arguments>\n" +
				"                        </configuration>\n" +
				"                    </plugin>\n" +
				"\n" +
				"                    <!-- Generates the manual files. -->\n" +
				"                    <!-- The generated output is in target/doc/manual/manual.html -->\n" +
				"                    <plugin>\n" +
				"                        <groupId>org.asciidoctor</groupId>\n" +
				"                        <artifactId>asciidoctor-maven-plugin</artifactId>\n" +
				"\n" +
				"                        <configuration>\n" +
				"                            <sourceDirectory>${basedir}/doc/manual</sourceDirectory>\n" +
				"                            <requires>\n" +
				"                                <require>asciidoctor-diagram</require>\n" +
				"                            </requires>\n" +
				"                        </configuration>\n" +
				"\n" +
				"                        <executions>\n" +
				"                            <execution>\n" +
				"                                <id>generate-manual</id>\n" +
				"                                <phase>prepare-package</phase>\n" +
				"                                <goals>\n" +
				"                                    <goal>process-asciidoc</goal>\n" +
				"                                </goals>\n" +
				"\n" +
				"                                <configuration>\n" +
				"                                    <outputDirectory>${project.build.directory}/doc/manual</outputDirectory>\n" +
				"                                    <attributes>\n" +
				"                                        <revdate>${maven.build.timestamp}</revdate>\n" +
				"                                        <organization>${project.organization.name}</organization>\n" +
				"                                        <source-highlighter>rouge</source-highlighter>\n" +
				"                                        <rouge-style>github</rouge-style>\n" +
				"                                        <icons>font</icons>\n" +
				"                                        <toclevels>3</toclevels>\n" +
				"                                        <sectnums>true</sectnums>\n" +
				"                                        <sectnumlevels>3</sectnumlevels>\n" +
				"                                        <sectanchors>true</sectanchors>\n" +
				"                                    </attributes>\n" +
				"                                </configuration>\n" +
				"                            </execution>\n" +
				"                        </executions>\n" +
				"                    </plugin>\n" +
				"                </plugins>\n" +
				"            </build>\n" +
				"        </profile>\n" +
				"    </profiles>\n" +
				"\n" +
				"\n" +
				"</project>");

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedCheckList);

		assertTrue(failedCheckList.isEmpty());
	}


}