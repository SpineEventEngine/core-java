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
import org.spine3.base.FieldPath;
import org.spine3.validation.options.ConstraintViolation;
import org.spine3.validation.options.PatternOption;
import org.spine3.validation.options.ValidationProto;

import java.util.List;

import static org.spine3.protobuf.Messages.newStringValue;
import static org.spine3.protobuf.Messages.toAny;

/**
 * Validates fields of type {@link String}.
 *
 * @author Alexander Litus
 */
/* package */ class StringFieldValidator extends FieldValidator<String> {

    private final PatternOption patternOption;
    private final String regex;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    /* package */ StringFieldValidator(FieldDescriptor descriptor, ImmutableList<String> fieldValues, FieldPath rootFieldPath) {
        super(descriptor, fieldValues, rootFieldPath);
        this.patternOption = getFieldOption(ValidationProto.pattern);
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
        final String msg = getErrorMessage(patternOption, patternOption.getMsg());
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                .setMessage(msg)
                .addFormatParam(regex)
                .setFieldPath(getFieldPath())
                .setFieldValue(toAny(newStringValue(fieldValue)))
                .build();
        return violation;
    }

    @Override
    @SuppressWarnings("RefusedBequest") // the base method call is redundant
    protected boolean isValueNotSet(String value) {
        final boolean isNotSet = value.isEmpty();
        return isNotSet;
    }
}
