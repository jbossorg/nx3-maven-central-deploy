package org.jboss.nexus.content;

import com.sonatype.nexus.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.common.entity.EntityId;

import java.util.*;

/** Parent class for working with artifacts.  */
public abstract class Component {

    protected Component(EntityId entityId, String group, String name, String version, long created, Set<Tag> tags) {
        this(entityId, group,name, version, created);
        this.tags.addAll(tags);
    }

    protected Component(EntityId entityId, String group, String name, String version, long created) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.entityId = entityId;
        this.created = created;
    }

    private final long created;

    private final String group, name, version;

    private final Set<Tag> tags = new HashSet<>();

    private final List<Asset> assetList = new ArrayList<>();

    private final EntityId entityId;

    /** Group coordinate of the artifact
     *
     * @return group part of coordinate
     */
    public String group() {
        return group;
    }

    /** Artifact name.
     *
     * @return artifact coordinate
     */
    public String name() {
        return name;
    }

    /** Version of the component.
     *
     * @return version coordinate
     */
    public String version() {
        return version;
    }


    public EntityId entityId() {
        return entityId;
    }


    /** Tags associated with the component.
     *
     * @return tags
     */
    public @NotNull Set<Tag> tags() {
        return tags;
    }

    /** Assets, that belong to the component
     *
     * @return list of assets
     */
    public @NotNull List<Asset> assetsInside() {
        return assetList;
    } // fixme probably not needed

    /** Time, when the component was created (in seconds from January 1st 1970)
     *
     * @see java.time.OffsetDateTime#toEpochSecond()
     *
     * @return time, when the component was added to the system
     */
    public long getCreated() {
        return created;
    }

    @Override
    public int hashCode() {
        return Objects.hash(group(), name(), version());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != getClass())
            return false;

        if(obj == this)
            return true;

        Component that = (Component) obj;

        return  Objects.equals(group(), that.group()) && Objects.equals(name(), that.name()) && Objects.equals(version(), version());
    }

    /** Alternative to {@link #toString()}. It is designed to provide the same result as {@link org.sonatype.nexus.repository.storage.Component#toStringExternal()}
     */
    public String toStringExternal() {
        return "group=" + group() +
                ", name=" + name() +
                ", version=" + version() +
                ", format=maven2";
    }
}
