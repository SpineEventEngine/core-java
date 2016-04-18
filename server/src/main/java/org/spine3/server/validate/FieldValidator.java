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
import org.spine3.base.FieldPath;
import org.spine3.validate.options.ConstraintViolation;
import org.spine3.validate.options.RequiredOption;
import org.spine3.validate.options.ValidationProto;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static org.spine3.base.Commands.isEntityFile;
import static org.spine3.base.Commands.isCommandsFile;

/**
 * Validates messages according to Spine custom protobuf options and provides constraint violations found.
 *
 * @param <V> a type of field values
 * @author Alexander Litus
 */
/* package */ abstract class FieldValidator<V> {

    private static final String ENTITY_ID_REPEATED_FIELD_MSG = "Entity ID must not be a repeated field.";

    private final FieldDescriptor fieldDescriptor;
    private final ImmutableList<V> values;
    private final FieldPath fieldPath;

    private final List<ConstraintViolation> violations = newLinkedList();

    private final boolean isEntityFile;
    private final boolean isCommandsFile;
    private final boolean isFirstField;
    private final RequiredOption requiredOption;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param values values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    protected FieldValidator(FieldDescriptor descriptor, ImmutableList<V> values, FieldPath rootFieldPath) {
        this.fieldDescriptor = descriptor;
        this.values = values;
        this.fieldPath = rootFieldPath.toBuilder()
                .addFieldName(fieldDescriptor.getName())
                .build();
        final FileDescriptor file = fieldDescriptor.getFile();
        this.isEntityFile = isEntityFile(file);
        this.isCommandsFile = isCommandsFile(file);
        this.isFirstField = fieldDescriptor.getIndex() == 0;
        this.requiredOption = getFieldOption(ValidationProto.required);
    }

    /**
     * Checks if the field value is not set.
     *
     * <p>If the field type is {@link Message}, it must be set to a non-default instance;
     * if it is {@link String} or {@link ByteString}, it must be set to a non-empty string or array.
     *
     * @param value a field value to check
     * @return {@code true} if the field is not set, {@code false} otherwise
     */
    protected abstract boolean isValueNotSet(V value);

    /**
     * Validates messages according to Spine custom protobuf options and returns validation constraint violations found.
     *
     * <p>The default implementation calls {@link #validateEntityId()} method if needed.
     *
     * <p>Use {@link #addViolation(ConstraintViolation)} method in custom implementations.
     */
    protected List<ConstraintViolation> validate() {
        if (isRequiredEntityIdField()) {
            validateEntityId();
        }
        return ImmutableList.copyOf(violations);
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
            final ConstraintViolation violation = ConstraintViolation.newBuilder()
                    .setMsgFormat(ENTITY_ID_REPEATED_FIELD_MSG)
                    .setFieldPath(getFieldPath())
                    .build();
            addViolation(violation);
            return;
        }
        final V value = getValues().get(0);
        if (isValueNotSet(value)) {
            addViolation(newViolation(requiredOption));
        }
    }

    /**
     * Checks if the field is required and not set and adds violations found.
     *
     * <p>If the field is repeated, it must have at least one value set, and all its values must be valid.
     *
     * <p>It is required to override {@link #isValueNotSet(Object)} method to use this one.
     */
    protected void checkIfRequiredAndNotSet() {
        if (!requiredOption.getValue()) {
            return;
        }
        if (values.isEmpty()) {
            addViolation(newViolation(requiredOption));
            return;
        }
        for (V value : values) {
            if (isValueNotSet(value)) {
                addViolation(newViolation(requiredOption));
                return; // return because one error message is enough for the "required" option
            }
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
     * Adds a validation constraint validation to the collection of violations.
     *
     * @param violation a violation to add
     */
    protected void addViolation(ConstraintViolation violation) {
        violations.add(violation);
    }

    private ConstraintViolation newViolation(RequiredOption option) {
        final String msg = getErrorMsgFormat(option, option.getMsgFormat());
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .setFieldPath(getFieldPath())
                .build();
        return violation;
    }

    /**
     * Returns a validation error message (a custom one (if present) or the default one).
     *
     * @param option a validation option used to get the default message
     * @param customMsg a user-defined error message
     */
    protected String getErrorMsgFormat(Message option, String customMsg) {
        final String defaultMsg = option.getDescriptorForType().getOptions().getExtension(ValidationProto.defaultMessage);
        final String msg = customMsg.isEmpty() ? defaultMsg : customMsg;
        return msg;
    }

    /**
     * Returns a field validation option.
     *
     * @param extension an extension key used to obtain a validation option
     */
    protected final <Option> Option getFieldOption(GeneratedExtension<FieldOptions, Option> extension) {
        final Option option = fieldDescriptor.getOptions().getExtension(extension);
        return option;
    }

    /**
     * Returns {@code true} if the field must be an entity ID
     * (if the current Protobuf file is for entity commands and the field is the first in a command message);
     * {@code false} otherwise.
     */
    private boolean isRequiredEntityIdField() {
        final boolean result = isEntityFile && isCommandsFile && isFirstField;
        return result;
    }

    /**
     * Returns a path to the current field.
     */
    protected FieldPath getFieldPath() {
        return fieldPath;
    }
}
