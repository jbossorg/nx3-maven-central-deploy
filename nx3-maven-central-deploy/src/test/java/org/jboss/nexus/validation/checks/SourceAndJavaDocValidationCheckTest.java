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

		Asset component1Asset = mockedAsset("something/file.jar"),
			 component1AssetSources = mockedAsset("something/file-sources.jar"),
			 component1AssetSingatureFile= mockedAsset("something/file.jar.asc"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

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

		Asset component1Asset = mockedAsset("something/file.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-sources.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetJavaDoc);

		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.jar"));
		assertFalse(errorExist("Source code is missing for something/file.jar"));
		assertTrue(errorExist("Signature file is missing for something/file.jar"));
	}

	@Test
	public void validateComponentJarMissingSources() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetJavaDoc);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertFalse(errorExist("JavaDoc is missing for something/file.jar"));
		assertTrue(errorExist("Source code is missing for something/file.jar"));

	}


	@Test
	public void validateComponentJarMissingSourcesAndJavaDoc() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.jar"));
		assertTrue(errorExist("Source code is missing for something/file.jar"));
		assertTrue(errorExist("Signature file is missing for something/file.jar"));

		assertSame(component1, failedChecks.get(0).getComponent());
		assertSame(component1, failedChecks.get(1).getComponent());
	}

	@Test
	public void validateComponentJarMissingSourcesAndJavaDocJavaDocDisabled() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_JAVADOC, true);

		Component component = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, failedChecks);

		assertFalse(errorExist("JavaDoc is missing for something/file.jar"));
		assertTrue(errorExist("Source code is missing for something/file.jar"));
		assertTrue(errorExist("Signature file is missing for something/file.jar"));

	}


	@Test
	public void validateComponentJarMissingSourcesAndJavaDocSourcesDisabled() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SOURCE_CODES, true);

		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.jar"));
		assertFalse(errorExist("Source code is missing for something/file.jar"));
	}
	@Test
	public void validateComponentJarMissingSignatureFile() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar"),
		    component1SignatureFile = mockedAsset("something/file.jar.asc");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);
		assertTrue(errorExist("Signature file is missing for something/file.jar"));

		failedChecks.clear();
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, true);
		assertFalse(errorExist("Signature file is missing for something/file.jar"));

		failedChecks.clear();
		assets.add(component1SignatureFile);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, false);
		assertFalse(errorExist("Signature file is missing for something/file.jar"));
	}

	@Test
	public void validateComponentWar() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.war");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.war"));
		assertTrue(errorExist("Source code is missing for something/file.war"));
	}

	@Test
	public void validateComponentWarOK() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.war"),
			component1AssetSignatureFile = mockedAsset("something/file.war.asc"),
			component1AssetSources = mockedAsset("something/file-sources.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSources);
		assets.add(component1AssetJavaDoc);
		assets.add(component1AssetSignatureFile);
		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentEar() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.ear");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.ear"));
		assertTrue(errorExist("Source code is missing for something/file.ear"));
	}

	@Test
	public void validateComponentEarOK() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.ear"),
			 component1AssetSignatueFile = mockedAsset("something/file.ear.asc"),
			 component1AssetSources = mockedAsset("something/file-sources.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSources);
		assets.add(component1AssetJavaDoc);
		assets.add(component1AssetSignatueFile);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentNoSuffix() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		when(component1.assetsInside()).thenReturn(assets);
		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentNoJava() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.txt");

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
