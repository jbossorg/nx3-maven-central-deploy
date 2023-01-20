package org.jboss.nexus.validation.reporting;

import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public abstract class TestReportCapabilityDescriptorParent extends CapabilityDescriptorSupport<TestReportCapabilityConfigurationParent> {

    public TestReportCapabilityDescriptorParent(List<FormField> formFields) {
        this.formFields = formFields;
    }

    private final List<FormField> formFields;

    @Override
    public List<FormField> formFields() {
        return formFields;
    }

    @Override
    public boolean isExposed() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public String about() {
        return "Report for failed Maven Central Deployment: "+ getClass().getName();
    }

    @Override
    public void validate(@Nullable CapabilityIdentity id, Map<String, String> properties, ValidationMode validationMode) {
        // nothing yet
    }

    @Override
    public Map<String, String> convert(Map<String, String> properties, int fromVersion) {
        return null;
    }

    @Override
    public boolean isDuplicated(@Nullable CapabilityIdentity id, Map<String, String> properties) {
        return false;
    }
}
