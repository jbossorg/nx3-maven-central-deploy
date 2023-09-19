package org.jboss.nexus.content;

import com.sonatype.nexus.tags.orient.TagComponent;

import static com.google.common.base.Preconditions.checkNotNull;

/** Implementation of component for OrientDB
 *
 */
public class ComponentOrientDBImpl extends Component {

    public ComponentOrientDBImpl(org.sonatype.nexus.repository.storage.Component storageComponent) {
        super(storageComponent.getEntityMetadata().getId(), storageComponent.requireGroup(), checkNotNull(storageComponent.name()), storageComponent.requireVersion()); // todo entityId

        if(TagComponent.class.isAssignableFrom(storageComponent.getClass())) {
            tags().addAll(((TagComponent) storageComponent).tags());
        }
    }

}