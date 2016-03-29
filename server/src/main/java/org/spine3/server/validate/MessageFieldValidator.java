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
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.validate.Validate;
import org.spine3.validation.options.Time;
import org.spine3.validation.options.TimeOption;
import org.spine3.validation.options.ValidOption;
import org.spine3.validation.options.ValidationProto;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static java.lang.String.format;
import static org.spine3.protobuf.Timestamps.isAfter;
import static org.spine3.validation.options.Time.*;

/**
 * Validates fields of type {@link Message}.
 *
 * @author Alexander Litus
 */
/* package */ class MessageFieldValidator extends FieldValidator<Message> {

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues field values to validate
     */
    /* package */ MessageFieldValidator(FieldDescriptor descriptor, ImmutableList<Message> fieldValues) {
        super(descriptor, fieldValues);
    }

    @Override
    protected void validate() {
        checkIfRequiredAndNotSet();
        if (!getValues().isEmpty()) {
            validateFieldsOfMessageIfNeeded();
            if (isTimestamp()) {
                validateTimestamps();
            }
        }
        if (isRequiredEntityIdField()) {
            validateEntityId();
        }
    }

    @Override
    @SuppressWarnings("RefusedBequest") // the base method call is redundant
    protected boolean isValueNotSet(Message value) {
        final boolean isNotSet = Validate.isDefault(value);
        return isNotSet;
    }

    private void validateFieldsOfMessageIfNeeded() {
        final ValidOption option = getFieldOption(ValidationProto.valid);
        if (!option.getValue()) {
            return;
        }
        for (Message value : getValues()) {
            final MessageValidator validator = new MessageValidator();
            validator.validate(value);
            if (validator.isMessageInvalid()) {
                assertFieldIsInvalid();
                final String errorMessage = validator.getErrorMessage();
                addErrorMessage(option, errorMessage);
            }
        }
    }

    private boolean isTimestamp() {
        final Message value = getValues().get(0);
        final boolean isTimestamp = value instanceof Timestamp;
        return isTimestamp;
    }

    private void validateTimestamps() {
        final TimeOption option = getFieldOption(ValidationProto.when);
        final Time when = option.getIn();
        if (when == UNDEFINED) {
            return;
        }
        final Timestamp now = getCurrentTime();
        for (Message value : getValues()) {
            if (isTimeInvalid((Timestamp) value, when, now)) {
                assertFieldIsInvalid();
                addErrorMessage(option);
                return; // return because one error message is enough for the "time" option
            }
        }
    }

    private static boolean isTimeInvalid(Timestamp time, Time when, Timestamp now) {
        final boolean mustBeInFutureButIsNot = (when == FUTURE) && !isAfter(time, /*than*/ now);
        final boolean mustBeInPastButIsNot = (when == PAST) && !isAfter(now, /*than*/ time);
        final boolean isInvalid = mustBeInFutureButIsNot || mustBeInPastButIsNot;
        return isInvalid;
    }

    private void addErrorMessage(TimeOption option) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String fieldName = getFieldName();
        final String when = option.getIn().toString().toLowerCase();
        final String msg = format(format, fieldName, when);
        addErrorMessage(msg);
    }

    private void addErrorMessage(ValidOption option, String errorMessageForProps) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String fieldName = getFieldName();
        final String msg = format(format, fieldName, errorMessageForProps);
        addErrorMessage(msg);
    }
}
