package org.jboss.nexus.validation.checks;

import org.sonatype.nexus.repository.storage.Component;

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
}
