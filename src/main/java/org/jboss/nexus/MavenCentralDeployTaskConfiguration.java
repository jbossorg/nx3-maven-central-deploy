package org.jboss.nexus;

import com.google.common.base.Strings;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import static com.google.common.base.Preconditions.checkState;

public class MavenCentralDeployTaskConfiguration extends TaskConfiguration {

    public static final String REPOSITORY = "repository";
    public static final String DRY_RUN = "dryRun";
    public static final String MARK_ARTIFACTS = "markArtifacts";
    public static final String FILTER = "filter";

    public static final String VARIABLES = "variables";

    public static final String DISABLE_HAS_PROJECT = "disableHasProject";
    public static final String DISABLE_HAS_SCM = "disableHasScm";
    public static final String DISABLE_HAS_LICENSE = "disableHasLicense";
    public static final String DISABLE_HAS_PROJECT_NAME = "disableHasProjectName";
    public static final String DISABLE_HAS_DEVELOPER_INFO = "disableHasDeveloperInfo";
    public static final String DISABLE_HAS_PROJECT_DESCRIPTION = "disableHasProjectDescription";
    public static final String DISABLE_HAS_PROJECT_URL = "disableHasProjectUrl";
    public static final String DISABLE_HAS_GROUP = "disableHasGroup";
    public static final String DISABLE_HAS_ARTIFACT = "disableHasArtifact";
    public static final String DISABLE_HAS_VERSION = "disableHasVersion";
    public static final String DISABLE_HAS_SNAPSHOT_VERSION = "disableHasSnapshotVersion";
    public static final String DISABLE_HAS_CHECKSUMS_MD5 = "disableHasChecksumsMD5";

    public static final String DISABLE_HAS_CHECKSUMS_SHA1 = "disableHasChecksumsSHA1";
    public static final String DISABLE_HAS_SOURCE_CODES = "disableHasSourceCodes";
    public static final String DISABLE_HAS_JAVADOC = "disableHasJavadoc";


    public static final String LATEST_STATUS = "latestStatus";

    public static final String RUN_NUMBER = "runNumber";

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

    public Boolean getDisableHasChecksumsMD5() {
        return getBoolean(DISABLE_HAS_CHECKSUMS_MD5, false);
    }

    public Boolean getDisableHasChecksumsSHA1() {
        return getBoolean(DISABLE_HAS_CHECKSUMS_SHA1, false);
    }

    public Boolean getDisableHasSourceCodes() {
        return getBoolean(DISABLE_HAS_SOURCE_CODES, false);
    }

    public Boolean getDisableHasJavaDoc() {
        return getBoolean(DISABLE_HAS_JAVADOC, false);
    }

    public String getVariables() {
        return getString(VARIABLES);
    }

    @SuppressWarnings("unused")
    public String getLatestStatus() {
        return getString(LATEST_STATUS);
    }

    /** Number of the last run
     *
     * @return last run number
     */
    public Integer getRunNumber() {
        return getInteger(RUN_NUMBER, 0);
    }

    /** Increases run number by one.
     */
    public void increaseRunNumber() {
        setInteger(RUN_NUMBER, getRunNumber()+1);
    }

    public void setLatestStatus(String latestStatus) {
        setString(LATEST_STATUS, latestStatus);
    }

}
