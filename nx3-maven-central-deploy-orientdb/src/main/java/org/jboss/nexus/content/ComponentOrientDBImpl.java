package org.jboss.nexus.content;

import com.sonatype.nexus.tags.orient.TagComponent;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/** Implementation of component for OrientDB
 *
 */
public class ComponentOrientDBImpl extends Component {

    public ComponentOrientDBImpl(org.sonatype.nexus.repository.storage.Component storageComponent, long created, List<Asset> assets) {
        super(Objects.requireNonNull(storageComponent.getEntityMetadata()).getId(), storageComponent.requireGroup(), checkNotNull(storageComponent.name()), storageComponent.requireVersion(), created);

        if(TagComponent.class.isAssignableFrom(storageComponent.getClass())) {
            tags().addAll(((TagComponent) storageComponent).tags());
        }

        if(assets != null) {
            assetsInside().addAll(assets);
        }

    }

}
