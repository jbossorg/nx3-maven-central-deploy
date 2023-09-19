package org.jboss.nexus.contentimpl;

import org.jboss.nexus.content.Component;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

/** Implementation of component for database environment.
 *
 */
public class ComponentDatabaseImpl extends Component {

    public ComponentDatabaseImpl(FluentComponent fluentComponent, String id) {
        super(new DetachedEntityId(id),  fluentComponent.namespace(), fluentComponent.name(), fluentComponent.version());
        // TODO: 13.07.2023 Initialize tags! 
    }

}
