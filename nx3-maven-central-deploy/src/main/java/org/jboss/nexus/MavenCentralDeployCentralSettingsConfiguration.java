package org.jboss.nexus;


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

    public static final String CENTRAL_PROXY = "centralProxy";
    public static final String CENTRAL_PROXY_PORT = "centralProxyPort";


    /** Constant for {@link #setCentralMode(String)}. Stays for deployment, that will wait for the deployer
     * to acknowledge the final push to Maven Central.
     */
    public static final String USER_MANAGED = "USER_MANAGED";


    /** Constant for {@link #setCentralMode(String)}. Stays for deployment, that will be done immediately
     * unless Sonatype finds some issues with the content.
     */
    public static final String AUTOMATIC = "AUTOMATIC";


    public MavenCentralDeployCentralSettingsConfiguration(final Map<String, String> properties) {
       setCentralUser(properties.get(CENTRAL_USER));
       setCentralPassword(properties.get(CENTRAL_PASSWORD));
       setCentralURL(properties.getOrDefault(CENTRAL_URL, "https://central.sonatype.com"));
       setCentralMode(properties.getOrDefault(CENTRAL_MODE, USER_MANAGED));
       setCentralProxy(properties.get(CENTRAL_PROXY));
       setCentralProxyPort(properties.get(CENTRAL_PROXY_PORT) != null ? Integer.parseInt(properties.get(CENTRAL_PROXY_PORT)): null) ;
   }

    private String centralUser;

    private String centralPassword;

    private String centralURL;

    private String centralMode;

    private String centralProxy;

    private Integer centralProxyPort;


    /** User.
     *
     * @return the user in Maven Central service
     */
    public String getCentralUser() {
        return centralUser;
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

    public void setCentralMode(String centralMode) {
        this.centralMode = centralMode;
    }

    /** Gets the proxy server host.
     *
     * @return hostname or null
     */
    public String getCentralProxy() {
        return centralProxy;
    }

    /** Sets proxy server.
     *
     * @param centralProxy host name of the proxy server
     */
    public void setCentralProxy(String centralProxy) {
        this.centralProxy = centralProxy;
    }

    /** Gets port of the proxy server used to connect to Maven Central.
     *
     * @return proxy server port
     */
    public Integer getCentralProxyPort() {
        return centralProxyPort;
    }

    /** Sets  port of the proxy server used to connect to Maven Central.
     *
     * @param centralProxyPort port number
     */
    public void setCentralProxyPort(Integer centralProxyPort) {
        this.centralProxyPort = centralProxyPort;
    }
}
