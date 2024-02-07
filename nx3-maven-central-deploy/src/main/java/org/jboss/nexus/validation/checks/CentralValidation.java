package org.jboss.nexus.validation.checks;

import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.content.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.sonatype.goodies.common.Loggers;

import java.util.List;

/** Parent class for all the tests. */
public abstract class CentralValidation {

    protected final Logger log = Loggers.getLogger(getClass());

    /** Method tests one aspect of the component and reports problems. The method should not throw any exception, it should just add the problem to listOfFailures.
     *
     * @param component component to solve
     * @param listOfFailures the list where the possible validation failures will be reported
     *
     */
    public abstract void validateComponent(@NotNull MavenCentralDeployTaskConfiguration mavenCentralDeployTaskConfiguration, @NotNull Component component, @NotNull List<FailedCheck> listOfFailures); // TODO: 06.12.2022 add error reporting parameter

}
