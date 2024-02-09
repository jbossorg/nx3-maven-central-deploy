package org.jboss.nexus;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jboss.nexus.MavenCentralDeployCentralSettingsConfiguration.USER_MANAGED;
import static org.jboss.nexus.MavenCentralDeployTaskDescriptor.CATEGORY;

@Singleton
@Named(MavenCentralDeployCentralSettingsDescriptor.TYPE_ID)
@SuppressWarnings("rawtypes")
public class MavenCentralDeployCentralSettingsDescriptor extends CapabilityDescriptorSupport<MavenCentralDeployCentralSettingsConfiguration> implements Taggable {

    public static final String TYPE_ID = "mcdDefaultCentralSettings";
    public static final CapabilityType CAPABILITY_TYPE = CapabilityType.capabilityType(TYPE_ID);

    private interface Messages
            extends MessageBundle {

        @DefaultMessage("Username")
        String centralUserLabel();

        @DefaultMessage("The user name in https://cenral.sonaype.com .")
        String centralUserHelp();

        @DefaultMessage("Password")
        String centralPasswordLabel();

        @DefaultMessage("Password of the account in https://cenral.sonaype.com .")
        String centralPasswordHelp();

        @DefaultMessage("Publishing mode")
        String centralModeLabel();

        @DefaultMessage("Use USER_MANAGED for manual confirmation of deployments or AUTOMATIC, so that validated deployments are published automatically.")
        String centralModeHelp();

        @DefaultMessage("Deployment URL")
        String centralUrlLabel();

        @DefaultMessage("Change only in case you want to publish the artifacts to a different (e.g. test) environment.")
        String centralUrlHelp();

        @DefaultMessage("MCD - Publishing Configuration")
        String name();

        @DefaultMessage("Default configuration for deploying to Maven Central. If configured here, you do not need to repeat those fields in all the tasks.")
        String about();
    }

    private static final MavenCentralDeployCentralSettingsDescriptor.Messages messages = I18N.create(MavenCentralDeployCentralSettingsDescriptor.Messages.class);

    private static final List<FormField> formFields ;
    static {
        @SuppressWarnings("rawtypes")
        final List<FormField> initializeFormFields = new ArrayList<>(4);
        initializeFormFields.add(new StringTextFormField(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_USER, messages.centralUserLabel(), messages.centralUserHelp(), FormField.MANDATORY));
        initializeFormFields.add(new PasswordFormField(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_PASSWORD, messages.centralPasswordLabel(), messages.centralPasswordHelp(), FormField.MANDATORY));
        initializeFormFields.add(new StringTextFormField(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_MODE, messages.centralModeLabel(), messages.centralModeHelp(), FormField.MANDATORY, "USER_MANAGED|AUTOMATIC").withInitialValue(USER_MANAGED));
        initializeFormFields.add(new StringTextFormField(MavenCentralDeployCentralSettingsConfiguration.CENTRAL_URL, messages.centralUrlLabel(), messages.centralUrlHelp(), FormField.MANDATORY).withInitialValue("https://central.sonatype.com"));

        formFields = Collections.unmodifiableList(initializeFormFields);
    }

    @Override
    public CapabilityType type() {
        return CAPABILITY_TYPE;
    }

    @Override
    public String name() {
        return messages.name();
    }

    @Override
    public String renderAbout() {
        return messages.about();
    }


    @Override
    public List<FormField> formFields() {
        return formFields;
    }

    @Override
    public Set<Tag> getTags() {
        return Collections.singleton(Tag.categoryTag(CATEGORY));
    }
}
