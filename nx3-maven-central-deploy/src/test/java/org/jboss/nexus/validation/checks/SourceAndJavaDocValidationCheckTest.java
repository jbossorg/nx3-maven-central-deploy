package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Asset;
import org.jboss.nexus.content.Component;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.nexus.testutils.Utils.mockedAsset;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourceAndJavaDocValidationCheckTest {

	private final SourceAndJavaDocValidationCheck tested  = new SourceAndJavaDocValidationCheck();

	private final List<FailedCheck> failedChecks = new ArrayList<>();

	private MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration;

	@Before
	public void setup() {
		failedChecks.clear();

		mavenCentralDeployTaskConfiguration = new MavenCentralDeployTaskConfiguration(TaskConfigurationGenerator.defaultMavenCentralDeployTaskConfiguration());
	}



	@Test
	public void validateComponentJar() {
		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");
		

		Asset component1Asset = mockedAsset("org/something/something-version.jar"),
			 component1AssetSources = mockedAsset("org/something/something-version-sources.jar"),
			 component1AssetSingatureFile= mockedAsset("org/something/something-version.jar.asc"),
			 component1AssetJavaDoc = mockedAsset("org/something/something-version-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSources);
		assets.add(component1AssetJavaDoc);
		assets.add(component1AssetSingatureFile);

		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());

	}

	@Test
	public void validateComponentJarMissingJavaDoc() {
		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version.jar"),
			 component1AssetJavaDoc = mockedAsset("org/something/something-version-sources.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetJavaDoc);

		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for org/something/something-version.jar"));
		assertFalse(errorExist("Source code is missing for org/something/something-version.jar"));
		assertTrue(errorExist("Signature file is missing for org/something/something-version.jar"));
	}

	@Test
	public void validateComponentJarMissingSources() {
		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version.jar"),
			 component1AssetJavaDoc = mockedAsset("org/something/something-version-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetJavaDoc);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertFalse(errorExist("JavaDoc is missing for org/something/something-version.jar"));
		assertTrue(errorExist("Source code is missing for org/something/something-version.jar"));

	}


	@Test
	public void validateComponentJarMissingSourcesAndJavaDoc() {
		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for org/something/something-version.jar"));
		assertTrue(errorExist("Source code is missing for org/something/something-version.jar"));
		assertTrue(errorExist("Signature file is missing for org/something/something-version.jar"));

		assertSame(component1, failedChecks.get(0).getComponent());
		assertSame(component1, failedChecks.get(1).getComponent());
	}

	@Test
	public void validateComponentJarMissingSourcesAndJavaDocJavaDocDisabled() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_JAVADOC, true);

		Component component = mock(Component.class);
		when(component.name()).thenReturn("something");
		when(component.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedChecks);

		assertFalse(errorExist("JavaDoc is missing for org/something/something-version.jar"));
		assertTrue(errorExist("Source code is missing for org/something/something-version.jar"));
		assertTrue(errorExist("Signature file is missing for org/something/something-version.jar"));

	}


	@Test
	public void validateComponentJarMissingSourcesAndJavaDocSourcesDisabled() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SOURCE_CODES, true);

		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for org/something/something-version.jar"));
		assertFalse(errorExist("Source code is missing for org/something/something-version.jar"));
	}
	@Test
	public void validateComponentJarMissingSignatureFile() {
		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version.jar"),
		    component1SignatureFile = mockedAsset("org/something/something-version.jar.asc");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);
		assertTrue(errorExist("Signature file is missing for org/something/something-version.jar"));

		failedChecks.clear();
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, true);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);
		assertFalse(errorExist("Signature file is missing for org/something/something-version.jar"));

		failedChecks.clear();
		assets.add(component1SignatureFile);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, false);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);
		assertFalse(errorExist("Signature file is missing for org/something/something-version.jar"));
	}

	@Test
	public void validateComponentJarMissingSignatureFileWithClassifier() {
		Component component1 = mock(Component.class);
		when(component1.name()).thenReturn("something");
		when(component1.version()).thenReturn("version");

		Asset component1Asset = mockedAsset("org/something/something-version-test.jar"),
		    component1SignatureFile = mockedAsset("org/something/something-version-test.jar.asc");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);
		assertTrue(errorExist("Signature file is missing for org/something/something-version-test.jar"));

		failedChecks.clear();
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, true);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);
		assertFalse(errorExist("Signature file is missing for org/something/something-version-test.jar"));

		failedChecks.clear();
		assets.add(component1SignatureFile);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, false);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());

	}

	@Test
	public void validateComponentWar() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("org/something/something-version.war");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertEquals(1, failedChecks.size());
		assertTrue(errorExist("Signature file is missing for org/something/something-version.war"));
	}

	@Test
	public void validateComponentWarOK() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("org/something/something-version.war"),
			component1AssetSignatureFile = mockedAsset("org/something/something-version.war.asc");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSignatureFile);
		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentEar() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("org/something/something-version.ear");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertEquals(1, failedChecks.size());
		assertTrue(errorExist("Signature file is missing for org/something/something-version.ear"));
	}

	@Test
	public void validateComponentEarOK() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("org/something/something-version.ear"),
			 component1AssetSignatueFile = mockedAsset("org/something/something-version.ear.asc");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSignatueFile);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentNoSuffix() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("org/something/something-version");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentNoJava() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("org/something/something-version.txt");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentDisabledSourceAndJavaDoc() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_JAVADOC, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SOURCE_CODES, true);

		Component component = mock(Component.class);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void makeJavaDocName(){
		assertEquals("/dir/file-javadoc.jar", SourceAndJavaDocValidationCheck.makeJavaDocName("/dir/file.jar"));
		assertEquals("/dir/file-javadoc.jar", SourceAndJavaDocValidationCheck.makeJavaDocName("/dir/file.war"));
		assertEquals("/dir/file-javadoc.jar", SourceAndJavaDocValidationCheck.makeJavaDocName("/dir/file.ear"));
	}

	@Test
	public void makeSourceCodeName() {
		assertEquals("/dir/file-sources.jar", SourceAndJavaDocValidationCheck.makeSourceCodeName("/dir/file.jar"));
		assertEquals("/dir/file-sources.jar", SourceAndJavaDocValidationCheck.makeSourceCodeName("/dir/file.war"));
		assertEquals("/dir/file-sources.jar", SourceAndJavaDocValidationCheck.makeSourceCodeName("/dir/file.ear"));
	}

	private boolean errorExist(String error) {
		return failedChecks.stream().anyMatch(err -> err.getProblem().equals(error));
	}

}
