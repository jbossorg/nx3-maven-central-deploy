package org.jboss.nexus.testutils;

import static org.mockito.Mockito.*;
import org.jboss.nexus.content.Asset;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class Utils {
	public static void mockAsset(Asset asset, String name) {
		when(asset.name()).thenReturn(name);
	}

	/** Mocks Asset using predefined content.
	 *
	 * @param asset mocked instance of {@link Asset}
	 * @param name name of the asset (usually path to the asset)
	 * @param content The text to be in the asset
	 */
	public static void mockAsset( @NotNull Asset asset, String name, @NotNull String content) {
		when(asset.name()).thenReturn(name);
		try {
			when(asset.openContentInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Prepares mock asset with a given name
	 *
	 * @param name name of the asset
	 * @return mocked asset
	 */
	public static Asset mockedAsset(@NotNull String name, String content) {
		Asset result = mock(Asset.class, name);
		mockAsset(result, name, content);
		return result;
	}

	/** Prepares mock asset with a given name
	 *
	 * @param name name of the asset
	 * @return mocked asset
	 */
	public static Asset mockedAsset(@NotNull String name) {
		Asset result = mock(Asset.class, name);
		mockAsset(result, name);
		return result;
	}



}
