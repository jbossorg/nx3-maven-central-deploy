package org.jboss.nexus;

import com.google.common.base.Strings;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import static com.google.common.base.Preconditions.checkState;

public class MavenCentralDeployTaskConfiguration extends TaskConfiguration {

    public static final String REPOSITORY = "repository";
    public static final String DRY_RUN = "dryRun";
    public static final String MARK_ARTIFACTS = "markArtifacts";
    public static final String FILTER = "filter";

    public static final String DISABLE_HAS_PROJECT = "disableHasProject";
    public static final String DISABLE_HAS_SCM = "disableHasScm";
    public static final String DISABLE_HAS_LICENSE = "disableHasLicense";
    public static final String DISABLE_HAS_PROJECT_NAME = "disableHasProjectname";
    public static final String DISABLE_HAS_DEVELOPER_INFO = "disableHasDeveloperinfo";
    public static final String DISABLE_HAS_PROJECT_DESCRIPTION = "disableHasProjectDescription";
    public static final String DISABLE_HAS_PROJECT_URL = "disableHasProjectUrl";
    public static final String DISABLE_HAS_GROUP = "disableHasGroup";
    public static final String DISABLE_HAS_ARTIFACT = "disableHasArtifact";
    public static final String DISABLE_HAS_VERSION = "disableHasVersion";
    public static final String DISABLE_HAS_SNAPSHOT_VERSION = "disableHasSnapshotversion";
    public static final String DISABLE_HAS_CHECKSUMS = "disableHasChecksums";
    public static final String DISABLE_HAS_SOURCE_CODES = "disableHasSourcecodes";
    public static final String DISABLE_HAS_JAVADOC = "disableHasJavadoc";



    public static final String PLAIN_TEXT_REPORT_OUTPUT_FILE = "ptOutputFile";

    public static final String LATEST_STATUS = "latest_status";

    public MavenCentralDeployTaskConfiguration() {
    }

    public MavenCentralDeployTaskConfiguration(TaskConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void validate() {
        checkState(!Strings.isNullOrEmpty(getRepository()), "Incomplete task configuration: repository");
        super.validate();
    }

    public String getRepository() {
        return getString(REPOSITORY);
    }

    public Boolean getDryRun() {
        return getBoolean(DRY_RUN, true);
    }

    public Boolean getMarkArtifacts() {
        return getBoolean(MARK_ARTIFACTS, false);
    }

    public String getFilter() {
        return getString(FILTER, "");
    }

    public Boolean getDisableHasProject() {
        return getBoolean(DISABLE_HAS_PROJECT, false);
    }

    public Boolean getDisableHasSCM() {
        return getBoolean(DISABLE_HAS_SCM, false);
    }

    public Boolean getDisableHasLicense() {
        return getBoolean(DISABLE_HAS_LICENSE, false);
    }

    public Boolean getDisableHasProjectName() {
        return getBoolean(DISABLE_HAS_PROJECT_NAME, false);
    }

    public Boolean getDisableHasDeveloperInfo() {
        return getBoolean(DISABLE_HAS_DEVELOPER_INFO, false);
    }

    public Boolean getDisableHasProjectDescription() {
        return getBoolean(DISABLE_HAS_PROJECT_DESCRIPTION, false);
    }

    public Boolean getDisableHasProjectURL() {
        return getBoolean(DISABLE_HAS_PROJECT_URL, false);
    }

    public Boolean getDisableHasGroup() {
        return getBoolean(DISABLE_HAS_GROUP, false);
    }

    public Boolean getDisableHasArtifact() {
        return getBoolean(DISABLE_HAS_ARTIFACT, false);
    }

    public Boolean getDisableHasVersion() {
        return getBoolean(DISABLE_HAS_VERSION, false);
    }

    public Boolean getDisableHasSnapshotVersion() {
        return getBoolean(DISABLE_HAS_SNAPSHOT_VERSION, false);
    }

    public Boolean getDisableHasChecksums() {
        return getBoolean(DISABLE_HAS_CHECKSUMS, false);
    }

    public Boolean getDisableHasSourceCodes() {
        return getBoolean(DISABLE_HAS_SOURCE_CODES, false);
    }

    public Boolean getDisableHasJavaDoc() {
        return getBoolean(DISABLE_HAS_JAVADOC, false);
    }

    public String getLatestStatus() {
        return getString(LATEST_STATUS);
    }

    public void setLatestStatus(String latestStatus) {
        setString(LATEST_STATUS, latestStatus);
    }

    /** Returns file name for plain text
     *
     * @return file name or null
     */
    public String getPlainTextReportOutputFile() {
        return getString(PLAIN_TEXT_REPORT_OUTPUT_FILE);
    }
}
