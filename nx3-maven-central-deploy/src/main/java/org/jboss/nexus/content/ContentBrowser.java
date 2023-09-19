package org.jboss.nexus.content;

import org.jboss.nexus.Filter;
import org.jboss.nexus.MavenCentralDeployTaskConfiguration;
import org.jboss.nexus.validation.checks.FailedCheck;
import org.slf4j.Logger;
import org.sonatype.nexus.repository.Repository;

import java.util.List;

/**
 *  Interface to chew through the artifacts based on the current Nexus engine
 */
public interface ContentBrowser {
    void prepareValidationData(final Repository repository, final Filter filter, final MavenCentralDeployTaskConfiguration configuration, final List<FailedCheck> listOfFailures, final List<Component> toDeploy, Logger log);
}
