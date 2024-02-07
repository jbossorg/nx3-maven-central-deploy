package org.jboss.nexus.tagging;

import org.apache.commons.lang3.StringUtils;
import org.jboss.nexus.MavenCentralDeployCapabilityParent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;
import java.util.Map;

@Named(MCDTagSetupDescriptor.TYPE_ID)
public class MCDTagSetupCapability extends MavenCentralDeployCapabilityParent<MCDTagSetupConfiguration> {
	public MCDTagSetupCapability() {
		log.debug("Constructor");
	}

	@Override
	protected MCDTagSetupConfiguration createConfig(Map<String, String> properties) {
		return new MCDTagSetupConfiguration(properties);
	}


	@Nullable
	@Override
	protected String renderStatus()  {
		StringBuilder stringBuilder = new StringBuilder("Tag setup for Maven Central Deployment Plugin:<br/>");
		if(!context().isEnabled()) {
			stringBuilder.append("- the feature is disabled.");
		} else {
			String string = getConfig().getDeployedTagName();
			if(StringUtils.isNotBlank(string))
				stringBuilder.append("- successful deployment tag name: ").append(string).append("<br/>");

			string = getConfig().getDeployedTagAttributes();
			if(StringUtils.isNotBlank(string))
				stringBuilder.append("- successful deployment tag attributes: ").append(string).append("<br/>");

			string = getConfig().getFailedTagName();
			if(StringUtils.isNotBlank(string))
				stringBuilder.append("- failed deployment tag name: ").append(string).append("<br/>");

			string = getConfig().getFailedTagAttributes();
			if(StringUtils.isNotBlank(string))
				stringBuilder.append("- failed deployment tag attributes: ").append(string).append("<br/>");

			stringBuilder.setLength(stringBuilder.length()-5);
		}

		return stringBuilder.toString();
	}

	@Override
	protected String renderDescription() {
		return "Tag Configuration for Maven Central Deployment";
	}
}
