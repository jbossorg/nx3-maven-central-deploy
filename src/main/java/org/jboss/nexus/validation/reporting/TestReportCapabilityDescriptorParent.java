package org.jboss.nexus.validation.reporting;

import org.sonatype.nexus.capability.*;
import org.sonatype.nexus.formfields.FormField;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.nexus.MavenCentralDeployTaskDescriptor.CATEGORY;

@SuppressWarnings("rawtypes")
public abstract class TestReportCapabilityDescriptorParent extends CapabilityDescriptorSupport<TestReportCapabilityConfigurationParent> implements Taggable {

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
    public String renderAbout() {
        return "Report for failed Maven Central Deployment: "+ getClass().getName();
    }

    @Override
    public Map<String, String> convert(Map<String, String> properties, int fromVersion) {
        return null;
    }

    @Override
    public boolean isDuplicated(@Nullable CapabilityIdentity id, Map<String, String> properties) {
        return false;
    }

    @Override
    public Set<Tag> getTags() {
        return Collections.singleton(Tag.categoryTag(CATEGORY));
    }
}
