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
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import org.spine3.validation.options.RequiredOption;
import org.spine3.validation.options.ValidationProto;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static org.spine3.base.Commands.isCommandsFile;
import static org.spine3.base.Commands.belongsToEntity;

/**
 * Validates a message field according to Spine custom protobuf options and provides validation error messages.
 *
 * @param <V> a type of field values
 * @author Alexander Litus
 */
/* package */ abstract class FieldValidator<V> {

    private boolean isValid = true;
    private final List<String> errorMessages = newLinkedList();
    private final FieldDescriptor fieldDescriptor;
    private final ImmutableList<V> values;
    private final String fieldName;
    private final FileDescriptor file;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     */
    protected FieldValidator(FieldDescriptor descriptor, ImmutableList<V> values) {
        this.fieldDescriptor = descriptor;
        this.values = values;
        this.fieldName = fieldDescriptor.getName();
        this.file = fieldDescriptor.getFile();
    }

    /**
     * Validates a message field according to Spine custom protobuf options and sets validation error messages.
     *
     * <p>The default implementation calls {@link #validateEntityId()} method if needed.
     *
     * <p>Use {@link #assertFieldIsInvalid()} and {@link #addErrorMessage(String)} methods in custom implementations.
     */
    protected void validate() {
        if (isRequiredEntityIdField()) {
            validateEntityId();
        }
    }

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
            assertFieldIsInvalid();
            addErrorMessage(option);
            return;
        }
        for (V value : values) {
            if (isValueNotSet(value)) {
                assertFieldIsInvalid();
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
     * Returns {@code true} if the validated field is valid, {@code false} otherwise.
     */
    protected boolean isValid() {
        return isValid;
    }

    /**
     * Returns {@code true} if the validated field is invalid, {@code false} otherwise.
     */
    protected boolean isInvalid() {
        final boolean isInvalid = !isValid;
        return isInvalid;
    }

    /**
     * Sets {@code isValid} field to {@code false}.
     */
    protected void assertFieldIsInvalid() {
        this.isValid = false;
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
        final String msg = format(format, fieldName);
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
     * Returns a simple name of the field.
     */
    protected String getFieldName() {
        return fieldName;
    }

    /**
     * Returns {@code true} if the field must be an entity ID
     * (if the current Protobuf file is for entity commands and the field is the first in a command message);
     * {@code false} otherwise.
     */
    @SuppressWarnings("RedundantIfStatement")
    private boolean isRequiredEntityIdField() {
        if (!belongsToEntity(file)) {
            return false;
        }
        if (!isCommandsFile(file)) {
            return false;
        }
        if (!isFirstField()) {
            return false;
        }
        return true;
    }

    private boolean isFirstField() {
        final int index = fieldDescriptor.getIndex();
        final boolean isFirst = index == 0;
        return isFirst;
    }

    /**
     * Validates the current field as it is a required entity ID.
     *
     * <p>The field must not be repeated or not set.
     *
     * @see #isRequiredEntityIdField()
     */
    protected void validateEntityId() {
        if (fieldDescriptor.isRepeated()) {
            assertFieldIsInvalid();
            addErrorMessage(format("'%s' must not be a repeated field", fieldName));
            return;
        }
        final V value = getValues().get(0);
        if (isValueNotSet(value)) {
            assertFieldIsInvalid();
            addErrorMessage(format("'%s' must be set", fieldName));
        }
    }
}
