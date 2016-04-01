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
import org.spine3.base.FieldPath;
import org.spine3.validation.options.ConstraintViolation;
import org.spine3.validation.options.Time;
import org.spine3.validation.options.TimeOption;
import org.spine3.validation.options.ValidOption;
import org.spine3.validation.options.ValidationProto;

import java.util.List;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.protobuf.Timestamps.isAfter;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validation.options.Time.FUTURE;
import static org.spine3.validation.options.Time.UNDEFINED;

/**
 * Validates fields of type {@link Message}.
 *
 * @author Alexander Litus
 */
/* package */ class MessageFieldValidator extends FieldValidator<Message> {

    private final TimeOption timeOption;
    private final ValidOption validOption;
    private final boolean isFieldTimestamp;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    /* package */ MessageFieldValidator(FieldDescriptor descriptor, ImmutableList<Message> fieldValues, FieldPath rootFieldPath) {
        super(descriptor, fieldValues, rootFieldPath);
        this.timeOption = getFieldOption(ValidationProto.when);
        this.validOption = getFieldOption(ValidationProto.valid);
        this.isFieldTimestamp = isTimestamp();
    }

    @Override
    protected List<ConstraintViolation> validate() {
        checkIfRequiredAndNotSet();
        if (!getValues().isEmpty()) {
            validateFieldsOfMessageIfNeeded();
            if (isFieldTimestamp) {
                validateTimestamps();
            }
        }
        final List<ConstraintViolation> violations = super.validate();
        return violations;
    }

    @Override
    @SuppressWarnings("RefusedBequest") // the base method call is redundant
    protected boolean isValueNotSet(Message value) {
        final boolean isNotSet = isDefault(value);
        return isNotSet;
    }

    private void validateFieldsOfMessageIfNeeded() {
        if (!validOption.getValue()) {
            return;
        }
        for (Message value : getValues()) {
            final MessageValidator validator = new MessageValidator(getFieldPath());
            final List<ConstraintViolation> violations = validator.validate(value);
            if (!violations.isEmpty()) {
                addViolation(newValidViolation(value, violations));
            }
        }
    }

    private boolean isTimestamp() {
        final ImmutableList<Message> values = getValues();
        final Message value = values.isEmpty() ? null : values.get(0);
        final boolean isTimestamp = value instanceof Timestamp;
        return isTimestamp;
    }

    private void validateTimestamps() {
        final Time when = timeOption.getIn();
        if (when == UNDEFINED) {
            return;
        }
        final Timestamp now = getCurrentTime();
        for (Message value : getValues()) {
            final Timestamp time = (Timestamp) value;
            if (isTimeInvalid(time, when, now)) {
                addViolation(newTimeViolation(time));
                return; // return because one error message is enough for the "time" option
            }
        }
    }

    private static boolean isTimeInvalid(Timestamp time, Time when, Timestamp now) {
        final boolean isValid = (when == FUTURE) ?
                                isAfter(time, /*than*/ now) :
                                isAfter(now, /*than*/ time);
        final boolean isInvalid = !isValid;
        return isInvalid;
    }

    private ConstraintViolation newTimeViolation(Timestamp fieldValue) {
        final String msg = getErrorMsgFormat(timeOption, timeOption.getMsg());
        final String when = timeOption.getIn().toString().toLowerCase();
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(when)
                .setFieldPath(getFieldPath())
                .setFieldValue(toAny(fieldValue))
                .build();
        return violation;
    }

    private ConstraintViolation newValidViolation(Message fieldValue, List<ConstraintViolation> violations) {
        final String msg = getErrorMsgFormat(validOption, validOption.getMsg());
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .setFieldPath(getFieldPath())
                .setFieldValue(toAny(fieldValue))
                .addAllViolation(violations)
                .build();
        return violation;
    }
}
