package org.jboss.nexus.tagging;

import org.jboss.nexus.MavenCentralDeployCapabilityConfigurationParent;

import java.util.Map;

public class MCDTagSetupConfiguration extends MavenCentralDeployCapabilityConfigurationParent {
	
	public static final String DEPLOYED_TAG_NAME = "mcdTagSetup.deployedTagName";
	
	public static final String DEPLOYED_TAG_ATTRIBUTES = "mcdTagSetup.deployedAttributes";
	
	public static final String FAILED_TAG_NAME = "mcdTagSetup.failedTagName";
	
	public static final String FAILED_TAG_ATTRIBUTES = "mcdTagSetup.failedTagAttributes";
		
	public MCDTagSetupConfiguration(final Map<String, String> properties) {
		setDeployedTagName(properties.get(DEPLOYED_TAG_NAME));
		setDeployedTagAttributes(properties.get(DEPLOYED_TAG_ATTRIBUTES));
		setFailedTagName(properties.get(FAILED_TAG_NAME));
		setFailedTagAttributes(properties.get(FAILED_TAG_ATTRIBUTES));
	}
	
	private String deployedTagName;
	
	private String deployedTagAttributes;
	
	private String failedTagName;
	
	private String failedTagAttributes;

	public String getDeployedTagName() {
		return deployedTagName;
	}

	public void setDeployedTagName(String deployedTagName) {
		this.deployedTagName = deployedTagName;
	}

	public String getDeployedTagAttributes() {
		return deployedTagAttributes;
	}

	public void setDeployedTagAttributes(String deployedTagAttributes) {
		this.deployedTagAttributes = deployedTagAttributes;
	}

	public String getFailedTagName() {
		return failedTagName;
	}

	public void setFailedTagName(String failedTagName) {
		this.failedTagName = failedTagName;
	}

	public String getFailedTagAttributes() {
		return failedTagAttributes;
	}

	public void setFailedTagAttributes(String failedTagAttributes) {
		this.failedTagAttributes = failedTagAttributes;
	}

}
