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

import com.google.protobuf.Descriptors.FieldDescriptor;
import org.spine3.validation.options.DigitsOption;
import org.spine3.validation.options.MaxOption;
import org.spine3.validation.options.MinOption;
import org.spine3.validation.options.ValidationProto;

import java.util.regex.Pattern;

import static java.lang.Math.abs;

/**
 * Validates fields of number types (protobuf: int32, double, etc).
 *
 * @author Alexander Litus
 */
/* package */ class NumberFieldValidator extends FieldValidator {

    private static final Pattern PATTERN_DOT = Pattern.compile("\\.");

    private final double value;
    private final MinOption minOption;
    private final MaxOption maxOption;
    private final DigitsOption digitsOption;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValue a value of the field to validate
     */
    /* package */ NumberFieldValidator(FieldDescriptor descriptor, Number fieldValue) {
        super(descriptor);
        this.value = fieldValue.doubleValue();
        this.minOption = getOption(ValidationProto.min);
        this.maxOption = getOption(ValidationProto.max);
        this.digitsOption = getOption(ValidationProto.digits);
    }

    @Override
    protected void validate() {
        validateRangeOptions();
        validateDigitsOption();
    }

    private void validateRangeOptions() {
        if (rangeOptionsNotSet()) {
            return;
        }
        if (isNotFitToMin()) {
            setIsFieldInvalid(true);
            addErrorMessage(minOption, minOption.getMsg());
        }
        if (isNotFitToMax()) {
            setIsFieldInvalid(true);
            addErrorMessage(maxOption, maxOption.getMsg());
        }
    }

    private boolean isNotFitToMin() {
        if (minOption.getIgnore()) {
            return false;
        }
        final double min = minOption.getIs();
        final boolean isInclusive = minOption.getInclusive();
        final boolean fitsAndIsInclusive = isInclusive && value >= min;
        final boolean fitsAndNonInclusive = !isInclusive && value > min;
        final boolean isNotFit = !fitsAndIsInclusive && !fitsAndNonInclusive;
        return isNotFit;
    }

    private boolean isNotFitToMax() {
        if (maxOption.getIgnore()) {
            return false;
        }
        final double max = maxOption.getIs();
        final boolean isInclusive = maxOption.getInclusive();
        final boolean fitsAndIsInclusive = isInclusive && value <= max;
        final boolean fitsAndNonInclusive = !isInclusive && value < max;
        final boolean isNotFit = !fitsAndIsInclusive && !fitsAndNonInclusive;
        return isNotFit;
    }

    private boolean rangeOptionsNotSet() {
        final boolean minValuesAreDefault = (minOption.getIs() == 0.0) && !minOption.getInclusive();
        final boolean maxValuesAreDefault = (maxOption.getIs() == 0.0) && !maxOption.getInclusive();
        final boolean result = minValuesAreDefault && maxValuesAreDefault;
        return result;
    }

    private void validateDigitsOption() {
        final int intDigitsMax = digitsOption.getIntegerMax();
        final int fractionDigitsMax = digitsOption.getFractionMax();
        if (intDigitsMax < 1 || fractionDigitsMax < 1) {
            return;
        }
        final String valueStr = String.valueOf(abs(value));
        final String[] parts = PATTERN_DOT.split(valueStr);
        final int intDigitsCount = parts[0].length();
        final int fractionalDigitsCount = parts[1].length();

        final boolean isInvalid = (intDigitsCount > intDigitsMax) || (fractionalDigitsCount > fractionDigitsMax);
        if (isInvalid) {
            setIsFieldInvalid(true);
            addErrorMessage(digitsOption, digitsOption.getMsg());
        }
    }
}
