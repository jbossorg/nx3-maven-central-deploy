package org.jboss.nexus.contentimpl;

import org.jboss.nexus.content.Component;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.search.ComponentSearchResult;

import java.util.List;
import java.util.Objects;

/** Implementation of component for database environment.
 *
 */
public class ComponentDatabaseImpl extends Component {
    /** Constructor for the component.
     *
     * @param componentSearchResult component from the search
     * @param fluentComponent component directly so we can get the information, when the component was created in the system
     * @param contentBrowserDatabaseImpl the utility class, that caches tags for you
     */
    public ComponentDatabaseImpl(final ComponentSearchResult componentSearchResult, final FluentComponent fluentComponent, final ContentBrowserDatabaseImpl contentBrowserDatabaseImpl) {
        super(new DetachedEntityId(componentSearchResult.getId()), componentSearchResult.getGroup(), componentSearchResult.getName(), componentSearchResult.getVersion(), fluentComponent.created().toEpochSecond());

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<String> tagNames = (List)componentSearchResult.getAnnotation("tags");

        if(tagNames != null) {
            tagNames.stream()
                    .map(contentBrowserDatabaseImpl::findTag)
                    .filter(Objects::nonNull)
                    .forEach(tags()::add);
        }
    }

}
