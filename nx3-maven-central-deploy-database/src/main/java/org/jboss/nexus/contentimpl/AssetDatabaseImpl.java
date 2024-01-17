package org.jboss.nexus.contentimpl;

import org.jboss.nexus.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class AssetDatabaseImpl implements Asset {

    private Long size;

    /** Constructor using {@link FluentAsset} class.
     *
      * @param fluentAsset asset
     */
    public AssetDatabaseImpl(FluentAsset fluentAsset) {
        this.fluentAsset = fluentAsset;
    }

    private final FluentAsset fluentAsset;

    @Override
    public String name() {
        return fluentAsset.path(); // TODO: 12.07.2023  check if this is right!
    }

    @Override
    public long size() {
        if(this.size != null) {
            return this.size;
        } else
            try (Content content = fluentAsset.download()) {
                return this.size = content.getSize();
            } catch (IOException e) {
                return -1;
            }
    }

    @Override
    public InputStream openContentInputStream() throws IOException {
        return fluentAsset.download().openInputStream();
    }
}
