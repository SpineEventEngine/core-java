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

package io.spine.validate;

import com.google.protobuf.Descriptors.FieldDescriptor;
import io.spine.base.FieldPath;
import io.spine.option.OptionsProto;
import io.spine.option.PatternOption;
import io.spine.protobuf.Wrapper;

import java.util.List;

/**
 * Validates fields of type {@link String}.
 *
 * @author Alexander Litus
 */
class StringFieldValidator extends FieldValidator<String> {

    private final PatternOption patternOption;
    private final String regex;

    /**
     * Creates a new validator instance.
     * @param descriptor   a descriptor of the field to validate
     * @param fieldValues   values to validate
     * @param rootFieldPath a path to the root field (if present)
     * @param strict        if {@code true} the validator would assume that the field is required
     *                      even if the corresponding option is not set
     */
    StringFieldValidator(FieldDescriptor descriptor,
                         Object fieldValues,
                         FieldPath rootFieldPath,
                         boolean strict) {
        super(descriptor, FieldValidator.<String>toValueList(fieldValues), rootFieldPath, strict);
        this.patternOption = getFieldOption(OptionsProto.pattern);
        this.regex = patternOption.getRegex();
    }

    @Override
    protected List<ConstraintViolation> validate() {
        checkIfRequiredAndNotSet();
        checkIfMatchesToRegexp();
        final List<ConstraintViolation> violations = super.validate();
        return violations;
    }

    private void checkIfMatchesToRegexp() {
        if (regex.isEmpty()) {
            return;
        }
        for (String value : getValues()) {
            if (!value.matches(regex)) {
                addViolation(newViolation(value));
            }
        }
    }

    private ConstraintViolation newViolation(String fieldValue) {
        final String msg = getErrorMsgFormat(patternOption, patternOption.getMsgFormat());
        final ConstraintViolation violation =
                ConstraintViolation.newBuilder()
                                   .setMsgFormat(msg)
                                   .addParam(regex)
                                   .setFieldPath(getFieldPath())
                                   .setFieldValue(Wrapper.forString()
                                                         .pack(fieldValue))
                                   .build();
        return violation;
    }

    @Override
    protected boolean isValueNotSet(String value) {
        final boolean result = value.isEmpty();
        return result;
    }
}
