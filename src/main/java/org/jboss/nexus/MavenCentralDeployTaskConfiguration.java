package org.jboss.nexus;

import com.google.common.base.Strings;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;

public class MavenCentralDeployTaskConfiguration extends TaskConfiguration {

    static final String REPOSITORY = "repository";
    static final String DRY_RUN = "dryRun";
    static final String MARK_ARTIFACTS = "markArtifacts";
    static final String FILTER = "filter";

    static final String LATEST_STATUS = "latest_status";

    public MavenCentralDeployTaskConfiguration() {
        super();
    }

      private String repository;

      private Boolean dryRun;

      private Boolean markArtifacts;

      private String filter;


      // fixme latest status added for testing
      private String latestStatus;


    public MavenCentralDeployTaskConfiguration(TaskConfiguration configuration) {
        super(configuration);

        repository = configuration.getString(REPOSITORY);
        dryRun = configuration.getBoolean(DRY_RUN, true);
        markArtifacts = configuration.getBoolean(MARK_ARTIFACTS, false);
        filter = configuration.getString(FILTER);
        latestStatus = configuration.getString(LATEST_STATUS);
    }

    @Override
    public TaskConfiguration apply(TaskConfiguration from) {
        repository = from.getString(REPOSITORY);
        dryRun = from.getBoolean(DRY_RUN, true);
        markArtifacts = from.getBoolean(MARK_ARTIFACTS, false);
        filter = from.getString(FILTER, "");
        latestStatus = from.getString(LATEST_STATUS);

        return super.apply(from);
    }

    @Override
    public void validate() {
        checkState(!Strings.isNullOrEmpty(getRepository()), "Incomplete task configuration: repository");
        super.validate();
    }

    public String getRepository() {
        return repository;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public Boolean getMarkArtifacts() {
        return markArtifacts;
    }

    public String getFilter() {
        return filter;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public void setLatestStatus(String latestStatus) {
        if(!Objects.equals(latestStatus, this.latestStatus)) {
            setString(LATEST_STATUS, latestStatus);
        }
    }
}
