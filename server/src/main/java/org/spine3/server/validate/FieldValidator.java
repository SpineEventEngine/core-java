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
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import com.google.protobuf.Message;
import org.spine3.validation.options.ValidationProto;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

/**
 * Validates a message field according to Spine custom protobuf options and provides validation error messages.
 *
 * @author Alexander Litus
 */
/* package */ abstract class FieldValidator {

    private boolean isFieldInvalid = false;

    private final List<String> errorMessages = newLinkedList();

    private final FieldDescriptor fieldDescriptor;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     */
    protected FieldValidator(FieldDescriptor descriptor) {
        this.fieldDescriptor = descriptor;
    }

    /**
     * Creates a new validator instance according to the field type and validates the field.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValue a value of the field to validate
     */
    /* package */ static FieldValidator newInstance(FieldDescriptor descriptor, Object fieldValue) {
        final FieldValidator validator;
        final JavaType fieldType = descriptor.getJavaType();
        switch (fieldType) {
            case MESSAGE:
                final List<Message> messages = toValuesList(fieldValue);
                validator = new MessageFieldValidator(descriptor, messages);
                break;
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
                final List<Number> numbers = toValuesList(fieldValue);
                validator = new NumberFieldValidator(descriptor, numbers);
                break;
            case STRING:
                final Collection<String> strings = toValuesList(fieldValue);
                validator = null; // TODO:2016-03-15:alexander.litus: impl
                break;
            case BYTE_STRING:
                final Collection<ByteString> byteStrings = toValuesList(fieldValue);
                validator = null; // TODO:2016-03-15:alexander.litus: impl
                break;
            case BOOLEAN:
            case ENUM:
            default:
                throw fieldTypeIsNotSupported(descriptor);
        }
        validator.validate();
        return validator;
    }

    @SuppressWarnings({"unchecked", "IfMayBeConditional"})
    private static <T> List<T> toValuesList(Object fieldValue) {
        if (fieldValue instanceof List) {
            return (List<T>) fieldValue;
        } else {
            return singletonList((T) fieldValue);
        }
    }

    /**
     * Validates a message field according to Spine custom protobuf options and sets validation error messages.
     *
     * <p>Uses {@link #setIsFieldInvalid(boolean)} and {@link #addErrorMessage(String)} methods.
     */
    protected abstract void validate();

    /**
     * Returns {@code true} if the validated field is invalid, {@code false} otherwise.
     */
    protected boolean isFieldInvalid() {
        return isFieldInvalid;
    }

    /**
     * Sets {@code isFieldInvalid} field.
     */
    protected void setIsFieldInvalid(boolean isFieldInvalid) {
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
     * Returns a validation option.
     *
     * @param extension an extension key used to obtain a validation option
     */
    protected <Option extends Message> Option getOption(GeneratedExtension<FieldOptions, Option> extension) {
        final Option option = fieldDescriptor.getOptions().getExtension(extension);
        return option;
    }

    /**
     * Returns a field descriptor.
     */
    protected FieldDescriptor getFieldDescriptor() {
        return fieldDescriptor;
    }

    private static IllegalArgumentException fieldTypeIsNotSupported(FieldDescriptor descriptor) {
        final String msg = format("The field type is not supported for validation: %s", descriptor.getType());
        throw new IllegalArgumentException(msg);
    }
}
