package org.jboss.nexus.content;

import org.jboss.nexus.validation.checks.FailedCheck;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


// TODO: 12.07.2023 this class!! 
public class AssetOrientDBImpl implements Asset {

    private final String name;
    private final BlobRef blobRef;

    private final BlobStoreManager blobStoreManager;

    public AssetOrientDBImpl(org.sonatype.nexus.repository.storage.Asset storageAsset, BlobStoreManager blobStoreManager) {
        super();
        this.name = storageAsset.name();
        this.blobRef = storageAsset.requireBlobRef();
        this.blobStoreManager = blobStoreManager;

    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public InputStream openContentInputStream() {
        Blob blob = Objects.requireNonNull(blobStoreManager.get(blobRef.getStore())).get(blobRef.getBlobId());
        if(blob == null) {
            throw new RuntimeException("Unable to get blob for "+name);
        } else {
            return blob.getInputStream();
        }
    }
}
