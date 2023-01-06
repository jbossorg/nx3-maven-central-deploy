package org.jboss.nexus.testutils;

import static org.mockito.Mockito.*;

import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

public class Utils {
	public static void mockComponent(Component component, String group, String artifact, String version) {
		 if(group != null)
		   when(component.group()).thenReturn(group);

		 if(artifact != null)
		   when(component.version()).thenReturn(version);

		 if(version != null)
		   when(component.name()).thenReturn(artifact);
	}

	public static void mockAsset(Asset asset, String name) {
		 when(asset.name()).thenReturn(name);
	}

	/** Prepares mock asset with a given name
	 *
	 * @param name name of the asset
	 * @return mocked asset
	 */
	public static Asset mockedAsset(@NotNull String name) {
		Asset result = mock(Asset.class, name);
		when(result.name()).thenReturn(name);
		return result;
	}



}
