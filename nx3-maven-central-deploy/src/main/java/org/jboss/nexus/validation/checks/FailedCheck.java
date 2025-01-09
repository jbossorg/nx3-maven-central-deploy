package org.jboss.nexus.validation.checks;

import org.jboss.nexus.TemplateRenderingHelper;
import org.jboss.nexus.content.Component;

import java.util.Objects;

/** The class holds the information about discovered problem.
 */
public class FailedCheck {
    private final Component component;
    private final String problem;

    public FailedCheck(String problem) {
        this(null, problem);
    }

    public FailedCheck(Component component, String problem) {
        if(component == null) {
            this.component = NO_COMPONENT;
        } else
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

    /** The method returns true, if it contains a component
     *
     * @return true if the check has an error associated with
     */
    public boolean isHasComponent() {
        return component != NO_COMPONENT;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FailedCheck that = (FailedCheck) o;
        return Objects.equals(getComponent().group(), that.getComponent().group()) && Objects.equals(getComponent().version(), that.getComponent().version()) && Objects.equals(getComponent().name(), that.getComponent().name()) && Objects.equals(getProblem(), that.getProblem());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getComponent().group(), getComponent().name(), getComponent().version(), getProblem());
    }

    public static final Component NO_COMPONENT = new TemplateRenderingHelper.FictiveComponent(" ", "-", " ");

    /** Returns human-readable representation of component.
     *
      * @return name of the component
     */
    public String formatComponent() {
        return "["+component.group()+':'+component.name()+':'+component.version()+"]";
    }
}
