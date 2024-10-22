package org.jboss.nexus;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkState;
import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.*;

public class MavenCentralDeployTaskConfiguration extends TaskConfiguration {

    public static final String REPOSITORY = "repository";
    public static final String CONTENT_SELECTOR = "contentSelector";
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

    public static final String LATEST_COMPONENT_TIME = "latestComponentTime";

    public static final String PROCESSING_TIME_OFFSET = "processingTimeOffset";

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

    public String getContentSelector() {
        return getString(CONTENT_SELECTOR, "");
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

    /** Marks the creation time of the latest component, that was successfully deployed to Maven Central.
     *
     * @param latestComponentTime time of the newest component successfully deployed in this deployment in epoch seconds
     */
    public void setLatestComponentTime(String latestComponentTime) {
        if(StringUtils.isNumeric(latestComponentTime)) {
            setLong(LATEST_COMPONENT_TIME, Long.parseLong(latestComponentTime));
        } else
            setString(LATEST_COMPONENT_TIME, null); // remove the value from the configuration
    }

    /** Creation time of the last component, that was successfully published to Maven Central
     *
     * @return time in seconds after Jan 01st 1970
     */
    public Long getLatestComponentTime() {
        return getLong(LATEST_COMPONENT_TIME, -1);
    }

    /** Sets the time, when new components will be ignored during the synchronization so the deployer has time to finish the deployment.
     *
     * @param processingTimeOffset minutes
     */
    public void setProcessingTimeOffset(Integer processingTimeOffset) {
          setInteger(PROCESSING_TIME_OFFSET, processingTimeOffset);
    }


    /** Gets the time, when new components will be ignored during the synchronization so the deployer has time to finish the deployment.
     *
     * @return minutes
     */
    public int getProcessingTimeOffset() {
          return getInteger(PROCESSING_TIME_OFFSET, 10);
    }


    /** Tries to fetch a variable from variables.
     *
     * @param variable the name of the variable
     * @return null or value of the variable
     */
    public String fetchVariable(@NotNull String variable) {
        if(StringUtils.isNotBlank(getVariables())) {
            try {
                Properties properties = new Properties();
                properties.load(new StringReader(getVariables()));
                return properties.getProperty(variable);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    ////////////////////////
    /** Gets the deployment user.
     *
     * @return the user in Maven Central service
     */
    public String getCentralUser() {
        return getString(CENTRAL_USER);
    }

    /** Gets the user. If the user is defined inside the task configuration, this value has precedence. The highest
     * precedence has the variable value.
     *
     * @param registeredDefaults  optional default configuration of the deployment
     *
     * @return the user in Maven Central service
     */
    public String getCentralUser(MavenCentralDeployCentralSettingsConfiguration registeredDefaults ) {
        String variableValue = fetchVariable(CENTRAL_USER);
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        variableValue = getCentralUser();
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        if(registeredDefaults != null)
            return registeredDefaults.getCentralUser();

        return null;
    }

    public void setCentralUser(String centralUser) {
        setString(CENTRAL_USER, centralUser);
    }

    /** The password of the user in Maven Central service
     *
     * @return password
     */
    public String getCentralPassword() {
        return getString(CENTRAL_PASSWORD);
    }

    /** The password of the user in Maven Central service. If defined in the task configuration, that value
     * has precedence.
     *
     * @return password
     */
    public String getCentralPassword(MavenCentralDeployCentralSettingsConfiguration registeredDefaults) {
        String variableValue = fetchVariable(CENTRAL_PASSWORD);
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        variableValue = getCentralPassword();
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        if(registeredDefaults != null)
            return registeredDefaults.getCentralPassword();

        return null;
    }

    public void setCentralPassword(String centralPassword) {
        setString(CENTRAL_PASSWORD, centralPassword);
    }

    /** Returns the URL of the Maven Central service
     *
     * @return URL as a string
     */
    public String getCentralURL() {
        return getString(CENTRAL_URL);
    }

    /** Returns the URL of the Maven Central service. The information from the tasks local configuration has precedence.
     *
     * @return URL as a string
     */
    public String getCentralURL(MavenCentralDeployCentralSettingsConfiguration registeredDefaults) {
        String variableValue = fetchVariable(CENTRAL_URL);
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        variableValue = getCentralURL();
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        if(registeredDefaults != null)
            return registeredDefaults.getCentralURL();

        return null;
    }

    public void setCentralURL(String centralURL) {
        setString(CENTRAL_URL, centralURL);
    }

    /** Gets mode of the deployment
     *
     * @return "USER_MANAGED" or "AUTOMATIC"
     */
    public String getCentralMode() {
        return getString(CENTRAL_MODE);
    }

    /** Gets mode of the deployment. If defined in local configuration, the value has precedence.
     *
     * @return "USER_MANAGED" or "AUTOMATIC"
     */
    public String getCentralMode(MavenCentralDeployCentralSettingsConfiguration registeredDefaults) {
        String variableValue = fetchVariable(CENTRAL_MODE);
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        variableValue = getCentralMode();
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        if(registeredDefaults != null)
            return registeredDefaults.getCentralMode();

        return null;
    }

    /** The method suggests a nice name of the bundle from the current configuration
     *
     * @return human-readable name of the bundle
     */
    public String getBundleName() {
        StringBuilder builder = new StringBuilder();
        if(StringUtils.isNotEmpty(getName())) {
            builder.append(getName().trim().replace(" ", "_"));
        } else {
            builder.append("bundle");
        }

        if( getRunNumber() != null)
            builder.append('-').append(getRunNumber());

        return builder.toString();
    }

    public void setCentralMode(String centralMode) {
        setString(CENTRAL_MODE, centralMode);
    }



    /** Gets the proxy server host.
     *
     * @return hostname or null
     */
    public String getCentralProxy() {
        return getString(CENTRAL_PROXY);
    }

    /** Gets mode of the deployment. If defined in local configuration, the value has precedence.
     *
     * @param registeredDefaults registered defaults for the Maven Central deployment
     *
     * @return host name or null
     */
    public String getCentralProxy(MavenCentralDeployCentralSettingsConfiguration registeredDefaults) {
        String variableValue = fetchVariable(CENTRAL_PROXY);
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        variableValue = getCentralProxy();
        if(StringUtils.isNotEmpty(variableValue))
            return variableValue;

        if(registeredDefaults != null)
            return registeredDefaults.getCentralProxy();

        return null;
    }


    /** Sets proxy server.
     *
     * @param centralProxy host name of the proxy server
     */
    public void setCentralProxy(String centralProxy) {
        setString(CENTRAL_PROXY, centralProxy);
    }

    /** Gets port of the proxy server used to connect to Maven Central.
     *
     * @return proxy server port
     */
    public Integer getCentralProxyPort() {
        return getInteger(CENTRAL_PROXY_PORT);
    }

    /** Gets port of the proxy server used to connect to Maven Central. If defined in local configuration, the value has precedence.
     *
     * @return port number
     */
    public Integer getCentralProxyPort(MavenCentralDeployCentralSettingsConfiguration registeredDefaults) {
        String variableValue = fetchVariable(CENTRAL_PROXY_PORT);
        if(StringUtils.isNotEmpty(variableValue))
            return Integer.parseInt(variableValue);

        Integer proxyPort = getCentralProxyPort();
        if(proxyPort != null)
            return proxyPort;

        if(registeredDefaults != null)
            return registeredDefaults.getCentralProxyPort();

        return null;
    }


    /** Sets  port of the proxy server used to connect to Maven Central.
     *
     * @param centralProxyPort port number
     */
    public void setCentralProxyPort(Integer centralProxyPort) {
        setInteger(CENTRAL_PROXY_PORT, centralProxyPort);
    }

}
