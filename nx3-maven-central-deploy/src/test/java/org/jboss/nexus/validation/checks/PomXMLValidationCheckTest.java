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


}