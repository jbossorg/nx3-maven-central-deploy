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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.*;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import java.util.Collections;
import java.util.Set;

@Named
@Singleton
@AvailabilityVersion(from = "1.0")
public class MavenCentralDeployTaskDescriptor
    extends TaskDescriptorSupport implements Taggable
{
  public static final String TYPE_ID = "mvn.central.deploy";
  public static final String CATEGORY = "MVN Central";

  @Override
  public Set<Tag> getTags() {
    return Collections.singleton(Tag.categoryTag(CATEGORY));
  }

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

    @DefaultMessage("Content Selector")
    String contentSelector();

    @DefaultMessage("Optionally you may filter artifacts by their content selector. If used alongside Filter, both will be used to narrow search (AND).")
    String contentSelectorHelp();

    @DefaultMessage("Mark Completed")
    String markArtifactsAfterRunLabel(); // TODO: 06.03.2023  

    @DefaultMessage("Uncheck while doing repeated testing. If checked, the processed artifacts are marked as deployed. With dry-run on it may be used for the initial marking of artifacts as deployed before you installed this plugin.")
    String markArtifactsAfterRunHelp();

    @DefaultMessage("Processing Time Offset")
    String processingTimeOffsetLabel();

    @DefaultMessage("A time in minutes, when components will not yet be deployed by the task. The value is here in order to make sure the components are fully deployed to Nexus before they get checked and synchronization starts.")
    String processingTimeOffsetHelp();

    @DefaultMessage("Latest Run Timestamp")
    String latestRunTimeStampLabel();

    @DefaultMessage("Timestamp of the last component, that was successfully deployed to Maven Central. It is in seconds after January 1st 1970. You may remove it to re-publish already published content or set it to a specific value as needed.")
    String latestRunTimeStampHelp();

    @DefaultMessage("Filter")
    String filterLabel();

    @DefaultMessage("Filter to just some artifacts. Format of the field is <b>group</b>=XYZ&<b>name</b>=YZW&<b>version</b>=ZWA&<b>tag</b>=TTT&<b>tagAttr</b>=attribute!=AAA. If you omit the field, a match in a group or artifact or version will be searched for. Tag attribute allows logical operations &lt; &gt; &lt;= &gt;= = != &lt;&gt;")
    String filterHelp();


    @DefaultMessage("Task variables")
    String variablesLabel();

    @DefaultMessage("Variables in property file format, that will be shared across the reports and tests to identify your test (eg. project name, task identification etc.).")
    String variablesHelp();


    @DefaultMessage("Latest Results")
    String latestResultsLabel();

    @DefaultMessage("Text for latest results")
    String latestResultsHelp();

    @DefaultMessage("Disable project checking")
    String disableHasProjectLabel();

    @DefaultMessage("Disables checking, whether project element is present in pom.xml files. You should really not consider disabling this check.")
    String disableHasProjectHelp();

    @DefaultMessage("Disable SCM checking")
    String disableHasSCMLabel();

    @DefaultMessage("Disables checking, whether there is a section in pom.xml file, that contains information about source code repository such as git, svn or similar.")
    String disableHasSCMHelp();

    @DefaultMessage("Disable license checking")
    String disableHasLicenseLabel();

    @DefaultMessage("Disables check of license in pom.xml files.")
    String disableHasLicenseHelp();

    @DefaultMessage("Disable project name checking")
    String disableHasProjectNameLabel();

    @DefaultMessage("Disables of checking of project name in pom.xml files.")
    String disableHasProjectNameHelp();

    @DefaultMessage("Disable developer info checking")
    String disableHasDeveloperInfoLabel();

    @DefaultMessage("Disables the check, whether there is a developer information in the pom.xml files.")
    String disableHasDeveloperInfoHelp();

    @DefaultMessage("Disable project description checking")
    String disableHasProjectDescriptionLabel();

    @DefaultMessage("Disables of pom.xml check, that searches for developer information.")
    String disableHasProjectDescriptionHelp();

    @DefaultMessage("Disable URL checking")
    String disableHasProjectURLLabel();

    @DefaultMessage("Disables the test of project URL in pom.xml files.")
    String disableHasProjectURLHelp();

    @DefaultMessage("Disable group checking")
    String disableHasGroupLabel();

    @DefaultMessage("Disables the check, that searches for project group coordinate. You should really not consider disabling this check.")
    String disableHasGroupHelp();

    @DefaultMessage("Disable artifact checking")
    String disableHasArtifactLabel();

    @DefaultMessage("Disables the check, that searches for project artifact coordinate. You should really not consider disabling this check.")
    String disableHasArtifactHelp();

    @DefaultMessage("Disable version checking")
    String disableHasVersionLabel();

    @DefaultMessage("Disables the check, that searches for project version coordinate. You should really not consider disabling this check.")
    String disableHasVersionHelp();

    @DefaultMessage("Disable snapshot version checking")
    String disableHasSnapshotVersionLabel();

    @DefaultMessage("If you are releasing a release artifact, it should never depend on any snapshot artifact. You have been warned.")
    String disableHasSnapshotVersionHelp();

    @DefaultMessage("Disable MD5 checksum checking")
    String disableHasChecksumMD5Label();

    @DefaultMessage("Disables MD5 checksum file checking.")
    String disableHasChecksumMD5Help();

    @DefaultMessage("Disable SHA1 checksum checking")
    String disableHasChecksumSHA1Label();

    @DefaultMessage("Disables SHA1 checksum file checking")
    String disableHasChecksumSHA1Help();

    @DefaultMessage("Disable source code checking")
    String disableHasSourceCodeLabel();

    @DefaultMessage("Disables the check of the presence of source code files.")
    String disableHasSourceCodeHelp();

    @DefaultMessage("Disable JavaDoc checking")
    String disableHasJavaDocLabel();

    @DefaultMessage("Disables checks of JavaDoc files")
    String disableHasJavaDocHelp();

    @DefaultMessage("Disable signature file checking")
    String disableHasSignatureFileLabel();

    @DefaultMessage("Disables checks of missing signature files (.asc).")
    String disableHasSignatureFileHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  @SuppressWarnings("rawtypes")
  protected static final FormField[] taskFields = {
          new TextAreaFormField(MavenCentralDeployTaskConfiguration.LATEST_STATUS, messages.latestResultsLabel(), messages.latestResultsHelp(), FormField.OPTIONAL, null, true ),
          new RepositoryCombobox(MavenCentralDeployTaskConfiguration.REPOSITORY, messages.repositoryLabel(), messages.repositoryHelp(), FormField.MANDATORY).includingAnyOfFormats("maven2"),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DRY_RUN, messages.dryRunLabel(), messages.dryRunHelp(), FormField.OPTIONAL).withInitialValue(true),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.MARK_ARTIFACTS, messages.markArtifactsAfterRunLabel(), messages.markArtifactsAfterRunHelp(), FormField.OPTIONAL).withInitialValue(false),
          new NumberTextFormField(MavenCentralDeployTaskConfiguration.LATEST_COMPONENT_TIME, messages.latestRunTimeStampLabel(), messages.latestRunTimeStampHelp(), FormField.OPTIONAL),
          new NumberTextFormField(MavenCentralDeployTaskConfiguration.PROCESSING_TIME_OFFSET, messages.processingTimeOffsetLabel(), messages.processingTimeOffsetHelp(), FormField.OPTIONAL).withInitialValue(10).withMinimumValue(0),
          new SelectorComboFormField(MavenCentralDeployTaskConfiguration.CONTENT_SELECTOR, messages.contentSelector(), messages.contentSelectorHelp(), FormField.OPTIONAL),
          new StringTextFormField(MavenCentralDeployTaskConfiguration.FILTER, messages.filterLabel(), messages.filterHelp(), FormField.OPTIONAL),
          new TextAreaFormField(MavenCentralDeployTaskConfiguration.VARIABLES, messages.variablesLabel(), messages.variablesHelp(), FormField.OPTIONAL),

          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT_NAME, messages.disableHasProjectNameLabel(), messages.disableHasProjectNameHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT_DESCRIPTION, messages.disableHasProjectDescriptionLabel(), messages.disableHasProjectDescriptionHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT_URL, messages.disableHasProjectURLLabel(), messages.disableHasProjectURLHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SCM, messages.disableHasSCMLabel(), messages.disableHasSCMHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_DEVELOPER_INFO, messages.disableHasDeveloperInfoLabel(), messages.disableHasDeveloperInfoHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_LICENSE, messages.disableHasLicenseLabel(), messages.disableHasLicenseHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SNAPSHOT_VERSION, messages.disableHasSnapshotVersionLabel(), messages.disableHasSnapshotVersionHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SOURCE_CODES, messages.disableHasSourceCodeLabel(), messages.disableHasSourceCodeHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_JAVADOC, messages.disableHasJavaDocLabel(), messages.disableHasJavaDocHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_SIGNATURE_FILE, messages.disableHasSignatureFileLabel(), messages.disableHasSignatureFileHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_CHECKSUMS_MD5, messages.disableHasChecksumMD5Label(), messages.disableHasChecksumMD5Help(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_CHECKSUMS_SHA1, messages.disableHasChecksumSHA1Label(), messages.disableHasChecksumSHA1Help(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_PROJECT, messages.disableHasProjectLabel(), messages.disableHasProjectHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_GROUP, messages.disableHasGroupLabel(), messages.disableHasGroupHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_ARTIFACT, messages.disableHasArtifactLabel(), messages.disableHasArtifactHelp(), false),
          new CheckboxFormField(MavenCentralDeployTaskConfiguration.DISABLE_HAS_VERSION, messages.disableHasVersionLabel(), messages.disableHasVersionHelp(), false)
  };



  @SuppressWarnings("unused")
  public MavenCentralDeployTaskDescriptor() {

    super(TYPE_ID, MavenCentralDeployTask.class, messages.name(), TaskDescriptorSupport.VISIBLE,
            TaskDescriptorSupport.EXPOSED, taskFields
    );
  }

  public MavenCentralDeployTaskDescriptor(String id, Class<? extends Task> type, String name, boolean visible, boolean exposed, FormField... formFields) {
    super(id, type, name, visible, exposed, formFields);
  }

}
