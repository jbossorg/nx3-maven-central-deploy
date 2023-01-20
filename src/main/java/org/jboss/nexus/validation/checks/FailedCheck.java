package org.jboss.nexus.validation.checks;

import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.repository.storage.Component;

import java.util.Objects;

/** The class holds the information about discovered problem.
 */
public class FailedCheck {
    private final Component component;
    private final String problem;

    public FailedCheck(Component component, String problem) {
        this.component = component;
        this.problem = problem;
    }

    /** Failing component */
    public Component getComponent() {
        return component;
    }

    /** The description of the problem */
    public String getProblem() {
        return problem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FailedCheck that = (FailedCheck) o;
        return Objects.equals(getComponent().requireGroup(), that.getComponent().requireGroup()) && Objects.equals(getComponent().requireVersion(), that.getComponent().requireVersion()) && Objects.equals(getComponent().name(), that.getComponent().name()) && Objects.equals(getProblem(), that.getProblem());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getComponent().requireGroup(), getComponent().name(), getComponent().requireVersion(), getProblem());
    }
}
