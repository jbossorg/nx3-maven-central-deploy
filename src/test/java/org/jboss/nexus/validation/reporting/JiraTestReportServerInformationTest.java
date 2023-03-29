package org.jboss.nexus.validation.reporting;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraTestReportServerInformationTest {

	JiraTestReportServerInformation tested;

	@Mock
	private URLConnection mockedURLConnection;


	@Before
	public void setup() {
		tested = spy(new JiraTestReportServerInformation());
		tested.setJiraConnectionInformation("https://issues.something.org", null, "someToken", null, null, null);

		try {
			when(tested.buildConnection(anyString())).thenReturn(mockedURLConnection);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void buildConnectionWithSlash() throws IOException {
		tested = new JiraTestReportServerInformation();
		tested.setJiraConnectionInformation("https://issues.something.org/", null, "someToken", null, null, null);

		URLConnection urlConnection = tested.buildConnection("/endpoint");

		assertEquals("issues.something.org", urlConnection.getURL().getHost());
		assertEquals("/endpoint", urlConnection.getURL().getPath());
	}
	@Test
	public void buildConnectionWithoutSlash() throws IOException {
		tested = new JiraTestReportServerInformation();
		tested.setJiraConnectionInformation("https://issues.something.org", null, "someToken", null, null, null);

		URLConnection urlConnection = tested.buildConnection("/endpoint");

		assertEquals("issues.something.org", urlConnection.getURL().getHost());
		assertEquals("/endpoint", urlConnection.getURL().getPath());
	}


	@Test
	public void findProjectIDReal() {
		tested = new JiraTestReportServerInformation();
		tested.setJiraConnectionInformation("https://issues.stage.redhat.com", null, "Mzk1NjI0ODQ2MjUxOtn4WO/W1MFmmDk2iuXICGPTjTTZ", null,"squid.corp.redhat.com", 3128); // fixme credentials remove!

		tested.tryJira();
		// TODO: 28.03.2023 remove this!!!!!!
	}

	@Test
	public void findProjectID() throws IOException {
		final String json = "{\n" +
			 "   \"id\": \"1234\",\n" +
			 "   \"key\": \"KEY\",\n" +
			 "   \"name\": \"Jira Project\"\n" +
			 "}";

		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		int id = tested.findProjectID("KEY");
		assertEquals(1234, id);

		id = tested.findProjectID("KEY");
		assertEquals(1234, id); // from cache

		verify(mockedURLConnection).getInputStream(); // called just once
	}

	@Test(expected = RuntimeException.class)
	public void findProjectIDNotExist() throws IOException {
		when(mockedURLConnection.getInputStream()).thenThrow(new FileNotFoundException("Not found!"));

		try {
			tested.findProjectID("KEY");
		} catch (RuntimeException e) {
			assertEquals("Project was not found: KEY", e.getMessage());
			throw e;
		}
	}

	@Test(expected = RuntimeException.class)
	public void findProjectIDIOError() throws IOException {
		when(mockedURLConnection.getInputStream()).thenThrow(new IOException("Network error!"));

		try {
			tested.findProjectID("KEY");
		} catch (RuntimeException e) {
			assertEquals("Error connecting to Jira server: Network error!", e.getMessage());
			throw e;
		}
	}


	@Test
	public void setJiraConnectionInformationTokenAuthentication() {
		tested.setJiraConnectionInformation("https://issues.stage.something.org", null, "token", null,null, null);
		assertEquals("Bearer token", tested.getAuthentication());
	}

	@Test
	public void setJiraConnectionInformationBasicAuthentication() {
		tested.setJiraConnectionInformation("https://issues.stage.something.org", "username", null, "password",null, null);
		assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", tested.getAuthentication());
	}

	@Test
	public void setJiraConnectionInformationNoAuthentication() {
		tested.setJiraConnectionInformation("https://issues.stage.something.org", "username", null, null,null, null);
		assertNull(tested.getAuthentication());

		tested.setJiraConnectionInformation("https://issues.stage.something.org", null, null, "password",null, null);
		assertNull(tested.getAuthentication());
	}


	@Test
	public void findPriorityIDNumber() throws IOException {
		String result = tested.findPriorityID("1000");

		assertEquals("1000", result);
		verify(mockedURLConnection, never()).getInputStream();
	}


	private static final String PRIORITY_RESPONSE = "[\n" +
		 " \t{\"id\": \"100\", \"name\": \"critical\"},\n" +
		 "\t{\"id\": \"200\", \"name\": \"moderate\"},\n" +
		 "  {\"id\": \"300\", \"name\": \"low\"}\n" +
		 "]";

	@Test
	public void findPriorityID() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.getBytes(StandardCharsets.UTF_8)));

		String result = tested.findPriorityID("moderate");
		assertEquals("200", result);

		assertEquals("100", tested.findPriorityID("critical"));
		assertEquals("300", tested.findPriorityID("low"));

		verify(mockedURLConnection).getInputStream(); // called just once
	}

	@Test
	public void findPriorityIDWithRefresh() throws IOException {
		when(mockedURLConnection.getInputStream())
			 .thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.getBytes(StandardCharsets.UTF_8)))
			 .thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.replace("critical", "serious").getBytes(StandardCharsets.UTF_8)));

		tested.findPriorityID("low"); // initiate the "old" priorities

		String result = tested.findPriorityID("serious");
		assertEquals("100", result);

		verify(mockedURLConnection, times(2)).getInputStream();

	}

	@Test(expected = RuntimeException.class)
	public void findPriorityIDWithRefreshFailed() throws IOException {
		when(mockedURLConnection.getInputStream()).thenReturn(new ByteArrayInputStream(PRIORITY_RESPONSE.getBytes(StandardCharsets.UTF_8)));


		try {
			tested.findPriorityID("serious");
		} catch (RuntimeException e) {
			assertEquals("Priority serious was not found!", e.getMessage());

			verify(mockedURLConnection).getInputStream(); // called just once
			throw e;
		}
	}



}