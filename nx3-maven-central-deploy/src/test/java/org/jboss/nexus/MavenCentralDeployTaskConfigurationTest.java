package org.jboss.nexus;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.*;
import static org.jboss.nexus.MavenCentralDeployTaskConfiguration.VARIABLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MavenCentralDeployTaskConfigurationTest {

    private MavenCentralDeployTaskConfiguration tested;

    private MavenCentralDeployCentralSettingsConfiguration registeredDefaultConfiguration;

    private static final Map<String, String> defaultConfiguration = new HashMap<>();

    static {
        defaultConfiguration.put(CENTRAL_URL, "https://default.sonatype.org");
        defaultConfiguration.put(CENTRAL_USER, "default_user");
        defaultConfiguration.put(CENTRAL_PASSWORD, "default_password");
        defaultConfiguration.put(CENTRAL_MODE, "default_mode");
        defaultConfiguration.put(CENTRAL_PROXY, "proxy.company.org");
        defaultConfiguration.put(CENTRAL_PROXY_PORT, "3127");
    }

    @Before
    public void setUp() {
        tested = new MavenCentralDeployTaskConfiguration();
        registeredDefaultConfiguration = new MavenCentralDeployCentralSettingsConfiguration(defaultConfiguration);
    }

    @Test
    public void getCentralUser() {
        assertNull(tested.getCentralUser());
        assertNull(tested.getCentralUser(null));

        assertEquals("default_user", tested.getCentralUser(registeredDefaultConfiguration));
        
        tested.setCentralUser("user");
        assertEquals("user", tested.getCentralUser());
        assertEquals("Direct configuration precedence", "user", tested.getCentralUser(null));
        assertEquals("Direct configuration precedence 2","user",  tested.getCentralUser(registeredDefaultConfiguration));

        tested.setString(VARIABLES, CENTRAL_USER+"=variable_user");
        assertEquals("Variable precedence", "variable_user", tested.getCentralUser(null));
        assertEquals("Variable precedence 2", "variable_user", tested.getCentralUser(registeredDefaultConfiguration));

    }

    @Test
    public void getCentralPassword() {
        assertNull(tested.getCentralPassword());
        assertNull(tested.getCentralPassword(null));

        assertEquals("default_password", tested.getCentralPassword(registeredDefaultConfiguration));

        tested.setCentralPassword("password");
        assertEquals("password", tested.getCentralPassword());
        assertEquals("Direct configuration precedence", "password", tested.getCentralPassword(null));
        assertEquals("Direct configuration precedence 2", "password", tested.getCentralPassword(registeredDefaultConfiguration));

        tested.setString(VARIABLES, CENTRAL_PASSWORD+"=variable_password");
        assertEquals("Variable precedence", "variable_password", tested.getCentralPassword(null));
        assertEquals("Variable precedence 2", "variable_password", tested.getCentralPassword(registeredDefaultConfiguration));
    }

    @Test
    public void getCentralURL() {
        assertNull(tested.getCentralURL());
        assertNull(tested.getCentralURL(null));

        assertEquals("https://default.sonatype.org", tested.getCentralURL(registeredDefaultConfiguration));

        tested.setCentralURL("https://central.sonatype.org");
        assertEquals("https://central.sonatype.org", tested.getCentralURL());
        assertEquals("Direct configuration precedence", "https://central.sonatype.org", tested.getCentralURL(null));
        assertEquals("Direct configuration precedence 2", "https://central.sonatype.org", tested.getCentralURL(registeredDefaultConfiguration));

        tested.setString(VARIABLES, CENTRAL_URL+"=https://variable.sonatype.org");
        assertEquals("Variable precedence", "https://variable.sonatype.org", tested.getCentralURL(null));
        assertEquals("Variable precedence 2", "https://variable.sonatype.org", tested.getCentralURL(registeredDefaultConfiguration));
    }

    @Test
    public void getCentralMode() {
        assertNull( tested.getCentralMode()); // this one has default
        assertNull( tested.getCentralMode(null));

        assertEquals("default_mode", tested.getCentralMode(registeredDefaultConfiguration));

        tested.setCentralMode(AUTOMATIC);
        assertEquals(AUTOMATIC, tested.getCentralMode());
        assertEquals("Direct configuration precedence", AUTOMATIC, tested.getCentralMode(null));
        assertEquals("Direct configuration precedence 2", AUTOMATIC, tested.getCentralMode(registeredDefaultConfiguration));

        tested.setString(VARIABLES, CENTRAL_MODE+"=variable_mode");
        assertEquals("Variable precedence", "variable_mode", tested.getCentralMode(null));
        assertEquals("Variable precedence 2", "variable_mode", tested.getCentralMode(registeredDefaultConfiguration));
    }


    @Test
    public void getCentralProxy() {
        assertNull(tested.getCentralProxy());
        assertNull(tested.getCentralProxy(null));

        assertEquals("proxy.company.org", tested.getCentralProxy(registeredDefaultConfiguration));

        tested.setCentralProxy("new.proxy.company.org");
        assertEquals("new.proxy.company.org", tested.getCentralProxy());
        assertEquals("Direct configuration precedence", "new.proxy.company.org", tested.getCentralProxy(null));
        assertEquals("Direct configuration precedence 2", "new.proxy.company.org", tested.getCentralProxy(registeredDefaultConfiguration));

        tested.setString(VARIABLES, CENTRAL_PROXY+"=variable.proxy.company.org");
        assertEquals("Variable precedence", "variable.proxy.company.org", tested.getCentralProxy(null));
        assertEquals("Variable precedence 2", "variable.proxy.company.org", tested.getCentralProxy(registeredDefaultConfiguration));
    }


    @Test
    public void getCentralProxyPort() {
        assertNull(tested.getCentralProxyPort());
        assertNull(tested.getCentralProxyPort(null));

        assertEquals(Integer.valueOf(3127), tested.getCentralProxyPort(registeredDefaultConfiguration));

        tested.setCentralProxyPort(3129);
        assertEquals(Integer.valueOf(3129), tested.getCentralProxyPort());
        assertEquals("Direct configuration precedence", Integer.valueOf(3129), tested.getCentralProxyPort(null));
        assertEquals("Direct configuration precedence 2", Integer.valueOf(3129), tested.getCentralProxyPort(registeredDefaultConfiguration));

        tested.setString(VARIABLES, CENTRAL_PROXY_PORT+"=3140");
        assertEquals("Variable precedence", Integer.valueOf(3140), tested.getCentralProxyPort(null));
        assertEquals("Variable precedence 2", Integer.valueOf(3140), tested.getCentralProxyPort(registeredDefaultConfiguration));
    }
}
