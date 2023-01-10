package org.jboss.nexus.validation.checks;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith((MockitoJUnitRunner.class))
public class PomXMLValidationCheckTest {

	private final BlobRef fakeBlobRef = new BlobRef("node", "store", "blob");

	@Mock
	private Blob testBlob;

	@Mock
	private BlobStore blobStore;

	@Mock
	private BlobStoreManager blobStoreManager;

	private PomXMLValidationCheck tested;

	@Mock
	private Component component;

	private List<Asset> listOfAssets;

	private List<FailedCheck> failedCheckList;

	@Before
	public void setup() {
		Asset testAsset = new Asset().name("some/SomeProject.pom").blobRef(fakeBlobRef);

		Mockito.when(blobStoreManager.get("store")).thenReturn(blobStore);
		Mockito.when(blobStore.get(fakeBlobRef.getBlobId())).thenReturn(testBlob); // set up the content of testBlob in the test

		tested = new PomXMLValidationCheck(blobStoreManager);

		failedCheckList = new ArrayList<>();

		listOfAssets = new ArrayList<>();
		listOfAssets.add(testAsset);
	}

	/**
	 * Prepares the blob to return the right input stream
	 *
	 * @param xml the content of the XML file
	 */
	private void prepareInputStream(@NotNull String xml) {
		Mockito.when(testBlob.getInputStream()).thenReturn(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
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
				  "   <organization></organization>" +
				  "   <group>group</group>" +
				  "   <artifact>artifact</artifact>" +
				  "   <version>version</version>" +
				  "   <scm>" +
				  "   </scm>" +
				  "   <licenses>" +
				  "       <license>" +
				  "       </license>" +
				  "       <license/>" +
				  "   </licenses>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);

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

		tested.validateComponent(component, listOfAssets, failedCheckList);

		assertTrue( errorExist("pom.xml validation failed: some/SomeProject.pom at [1,100]: license appeared outside its expected location in xml."));
		assertFalse( errorExist("some/SomeProject.pom does not have any license specified!"));
	}

	@Test
	public void validateComponentLicensesWrongLevel() {
		prepareInputStream(
			 "<project>" +
				  "   <scm>" +
				  "      <licenses>" +
				  "         <license/>" +
				  "      </licenses>" +
				  "   </scm>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);

		assertTrue( errorExist("pom.xml validation failed: some/SomeProject.pom at [1,34]: licenses section appeared outside its expected location in xml."));
		assertTrue( errorExist("some/SomeProject.pom does not have any license specified!"));
	}

	@Test
	public void validateComponentMissingLicense() {
		prepareInputStream(
			 "<project>" +
				  "   <scm>" +
				  "   </scm>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);

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
				  "   </licenses>" +
				  ""); // missing end tag

		tested.validateComponent(component, listOfAssets, failedCheckList);

		assertTrue(errorExist("some/SomeProject.pom parsing error: ParseError at [row,col]:[1,104]" +
				  "Message: XML document structures must start and end within the same entity."));
	}


	@Test
	public void validateComponentSeveralEntities() {
		final String[] Level2entities = {
			 "name",
			 "description",
			 "url",
			 "artifact"
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
			tested.validateComponent(component, listOfAssets, failedCheckList);
			assertTrue(errorExist(errors[i]));


			failedCheckList.clear();
			prepareInputStream(
				 xmlTemplate.replace("xxx", entity));

			tested.validateComponent(component, listOfAssets, failedCheckList);
			assertFalse(errorExist(errors[i++]));
		}
	}

	@Test
	public void validateComponentGroup() {
		prepareInputStream(
	   "<project>" +
			 "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
		String groupError = "some/SomeProject.pom does not have the project group specified!";
		assertTrue(errorExist(groupError));

		prepareInputStream(
	   "<project>" +
				  "<group></group>" +
			 "</project>");

		failedCheckList.clear();
		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertFalse("Direct group", errorExist(groupError));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				   "<parent>" +
				  "      <group>ff</group>" +
				   "</parent>" +
				  "</project>");

		failedCheckList.clear();
		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertFalse("Group in parent", errorExist(groupError));
	}

	@Test
	public void validateComponentVersion() {
		prepareInputStream(
	   "<project>" +
			 "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
		String versionError = "some/SomeProject.pom does not have the project version specified!";
		assertTrue(errorExist(versionError));

		prepareInputStream(
	   "<project>" +
				  "<version>version</version>" +
			 "</project>");

		failedCheckList.clear();
		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertFalse("Direct group", errorExist(versionError));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				   "<parent>" +
				  "      <version>version</version>" +
				   "</parent>" +
				  "</project>");

		failedCheckList.clear();
		tested.validateComponent(component, listOfAssets, failedCheckList);
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
				  "            <group>group</group>" +
				  "            <artifact>artifact</artifact>" +
				  "            <version>version</version>" +
				  "       </dependency>" +
				  "   </dependencies>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
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

		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertTrue("Parent SNAPSHOT", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				  "<parent>" +
				  "      <version>something</version>" +
				  "</parent>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertFalse("Parent SNAPSHOT OK", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				  "   <version>something-SNAPSHOT</version>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertTrue("Direct SNAPSHOT", errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

		failedCheckList.clear();
		prepareInputStream(
			 "<project>" +
				  "   <version>something</version>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
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

		tested.validateComponent(component, listOfAssets, failedCheckList);
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
				  "            <group>group</group>" +
				  "            <artifact>artifact</artifact>" +
				  "            <version>version-SNAPSHOT</version>" +
				  "       </dependency>" +
				  "   </dependencies>" +
				  "</project>");

		tested.validateComponent(component, listOfAssets, failedCheckList);
		assertTrue( errorExist("some/SomeProject.pom contains a dependency on a SNAPSHOT artifact!"));

	}



}