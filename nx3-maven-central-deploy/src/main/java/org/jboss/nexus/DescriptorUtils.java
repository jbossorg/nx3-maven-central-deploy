package org.jboss.nexus;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sonatype.nexus.formfields.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The class with utilities to combine configurations.
 *
 */
@SuppressWarnings("rawtypes")
public class DescriptorUtils {

    /** The method combines the arrays of form fields.
     *
     * @param mainFields The main field. All fields are copied as they were defined.
     * @param ignoredFields optional list of field names, that will not be taken from the other fields
     * @param otherFormFields one or more other arrays with fields. If they were mandatory, they will become optional.
     *
     * @return a FormField array with combined fields.
     */
    @SafeVarargs
    public static FormField[] combineDescriptors(@NotNull List<FormField> mainFields, @Nullable List<String> ignoredFields, List<FormField>... otherFormFields ) {

        List<FormField> fields = new ArrayList<>(mainFields);

        final List<String> ignoreFields;
        if(ignoredFields == null) {
            ignoreFields = Collections.emptyList();
        } else
            ignoreFields = ignoredFields;

        for(List<FormField> otherFormField : otherFormFields  ) {
            // re-create the mandatory fields, so they are always optional here. Default values and similar are retained.
            otherFormField.stream()
                    .filter(f -> !ignoreFields.contains(f.getId())).forEach(field -> {
                        if (field.isRequired()) {
                            // we must duplicate required fields, because we do not want them to be mandatory here
                            switch (field.getType()) {
                                case "text-area":
                                    TextAreaFormField textAreaFormField = new TextAreaFormField(field.getId(), field.getLabel(), field.getHelpText(), false, field.getRegexValidation(), field.isReadOnly());
                                    if (field.getInitialValue() != null) {
                                        textAreaFormField.withInitialValue(((TextAreaFormField) field).getInitialValue());
                                    }
                                    textAreaFormField.setReadOnly(field.isReadOnly());
                                    textAreaFormField.setDisabled(field.isDisabled());
                                    textAreaFormField.setRegexValidation(field.getRegexValidation());
                                    fields.add(textAreaFormField);
                                    break;
                                case "string":
                                    StringTextFormField stringTextFormField = new StringTextFormField(field.getId(), field.getLabel(), field.getHelpText(), false, field.getRegexValidation());
                                    if (field.getInitialValue() != null)
                                        stringTextFormField.withInitialValue(((StringTextFormField) field).getInitialValue());
                                    stringTextFormField.setReadOnly(field.isReadOnly());
                                    stringTextFormField.setDisabled(field.isDisabled());
                                    stringTextFormField.setRegexValidation(field.getRegexValidation());

                                    fields.add(stringTextFormField);
                                    break;
                                case "number":
                                    NumberTextFormField numberTextFormField = new NumberTextFormField(field.getId(), field.getLabel(), field.getHelpText(), false, field.getRegexValidation());
                                    if (field.getInitialValue() != null)
                                        numberTextFormField.withInitialValue(((NumberTextFormField) field).getInitialValue());

                                    Number minimumValue = ((NumberTextFormField) field).getMinimumValue();
                                    if(minimumValue != null)
                                        numberTextFormField.withMinimumValue(minimumValue);

                                    Number maximumValue = ((NumberTextFormField) field).getMaximumValue();
                                    if(maximumValue != null)
                                        numberTextFormField.withMaximumValue(maximumValue);

                                    numberTextFormField.setReadOnly(field.isReadOnly());
                                    numberTextFormField.setDisabled(field.isDisabled());

                                    fields.add(numberTextFormField);
                                    break;
                                case "boolean":
                                    CheckboxFormField checkboxFormField = new CheckboxFormField(field.getId(), field.getLabel(), field.getHelpText(), false);
                                    if (field.getInitialValue() != null)
                                        checkboxFormField.withInitialValue(((CheckboxFormField) field).getInitialValue());

                                    checkboxFormField.setReadOnly(field.isReadOnly());
                                    checkboxFormField.setDisabled(field.isDisabled());

                                    fields.add(checkboxFormField);
                                    break;
                                default:
                                    throw new RuntimeException("Programming error - Unexpected field type!");
                            }

                        } else
                            fields.add(field);
                    });
        }

        return fields.toArray(new FormField[0]);
    }
}
