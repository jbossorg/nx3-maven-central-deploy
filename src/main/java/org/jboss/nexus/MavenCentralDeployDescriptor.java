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
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.formfields.*;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

@Named
@Singleton
public class MavenCentralDeployDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "mvn.central.deploy";

  private interface Messages
          extends MessageBundle
  {
    @DefaultMessage("Maven Central Deployment")
    String name();

    @DefaultMessage("Source Repository")
    String repositoryLabel();

    @DefaultMessage("Repository, that will be used as source for artifacts")
    String repositoryHelp();

    @DefaultMessage("Dry-Run")
    String dryRunLabel();

    @DefaultMessage("Test run with no push to Maven Central.")
    String dryRunHelp();


    @DefaultMessage("Mark Completed")
    String markArtifactsAfterRunLabel();

    @DefaultMessage("Uncheck while doing repeated testing. If checked, the processed artifacts are marked as deployed. With dry-run on it may be used for the initial marking of artifacts as deployed before you installed this plugin.")
    String markArtifactsAfterRunHelp();

    @DefaultMessage("Filter")
    String filterLabel();

    @DefaultMessage("Filter to select artifacts")
    String filterHelp();


    @DefaultMessage("Latest Results")
    String latestResultsLabel();

    @DefaultMessage("Text for latest results")
    String latestResultsHelp();

  }

//
//  private  final RepositoryCombobox repository ;
//
//  private  final CheckboxFormField dryRun ;
//
//  private final CheckboxFormField  markArtifactsTransferred;
//
//  private final StringTextFormField filter ;

  private static final Messages messages = I18N.create(Messages.class);

  @Inject
  public MavenCentralDeployDescriptor() {
    // filter, repository, deploy username, deploy password, deploy URL, source tag, done tag
    // dry-run
    // tests: JavaDoc, Sources, md5+sha1, sha256, sha512, license, project name and description, developer, scm, snapshot dependency

    super(TYPE_ID, MavenCentralDeployTask.class, "Maven Central Deployment", TaskDescriptorSupport.VISIBLE,
            TaskDescriptorSupport.EXPOSED,
            (new RepositoryCombobox(MavenCentralDeployTaskConfiguration.REPOSITORY, messages.repositoryLabel(), messages.repositoryHelp(), FormField.MANDATORY)).includingAnyOfFormats("maven2"),
            (new CheckboxFormField(MavenCentralDeployTaskConfiguration.DRY_RUN, messages.dryRunLabel(), messages.dryRunHelp(), FormField.OPTIONAL)).withInitialValue(true),
            (new CheckboxFormField(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, messages.markArtifactsAfterRunLabel(), messages.markArtifactsAfterRunHelp(), FormField.OPTIONAL)).withInitialValue(false),
            new StringTextFormField(MavenCentralDeployTaskConfiguration.FILTER, messages.filterLabel(), messages.filterHelp(), FormField.OPTIONAL),
            new TextAreaFormField(MavenCentralDeployTaskConfiguration.LATEST_STATUS, messages.latestResultsLabel(), messages.latestResultsHelp(), FormField.OPTIONAL, null, true )
            );

  }



}
