package org.jboss.nexus;

import org.sonatype.nexus.capability.CapabilitySupport;

import javax.inject.Inject;

public abstract class MavenCentralDeployCapabilityParent<ConfigT extends MavenCentralDeployCapabilityConfigurationParent> extends CapabilitySupport<ConfigT> {

	@Inject
	private MavenCentralDeploy mavenCentralDeploy;

	@Override
	protected void configure(ConfigT config)  {
		mavenCentralDeploy.updateConfiguration(config);
	}

	@Override
	public void onActivate(ConfigT config)  {
		mavenCentralDeploy.registerConfiguration(config);
	}


	@Override
	public void onPassivate(ConfigT config) {
		mavenCentralDeploy.unregisterConfiguration(config);
	}

	@Override
	protected void onRemove(ConfigT config) {
		mavenCentralDeploy.unregisterConfiguration(config);
	}
}
