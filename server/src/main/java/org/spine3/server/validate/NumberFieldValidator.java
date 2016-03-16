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
import org.spine3.validation.options.DigitsOption;
import org.spine3.validation.options.MaxOption;
import org.spine3.validation.options.MinOption;
import org.spine3.validation.options.ValidationProto;

import java.util.regex.Pattern;

import static java.lang.Math.abs;
import static java.lang.String.format;

/**
 * Validates fields of number types (protobuf: int32, double, etc).
 *
 * @author Alexander Litus
 */
/* package */ class NumberFieldValidator extends FieldValidator<Number> {

    private static final Pattern PATTERN_DOT = Pattern.compile("\\.");

    private final MinOption minOption;
    private final MaxOption maxOption;
    private final DigitsOption digitsOption;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues field values to validate
     */
    /* package */ NumberFieldValidator(FieldDescriptor descriptor, ImmutableList<Number> fieldValues) {
        super(descriptor, fieldValues);
        this.minOption = getOption(ValidationProto.min);
        this.maxOption = getOption(ValidationProto.max);
        this.digitsOption = getOption(ValidationProto.digits);
    }

    @Override
    protected void validate() {
        for (Number value : getValues()) {
            final double doubleValue = value.doubleValue();
            validateRangeOptions(doubleValue);
            validateDigitsOption(doubleValue);
        }
    }

    private void validateRangeOptions(double value) {
        if (rangeOptionsNotSet()) {
            return;
        }
        if (isNotFitToMin(value)) {
            setIsFieldInvalid(true);
            addErrorMessage(minOption, value);
        }
        if (isNotFitToMax(value)) {
            setIsFieldInvalid(true);
            addErrorMessage(maxOption, value);
        }
    }

    private boolean isNotFitToMin(double value) {
        if (minOption.getIgnore()) {
            return false;
        }
        final double min = minOption.getIs();
        final boolean isInclusive = minOption.getInclusive();
        final boolean fitsAndIsInclusive = isInclusive && (value >= min);
        final boolean fitsAndNonInclusive = !isInclusive && (value > min);
        final boolean isNotFit = !fitsAndIsInclusive && !fitsAndNonInclusive;
        return isNotFit;
    }

    private boolean isNotFitToMax(double value) {
        if (maxOption.getIgnore()) {
            return false;
        }
        final double max = maxOption.getIs();
        final boolean isInclusive = maxOption.getInclusive();
        final boolean fitsAndIsInclusive = isInclusive && (value <= max);
        final boolean fitsAndNonInclusive = !isInclusive && (value < max);
        final boolean isNotFit = !fitsAndIsInclusive && !fitsAndNonInclusive;
        return isNotFit;
    }

    private boolean rangeOptionsNotSet() {
        final boolean minOptionIsDefault = (minOption.getIs() == 0.0) && !minOption.getInclusive();
        final boolean maxOptionIsDefault = (maxOption.getIs() == 0.0) && !maxOption.getInclusive();
        final boolean result = minOptionIsDefault && maxOptionIsDefault;
        return result;
    }

    private void validateDigitsOption(double value) {
        final int intDigitsMax = digitsOption.getIntegerMax();
        final int fractionDigitsMax = digitsOption.getFractionMax();
        if (intDigitsMax < 1 || fractionDigitsMax < 1) {
            return;
        }
        final String valueStr = String.valueOf(abs(value));
        final String[] parts = PATTERN_DOT.split(valueStr);
        final int intDigitsCount = parts[0].length();
        final int fractionDigitsCount = parts[1].length();
        final boolean isInvalid = (intDigitsCount > intDigitsMax) || (fractionDigitsCount > fractionDigitsMax);
        if (isInvalid) {
            setIsFieldInvalid(true);
            addErrorMessage(digitsOption, intDigitsCount, fractionDigitsCount);
        }
    }

    private void addErrorMessage(MinOption option, double value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = formatErrorMessage(format, value, option.getInclusive(), option.getIs());
        addErrorMessage(msg);
    }

    private void addErrorMessage(MaxOption option, double value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = formatErrorMessage(format, value, option.getInclusive(), option.getIs());
        addErrorMessage(msg);
    }

    private String formatErrorMessage(String format, double value, boolean inclusive, double minOrMax) {
        final String msg = format(format,
                getFieldDescriptor().getName(),
                inclusive ? "or equal to " : "",
                minOrMax,
                value);
        return msg;
    }

    private void addErrorMessage(DigitsOption option, int intDigitsCount, int fractionDigitsCount) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = format(format,
                getFieldDescriptor().getName(),
                option.getIntegerMax(),
                option.getFractionMax(),
                intDigitsCount,
                fractionDigitsCount);
        addErrorMessage(msg);
    }
}
