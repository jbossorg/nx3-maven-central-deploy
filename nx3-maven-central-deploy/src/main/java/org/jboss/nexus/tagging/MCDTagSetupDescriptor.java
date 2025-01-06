package org.jboss.nexus.tagging;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;

import static org.jboss.nexus.MavenCentralDeployTaskDescriptor.CATEGORY;
import static org.jboss.nexus.tagging.MCDTagSetupDescriptor.TYPE_ID;

@SuppressWarnings("rawtypes")
@Singleton
@Named(TYPE_ID)
@AvailabilityVersion(from = "1.0")
public class MCDTagSetupDescriptor extends CapabilityDescriptorSupport<MCDTagSetupConfiguration> implements Taggable {

	public MCDTagSetupDescriptor() {
		setExposed(true);
		setHidden(false);
	}

	public static final String TYPE_ID = "mcdTagSetupCapability";

	public static final CapabilityType CAPABILITY_TYPE = CapabilityType.capabilityType(TYPE_ID);

	@Override
	public CapabilityType type() {
		return CAPABILITY_TYPE;
	}

	@Override
	public String name() {
		return messages.name();
	}

	private interface Messages
		 extends MessageBundle {

		@DefaultMessage("MCD - Tags Setup")
		String name();

		@DefaultMessage("Successfully Deployed Artifact Tag Name")
		String deployedTagNameLabel();

		@DefaultMessage("Name of the tag, that will be set for artifacts, that were successfully deployed to Maven Central.")
		String deployedTagNameHelp();

		@DefaultMessage("Successfully Deployed Artifact Tag Attributes")
		String deployedTagAttributesLabel();

		@DefaultMessage("List of attributes in format attribute=value of tag attributes, that will be assigned to the tag. Multiple attributes are divided by a newline (property file like format). You may use ${variable} from the deployment task.")
		String deployedTagAttributesHelp();

		@DefaultMessage("Tag Name for Validation Failed Artifacts")
		String failedTagNameLabel();

		@DefaultMessage("If during the validation process an artifact will not comply the rules for deployment, it will be marked with this tag.")
		String failedTagNameHelp();

		@DefaultMessage("Tag Attributes for Validation Failed Artifacts")
		String failedTagAttributesLabel();

		@DefaultMessage("List of attributes in format attribute=value of tag attributes, that will be assigned to the tag. Multiple attributes are divided by a newline (property file like format). You may use ${variable} from the deployment task.")
		String failedTagAttributesHelp();

		@DefaultMessage("Setup names of the tags, that will be used for marking artifacts on successful and failed deployments to Maven Central. Tagging only works if you have NX3 Professional.")
		String about();
	}

	private static final Messages messages = I18N.create(Messages.class);

	private static final List<FormField> formFields ;
	static {
		List<FormField> initializeFormFields = new ArrayList<>(4);
		initializeFormFields.add(new StringTextFormField(MCDTagSetupConfiguration.DEPLOYED_TAG_NAME, messages.deployedTagNameLabel(), messages.deployedTagNameHelp(), FormField.OPTIONAL));
		initializeFormFields.add(new TextAreaFormField(MCDTagSetupConfiguration.DEPLOYED_TAG_ATTRIBUTES, messages.deployedTagAttributesLabel(), messages.deployedTagAttributesHelp(), FormField.OPTIONAL));
		initializeFormFields.add(new StringTextFormField(MCDTagSetupConfiguration.FAILED_TAG_NAME, messages.failedTagNameLabel(), messages.failedTagNameHelp(), FormField.OPTIONAL));
		initializeFormFields.add(new TextAreaFormField(MCDTagSetupConfiguration.FAILED_TAG_ATTRIBUTES, messages.failedTagAttributesLabel(), messages.failedTagAttributesHelp(), FormField.OPTIONAL));

		formFields = Collections.unmodifiableList(initializeFormFields);
	}

	@Override
	public List<FormField> formFields() {
		return formFields;
	}

	@Override
	protected String renderAbout() {
		return messages.about();
	}

	@Override
	protected MCDTagSetupConfiguration createConfig(Map<String, String> properties) {
		return new MCDTagSetupConfiguration(properties);
	}

	@Override
	public Set<Tag> getTags() {
		return Collections.singleton(Tag.categoryTag(CATEGORY));
	}
}
