package org.jboss.nexus.validation.reporting;

import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.formfields.FormField;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class TestReportCapabilityDescriptorParent implements CapabilityDescriptor {

    @Override
    public CapabilityType type() {
        return null;
    }

    @Override
    public String name() {
        // TODO: 06.12.2022
        return getClass().getName();
    }

    @Override
    public List<FormField> formFields() {
        // TODO: 06.12.2022
        return null;
    }

    @Override
    public boolean isExposed() {
        // TODO: 06.12.2022
        return false;
    }

    @Override
    public boolean isHidden() {
        // TODO: 06.12.2022
        return false;
    }

    @Override
    public String about() {
        // TODO: 06.12.2022
        return null;
    }

    @Override
    public void validate(@Nullable CapabilityIdentity id, Map<String, String> properties, ValidationMode validationMode) {
        // TODO: 06.12.2022 
    }

    @Override
    public int version() {
        return 0;
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
