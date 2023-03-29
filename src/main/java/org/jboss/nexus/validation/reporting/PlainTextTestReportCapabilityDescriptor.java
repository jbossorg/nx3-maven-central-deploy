package org.jboss.nexus.validation.reporting;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.*;

import static org.jboss.nexus.validation.reporting.PlainTextTestReportCapabilityDescriptor.TYPE_ID;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

@Named(TYPE_ID)
@Singleton
public class PlainTextTestReportCapabilityDescriptor extends TestReportCapabilityDescriptorParent implements Taggable {

	public static final String TYPE_ID = "nx3Deploy.textReport";

	public static final CapabilityType TYPE = capabilityType(TYPE_ID);

	private interface Messages
		 extends MessageBundle {

		@DefaultMessage("MCD - Plaintext Error Report")
		String name();

		@DefaultMessage("Report File Name")
		String textDirLabel();

		@DefaultMessage("Name of the file, that will be used to store logs if there are any errors in validation. ${variable} will get replaced if you set the variable in the task configuration in the field variables.")
		String textDirHelp();

		@DefaultMessage("Append reports to file")
		String appendFileLabel();

		@DefaultMessage("If checked, the output will be appended to the file. Otherwise with each deployment the file gets replaced. Beware the file size!")
		String appendFileHelp();

		@DefaultMessage("Export template")
		String reportTemplateLabel();

		@DefaultMessage("Velocity template for the output.")
		String reportTemplateHelp();

		@DefaultMessage("Plugin to complement Maven Central Deployment tasks. It adds logging of errors to a plain text file.")
		String about();
	}

	private static final Messages messages = I18N.create(PlainTextTestReportCapabilityDescriptor.Messages.class);


	@SuppressWarnings("rawtypes")
	private static final List<FormField> formFields = new ArrayList<>();
	static {
		formFields.add(new StringTextFormField(PlainTextTestReportCapabilityConfiguration.FILE_NAME, messages.textDirLabel(), messages.textDirHelp(), FormField.MANDATORY));
		formFields.add(new CheckboxFormField(PlainTextTestReportCapabilityConfiguration.APPEND_FILE, messages.appendFileLabel(), messages.appendFileHelp(), FormField.OPTIONAL).withInitialValue(true));
		formFields.add(new TextAreaFormField(PlainTextTestReportCapabilityConfiguration.REPORT_TEMPLATE, messages.reportTemplateLabel(), messages.reportTemplateHelp(), FormField.MANDATORY).withInitialValue(PlainTextTestReportCapabilityConfiguration.DEFAULT_TEMPLATE));
	}

	public PlainTextTestReportCapabilityDescriptor() {
		super(formFields);
		setExposed(true);
		setHidden(false);
	}

	@Override
	public CapabilityType type() {
		return TYPE;
	}

	@Override
	public String name() {
		return messages.name();
	}

	@Override
	public int version() {
		return 0;
	}

	@Override
	protected TestReportCapabilityConfigurationParent createConfig(Map<String, String> properties) {
		return new PlainTextTestReportCapabilityConfiguration(properties);
	}

	@Override
	public String renderAbout() {
		return messages.about();
	}

}
