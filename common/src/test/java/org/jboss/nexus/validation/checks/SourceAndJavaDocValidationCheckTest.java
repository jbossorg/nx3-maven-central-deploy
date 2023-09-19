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
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSources);
		assets.add(component1AssetJavaDoc);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

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

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.jar"));
		assertFalse(errorExist("Source code is missing for something/file.jar"));
	}

	@Test
	public void validateComponentJarMissingSources() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetJavaDoc);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertFalse(errorExist("JavaDoc is missing for something/file.jar"));
		assertTrue(errorExist("Source code is missing for something/file.jar"));

	}


	@Test
	public void validateComponentJarMissingSourcesAndJavaDoc() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.jar"));
		assertTrue(errorExist("Source code is missing for something/file.jar"));

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

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, assets, failedChecks);

		assertFalse(errorExist("JavaDoc is missing for something/file.jar"));
		assertTrue(errorExist("Source code is missing for something/file.jar"));
	}


	@Test
	public void validateComponentJarMissingSourcesAndJavaDocSourcesDisabled() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SOURCE_CODES, true);

		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.jar"));
		assertFalse(errorExist("Source code is missing for something/file.jar"));
	}

	@Test
	public void validateComponentWar() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.war");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.war"));
		assertTrue(errorExist("Source code is missing for something/file.war"));
	}

	@Test
	public void validateComponentWarOK() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.war"),
			component1AssetSources = mockedAsset("something/file-sources.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSources);
		assets.add(component1AssetJavaDoc);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentEar() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.ear");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(errorExist("JavaDoc is missing for something/file.ear"));
		assertTrue(errorExist("Source code is missing for something/file.ear"));
	}

	@Test
	public void validateComponentEarOK() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.ear"),
			 component1AssetSources = mockedAsset("something/file-sources.jar"),
			 component1AssetJavaDoc = mockedAsset("something/file-javadoc.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);
		assets.add(component1AssetSources);
		assets.add(component1AssetJavaDoc);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentNoSuffix() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentNoJava() {
		Component component1 = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.txt");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component1, assets, failedChecks);

		assertTrue(failedChecks.isEmpty());
	}

	@Test
	public void validateComponentDisabledSourceAndJavaDoc() {
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_JAVADOC, true);
		mavenCentralDeployTaskConfiguration.setBoolean(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SOURCE_CODES, true);

		Component component = mock(Component.class);

		Asset component1Asset = mockedAsset("something/file.jar");

		List<Asset> assets = new ArrayList<>();
		assets.add(component1Asset);

		tested.validateComponent(mavenCentralDeployTaskConfiguration, component, assets, failedChecks);

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
