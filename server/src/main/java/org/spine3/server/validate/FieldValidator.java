/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.server.validate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import org.spine3.validation.options.RequiredOption;
import org.spine3.validation.options.ValidationProto;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

/**
 * Validates a message field according to Spine custom protobuf options and provides validation error messages.
 *
 * @param <V> a type of field values
 * @author Alexander Litus
 */
/* package */ abstract class FieldValidator<V> {

    private boolean isFieldInvalid = false;
    private final List<String> errorMessages = newLinkedList();
    private final FieldDescriptor fieldDescriptor;
    private final ImmutableList<V> values;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     */
    protected FieldValidator(FieldDescriptor descriptor, ImmutableList<V> values) {
        this.fieldDescriptor = descriptor;
        this.values = values;
    }

    /**
     * Validates a message field according to Spine custom protobuf options and sets validation error messages.
     *
     * <p>Uses {@link #setIsFieldInvalid(boolean)} and {@link #addErrorMessage(String)} methods.
     */
    protected abstract void validate();

    /**
     * Returns an immutable list of the field values.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // is immutable list
    protected ImmutableList<V> getValues() {
        return values;
    }

    /**
     * Checks if the field is required and not set.
     *
     * <p>If the field is repeated, it must have at least one value set, and all its values must be valid.
     *
     * <p>It is required to override {@link #isValueNotSet(Object)} method to use this one.
     */
    protected void checkIfRequiredAndNotSet() {
        final RequiredOption option = getFieldOption(ValidationProto.required);
        if (!option.getValue()) {
            return;
        }
        if (values.isEmpty()) {
            setIsFieldInvalid(true);
            addErrorMessage(option);
            return;
        }
        for (V value : values) {
            if (isValueNotSet(value)) {
                setIsFieldInvalid(true);
                addErrorMessage(option);
                return; // return because one error message is enough for the "required" option
            }
        }
    }

    /**
     * Checks if the field value is not set.
     *
     * <p>If the field type is {@link Message}, it must be set to a non-default instance;
     * if it is {@link String} or {@link ByteString}, it must be set to a non-empty string or array.
     *
     * <p>The default implementation throws {@link UnsupportedOperationException}.
     *
     * @param value a field value to check
     * @return {@code true} if the field is not set, {@code false} otherwise
     */
    protected boolean isValueNotSet(V value) {
        throw new UnsupportedOperationException("The method is not implemented.");
    }

    /**
     * Returns {@code true} if the validated field is invalid, {@code false} otherwise.
     */
    protected boolean isFieldInvalid() {
        return isFieldInvalid;
    }

    /**
     * Sets {@code isFieldInvalid} field.
     */
    protected void setIsFieldInvalid(boolean isFieldInvalid) { // TODO:2016-03-18:alexander.litus: assert method
        this.isFieldInvalid = isFieldInvalid;
    }

    /**
     * Returns validation error messages.
     */
    protected List<String> getErrorMessages() {
        return ImmutableList.copyOf(errorMessages);
    }

    /**
     * Adds a validation error message to the collection of messages.
     *
     * @param msg an error message to add
     */
    protected void addErrorMessage(String msg) {
        errorMessages.add(msg);
    }

    private void addErrorMessage(RequiredOption option) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = format(format, getFieldDescriptor().getName());
        addErrorMessage(msg);
    }

    /**
     * Returns a validation error message format string (a custom one (if present) or the default one).
     *
     * @param option a validation option used to get the default message
     * @param customMsg a user-defined error message
     */
    protected String getErrorMessageFormat(Message option, String customMsg) {
        final String defaultMsg = option.getDescriptorForType().getOptions().getExtension(ValidationProto.defaultMessage);
        final String msg = customMsg.isEmpty() ? defaultMsg : customMsg;
        return msg;
    }

    /**
     * Returns a field validation option.
     *
     * @param extension an extension key used to obtain a validation option
     */
    protected <Option> Option getFieldOption(GeneratedExtension<FieldOptions, Option> extension) {
        final Option option = fieldDescriptor.getOptions().getExtension(extension);
        return option;
    }

    /**
     * Returns a field descriptor.
     */
    protected FieldDescriptor getFieldDescriptor() {
        return fieldDescriptor;
    }

    /**
     * Validates the current field as it is an entity ID.
     *
     * <p>The field must not be repeated or not set.
     */
    protected void validateEntityId() {
        final FieldDescriptor descriptor = getFieldDescriptor();
        if (descriptor.isRepeated()) {
            setIsFieldInvalid(true);
            addErrorMessage(format("'%s' must not be a repeated field", descriptor.getName()));
            return;
        }
        final V value = getValues().get(0);
        if (isValueNotSet(value)) {
            setIsFieldInvalid(true);
            addErrorMessage(format("'%s' must be set", descriptor.getName()));
        }
    }
}
