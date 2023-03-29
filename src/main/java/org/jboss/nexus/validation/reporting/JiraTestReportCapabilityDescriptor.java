package org.jboss.nexus.validation.reporting;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named(JiraTestReportCapabilityDescriptor.TYPE_ID)
public class JiraTestReportCapabilityDescriptor extends TestReportCapabilityDescriptorParent{
	public static final String TYPE_ID = "nx3Deploy.jira.report";

	private static final CapabilityType CAPABILITY_TYPE = CapabilityType.capabilityType(TYPE_ID);

	@SuppressWarnings("rawtypes")
	private static final List<FormField> formFields = new ArrayList<>();

	static {
		// TODO: 24.03.2023   add fields

	}

	private interface Messages
		 extends MessageBundle {

		@DefaultMessage("MCD - Jira Error Report")
		String name();
	}

	private static final Messages messages = I18N.create(JiraTestReportCapabilityDescriptor.Messages.class);

	public JiraTestReportCapabilityDescriptor() {
		super(formFields);
	}

	@Override
	public CapabilityType type() {
		return CAPABILITY_TYPE;
	}

	@Override
	public String name() {
		return messages.name();
	}
}
