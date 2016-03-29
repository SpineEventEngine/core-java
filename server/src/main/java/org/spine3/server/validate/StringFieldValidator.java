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
import org.spine3.validation.options.PatternOption;
import org.spine3.validation.options.ValidationProto;

import static java.lang.String.format;

/**
 * Validates fields of type {@link String}.
 *
 * @author Alexander Litus
 */
/* package */ class StringFieldValidator extends FieldValidator<String> {

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues field values to validate
     */
    /* package */ StringFieldValidator(FieldDescriptor descriptor, ImmutableList<String> fieldValues) {
        super(descriptor, fieldValues);
    }

    @Override
    protected void validate() {
        checkIfRequiredAndNotSet();
        checkIfMatchesToRegexp();
        if (isRequiredEntityIdField()) {
            validateEntityId();
        }
    }

    private void checkIfMatchesToRegexp() {
        final PatternOption option = getFieldOption(ValidationProto.pattern);
        final String regex = option.getRegex();
        if (regex.isEmpty()) {
            return;
        }
        for (String value : getValues()) {
            if (!value.matches(regex)) {
                assertFieldIsInvalid();
                addErrorMessage(option, value);
            }
        }
    }

    private void addErrorMessage(PatternOption option, String value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String fieldName = getFieldName();
        final String regex = option.getRegex();
        final String msg = format(format, fieldName, regex, value);
        addErrorMessage(msg);
    }

    @Override
    @SuppressWarnings("RefusedBequest") // the base method call is redundant
    protected boolean isValueNotSet(String value) {
        final boolean isNotSet = value.isEmpty();
        return isNotSet;
    }
}
