package org.jboss.nexus;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

/** Default Configuration for Deploying to Maven Central.
 *
 *
 */
public class MavenCentralDeployCentralSettingsConfiguration extends MavenCentralDeployCapabilityConfigurationParent {

    public static final String CENTRAL_USER = "centralUser";

    public static final String CENTRAL_PASSWORD = "centralPassword";

    public static final String CENTRAL_URL = "centralURL";

    public static final String CENTRAL_MODE = "centralMode";

   public MavenCentralDeployCentralSettingsConfiguration(final Map<String, String> properties) {
       setCentralUser(properties.get(CENTRAL_USER));
       setCentralPassword(properties.get(CENTRAL_PASSWORD));
       setCentralURL(properties.getOrDefault(CENTRAL_URL, "https://central.sonatype.com"));
       setCentralMode(properties.getOrDefault(CENTRAL_MODE, "USER_MANAGED"));
   }

    private String centralUser;

    private String centralPassword;

    private String centralURL;

    private String centralMode;


    /** User.
     *
     * @return the user in Maven Central service
     */
    public String getCentralUser() {
        return centralUser;
    }

    /** Gets the user. If the user is defined inside the task configuration, this value has precedence.
     *
     * @return the user in Maven Central service
     */
    public String getCentralUser(MavenCentralDeployTaskConfiguration localConfiguration ) {

        String variableValue = localConfiguration.fetchVariable(CENTRAL_USER);
        if(StringUtils.isNotBlank(variableValue))
            return variableValue;

        return localConfiguration.getString(CENTRAL_USER,  getCentralUser() );
    }

    public void setCentralUser(String centralUser) {
        this.centralUser = centralUser;
    }

    /** The password of the user in Maven Central service
     *
     * @return password
     */
    public String getCentralPassword() {
        return centralPassword;
    }

    /** The password of the user in Maven Central service. If defined in the task configuration, that value
     * has precedence.
     *
     * @return password
     */
    public String getCentralPassword(MavenCentralDeployTaskConfiguration localConfiguration) {
        String variableValue = localConfiguration.fetchVariable(CENTRAL_PASSWORD);
        if(StringUtils.isNotBlank(variableValue))
            return variableValue;

        return localConfiguration.getString(CENTRAL_PASSWORD, getCentralPassword());
    }

    public void setCentralPassword(String centralPassword) {
        this.centralPassword = centralPassword;
    }

    /** Returns the URL of the Maven Central service
     *
     * @return URL as a string
     */
    public String getCentralURL() {
        return centralURL;
    }

    /** Returns the URL of the Maven Central service. The information from the tasks local configuration has precedence.
     *
     * @return URL as a string
     */
    public String getCentralURL(MavenCentralDeployTaskConfiguration localConfiguration) {
        String variableValue = localConfiguration.fetchVariable(CENTRAL_URL);
        if(StringUtils.isNotBlank(variableValue))
            return variableValue;

        return localConfiguration.getString(CENTRAL_URL, getCentralURL());
    }

    public void setCentralURL(String centralURL) {
        this.centralURL = centralURL;
    }

    /** Gets mode of the deployment
     *
     * @return "USER_MANAGED" or "AUTOMATIC"
     */
    public String getCentralMode() {
        return centralMode;
    }

    /** Gets mode of the deployment. If defined in local configuration, the value has precedence.
     *
     * @return "USER_MANAGED" or "AUTOMATIC"
     */
    public String getCentralMode(MavenCentralDeployTaskConfiguration localConfiguration) {
        String variableValue = localConfiguration.fetchVariable(CENTRAL_MODE);
        if(StringUtils.isNotBlank(variableValue))
            return variableValue;

        return localConfiguration.getString(CENTRAL_MODE, getCentralMode());
    }

    public void setCentralMode(String centralMode) {
        this.centralMode = centralMode;
    }
}
