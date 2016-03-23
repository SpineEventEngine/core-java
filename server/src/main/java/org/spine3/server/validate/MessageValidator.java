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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import java.util.List;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

/**
 * Validates messages according to Spine custom protobuf options and provides validation error messages.
 *
 * @author Alexander Litus
 */
public class MessageValidator {

    private final FieldValidatorFactory fieldValidatorFactory;

    private boolean isMessageInvalid = false;
    private boolean isValidated = false;
    private String errorMessage = "";

    /**
     * Creates a new validator instance.
     *
     * @param validatorFactory a field validator factory to use
     */
    public MessageValidator(FieldValidatorFactory validatorFactory) {
        this.fieldValidatorFactory = validatorFactory;
    }

    /**
     * Validates messages according to Spine custom protobuf options and sets validation error messages.
     *
     * @param message a message to validate
     */
    public void validate(Message message) {
        final List<String> errorMessages = newLinkedList();
        final Descriptor msgDescriptor = message.getDescriptorForType();
        final List<FieldDescriptor> fields = msgDescriptor.getFields();
        for (FieldDescriptor field : fields) {
            final Object value = message.getField(field);
            final FieldValidator<?> validator = fieldValidatorFactory.create(field, value);
            validator.validate();
            if (validator.isFieldInvalid()) {
                isMessageInvalid = true;
                final List<String> messages = validator.getErrorMessages();
                errorMessages.addAll(messages);
            }
        }
        if (isMessageInvalid) {
            errorMessage = buildErrorMessage(errorMessages, msgDescriptor);
        }
        isValidated = true;
    }

    /**
     * Returns false if the validated {@link Message} is invalid.
     * {@link #validate(Message)} method must be called first.
     */
    public boolean isMessageInvalid() {
        checkValidated();
        return isMessageInvalid;
    }

    /**
     * Returns a validation error message constructed from error messages for different fields.
     */
    public String getErrorMessage() {
        checkValidated();
        return errorMessage;
    }

    @VisibleForTesting
    /* package */ static String buildErrorMessage(List<String> messages, Descriptor msgDescriptor) {
        final int averageMsgLength = 32;
        final StringBuilder builder = new StringBuilder(messages.size() * averageMsgLength);
        builder.append(format("Message %s is invalid: ", msgDescriptor.getFullName()));
        for (int i = 0; i < messages.size() ; i++) {
            final String msg = messages.get(i);
            builder.append(msg);
            final boolean isLast = i == (messages.size() - 1);
            if (isLast) {
                builder.append('.');
            } else {
                builder.append("; ");
            }
        }
        return builder.toString();
    }

    private void checkValidated() {
        if (!isValidated) {
            throw new IllegalStateException("'validate()' method was not called.");
        }
    }
}
