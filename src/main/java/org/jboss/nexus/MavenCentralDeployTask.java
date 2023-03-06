/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.jboss.nexus;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.Nullable;
import org.jboss.nexus.tagging.MCDTagSetupCapability;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;


@Named
@TaskLogging(NEXUS_LOG_ONLY)
public class MavenCentralDeployTask
    extends TaskSupport
    implements Cancelable
{
  private final MavenCentralDeploy mavenCentralDeploy;
  private final MCDTagSetupCapability mcdTagSetupCapability;

  @Inject
  public MavenCentralDeployTask(final MavenCentralDeploy mavenCentralDeploy, @Nullable final MCDTagSetupCapability mcdTagSetupCapability) {
    this.mavenCentralDeploy = checkNotNull(mavenCentralDeploy);
    this.mcdTagSetupCapability = mcdTagSetupCapability;
  }

  @Override
  protected String execute()  {
    mavenCentralDeploy.processDeployment((MavenCentralDeployTaskConfiguration) getConfiguration());
    return "We are sending something!";
  }

  @Override
  public void cancel() {
    mavenCentralDeploy.cancelDeployment();
    super.cancel();
  }

  @Override
  protected TaskConfiguration createTaskConfiguration() {
    return new MavenCentralDeployTaskConfiguration();
  }

  @Override
  public String getMessage() {
    return "Deploy selected artifacts to Maven Central";
  }
}
