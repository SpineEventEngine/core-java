/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.FieldPath;
import org.spine3.protobuf.AnyPacker;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.internal.IfInvalidOption;
import org.spine3.validate.internal.Time;
import org.spine3.validate.internal.TimeOption;
import org.spine3.validate.internal.ValidationProto;

import java.util.List;

import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.protobuf.Timestamps.isLaterThan;
import static org.spine3.validate.Validate.isDefault;
import static org.spine3.validate.internal.Time.FUTURE;
import static org.spine3.validate.internal.Time.TIME_UNDEFINED;

/**
 * Validates fields of type {@link Message}.
 *
 * @author Alexander Litus
 */
class MessageFieldValidator extends FieldValidator<Message> {

    private final TimeOption timeOption;
    private final boolean validateOption;
    private final IfInvalidOption ifInvalidOption;
    private final boolean isFieldTimestamp;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor    a descriptor of the field to validate
     * @param fieldValues   values to validate
     * @param rootFieldPath a path to the root field (if present)
     * @param strict        if {@code true} the validator would assume that the field is required even
     *                      if the corresponding field option is not present
     */
    MessageFieldValidator(FieldDescriptor descriptor,
                          ImmutableList<Message> fieldValues,
                          FieldPath rootFieldPath, boolean strict) {
        super(descriptor, fieldValues, rootFieldPath, strict);
        this.timeOption = getFieldOption(ValidationProto.when);
        this.validateOption = getFieldOption(ValidationProto.valid);
        this.ifInvalidOption = getFieldOption(ValidationProto.ifInvalid);
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
    protected boolean isValueNotSet(Message value) {
        final boolean result = isDefault(value);
        return result;
    }

    private void validateFieldsOfMessageIfNeeded() {
        if (!validateOption) {
            if (hasCustomInvalidMessage()) {
                log().warn("'if_invalid' option is set without '(valid) = true'");
            }
            return;
        }
        for (Message value : getValues()) {
            final MessageValidator validator = MessageValidator.newInstance(getFieldPath());
            final List<ConstraintViolation> violations = validator.validate(value);
            if (!violations.isEmpty()) {
                addViolation(newValidViolation(value, violations));
            }
        }
    }

    /**
     * @return {@code true} if the option {@code 'if_invalid'} is set to a non-default value.
     */
    private boolean hasCustomInvalidMessage() {
        final boolean result = !isDefault(ifInvalidOption);
        return result;
    }

    private boolean isTimestamp() {
        final ImmutableList<Message> values = getValues();
        final Message value = values.isEmpty() ? null : values.get(0);
        final boolean isTimestamp = value instanceof Timestamp;
        return isTimestamp;
    }

    private void validateTimestamps() {
        final Time when = timeOption.getIn();
        if (when == TIME_UNDEFINED) {
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

    /**
     * Checks the time.
     *
     * @param timeToCheck  a timestamp to check
     * @param whenExpected the time when the checked timestamp should be
     * @param now          the current moment
     * @return {@code true} if the time is valid according to {@code whenExpected} parameter, {@code false} otherwise
     */
    private static boolean isTimeInvalid(Timestamp timeToCheck, Time whenExpected, Timestamp now) {
        final boolean isValid = (whenExpected == FUTURE)
                                ? isLaterThan(timeToCheck, /*than*/ now)
                                : isLaterThan(now, /*than*/ timeToCheck);
        final boolean isInvalid = !isValid;
        return isInvalid;
    }

    private ConstraintViolation newTimeViolation(Timestamp fieldValue) {
        final String msg = getErrorMsgFormat(timeOption, timeOption.getMsgFormat());
        final String when = timeOption.getIn()
                                      .toString()
                                      .toLowerCase();
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                                                                 .setMsgFormat(msg)
                                                                 .addParam(when)
                                                                 .setFieldPath(getFieldPath())
                                                                 .setFieldValue(AnyPacker.pack(fieldValue))
                                                                 .build();
        return violation;
    }

    private ConstraintViolation newValidViolation(Message fieldValue, Iterable<ConstraintViolation> violations) {
        final String msg = getErrorMsgFormat(ifInvalidOption, ifInvalidOption.getMsgFormat());
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                                                                 .setMsgFormat(msg)
                                                                 .setFieldPath(getFieldPath())
                                                                 .setFieldValue(AnyPacker.pack(fieldValue))
                                                                 .addAllViolation(violations)
                                                                 .build();
        return violation;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(MessageFieldValidator.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
