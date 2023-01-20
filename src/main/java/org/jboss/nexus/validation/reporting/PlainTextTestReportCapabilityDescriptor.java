package org.jboss.nexus.validation.reporting;

import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.TextAreaFormField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.jboss.nexus.validation.reporting.PlainTextTestReportCapabilityDescriptor.TYPE_ID;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;
import org.sonatype.goodies.i18n.MessageBundle;


// FIXME: 17.01.2023 Probably not needed class

//@Named(TYPE_ID)
//@Singleton
public class PlainTextTestReportCapabilityDescriptor extends TestReportCapabilityDescriptorParent {

	public static final String TYPE_ID = "nx3Deploy.textReport";

	public static final CapabilityType TYPE = capabilityType(TYPE_ID);

	@SuppressWarnings("rawtypes")
	private static final List<FormField> formFields = new ArrayList<>();

	static {
		formFields.add(new TextAreaFormField("textDir"));
	}
	private interface Messages
		 extends MessageBundle {
		@DefaultMessage("Report File Name")
		String textDirLabel();

		@DefaultMessage("File, that will receive ")
		String textDirHelp();
	}

	public PlainTextTestReportCapabilityDescriptor() {
		super(formFields);
	}

	@Override
	public CapabilityType type() {
		return TYPE;
	}

	@Override
	public String name() {
		return "Maven Central Deployment Plugin - text error report";
	}

	@Override
	public int version() {
		return 0;
	}
}
