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

	private Asset testAsset;

	private BlobRef fakeBlobRef = new BlobRef("node", "store", "blob");

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
		testAsset = new Asset().name("some/SomeProject.pom").blobRef(fakeBlobRef);

		Mockito.when(blobStoreManager.get("store")).thenReturn(blobStore);
		Mockito.when(blobStore.get(fakeBlobRef.getBlobId())).thenReturn(testBlob); // set up the content of testBlob in the test

//		Mockito.when(component.version()).thenReturn("version");
//		Mockito.when(component.requireVersion()).thenReturn("version");
//		Mockito.when(component.group()).thenReturn("group");
//		Mockito.when(component.requireGroup()).thenReturn("group");


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

		assertTrue( errorExist("pom.xml validation failed: some/SomeProject.pom at [1,100]: licenses section appeared outside its expected location in xml."));
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

		assertTrue(errorExist("some/SomeProject.pom at [1,104] parsing error: ParseError at [row,col]:[1,104]\n" +
				  "Message: XML document structures must start and end within the same entity."));
	}
}