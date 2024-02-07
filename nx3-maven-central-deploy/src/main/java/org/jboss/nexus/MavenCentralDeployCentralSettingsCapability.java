package org.jboss.nexus;

import javax.inject.Named;
import java.util.Map;
@Named(MavenCentralDeployCentralSettingsDescriptor.TYPE_ID)
public class MavenCentralDeployCentralSettingsCapability extends MavenCentralDeployCapabilityParent<MavenCentralDeployCentralSettingsConfiguration> {

    @Override
    protected MavenCentralDeployCentralSettingsConfiguration createConfig(Map<String, String> properties) {
        return new MavenCentralDeployCentralSettingsConfiguration(properties);
    }

    @Override
    protected String renderDescription()  {
        return "Maven Central Deployment Default Configuration";
    }
}
