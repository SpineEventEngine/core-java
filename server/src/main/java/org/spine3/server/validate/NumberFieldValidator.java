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
import org.spine3.validation.options.DecimalMaxOption;
import org.spine3.validation.options.DecimalMinOption;
import org.spine3.validation.options.DigitsOption;
import org.spine3.validation.options.MaxOption;
import org.spine3.validation.options.MinOption;
import org.spine3.validation.options.ValidationProto;

import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Validates fields of number types (protobuf: int32, double, etc).
 *
 * @param <V> the type of the field value
 * @author Alexander Litus
 */
/* package */ abstract class NumberFieldValidator<V extends Number & Comparable<V>> extends FieldValidator<V> {

    private static final Pattern PATTERN_DOT = Pattern.compile("\\.");

    private final DecimalMinOption minDecimalOption;
    private final boolean isMinDecimalInclusive;

    private final DecimalMaxOption maxDecimalOption;
    private final boolean isMaxDecimalInclusive;

    private final MinOption minOption;
    private final MaxOption maxOption;

    private final DigitsOption digitsOption;

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues field values to validate
     */
    protected NumberFieldValidator(FieldDescriptor descriptor, ImmutableList<V> fieldValues) {
        super(descriptor, fieldValues);
        this.minDecimalOption = getFieldOption(ValidationProto.decimalMin);
        this.isMinDecimalInclusive = minDecimalOption.getInclusive();
        this.maxDecimalOption = getFieldOption(ValidationProto.decimalMax);
        this.isMaxDecimalInclusive = maxDecimalOption.getInclusive();
        this.minOption = getFieldOption(ValidationProto.min);
        this.maxOption = getFieldOption(ValidationProto.max);
        this.digitsOption = getFieldOption(ValidationProto.digits);
    }

    @Override
    protected void validate() {
        for (V value : getValues()) {
            validateRangeOptions(value);
            validateDigitsOption(value);
        }
    }

    @Override
    @SuppressWarnings("RefusedBequest")
    protected boolean isValueNotSet(V value) {
        final int intValue = value.intValue();
        final boolean isNotSet = intValue == 0;
        return isNotSet;
    }

    /**
     * Converts a string representation to a number.
     */
    protected abstract V toNumber(String value);

    /**
     * Returns an absolute value of the number.
     */
    protected abstract V getAbs(V number);

    private void validateRangeOptions(V value) {
        if (!fitsToOptionDecimalMin(value)) {
            assertFieldIsInvalid();
            addErrorMessage(minDecimalOption, value);
        }
        if (!fitsToOptionDecimalMax(value)) {
            assertFieldIsInvalid();
            addErrorMessage(maxDecimalOption, value);
        }
        if (!fitsToOptionMin(value)) {
            assertFieldIsInvalid();
            addErrorMessage(minOption, value);
        }
        if (!fitsToOptionMax(value)) {
            assertFieldIsInvalid();
            addErrorMessage(maxOption, value);
        }
    }

    private boolean fitsToOptionDecimalMin(V value) {
        final String minAsString = minDecimalOption.getValue();
        if (minAsString.isEmpty()) {
            return true;
        }
        final V min = toNumber(minAsString);
        final int comparisonResult = value.compareTo(min);
        final boolean fits = isMinDecimalInclusive ?
                             comparisonResult >= 0 :
                             comparisonResult > 0;
        return fits;
    }

    private boolean fitsToOptionDecimalMax(V value) {
        final String maxAsString = maxDecimalOption.getValue();
        if (maxAsString.isEmpty()) {
            return true;
        }
        final V max = toNumber(maxAsString);
        final boolean fits = isMaxDecimalInclusive ?
                             value.compareTo(max) <= 0 :
                             value.compareTo(max) < 0;
        return fits;
    }

    private boolean fitsToOptionMin(V value) {
        final String minAsString = minOption.getValue();
        if (minAsString.isEmpty()) {
            return true;
        }
        final V min = toNumber(minAsString);
        final boolean isGreaterThanOrEqualToMin = value.compareTo(min) >= 0;
        return isGreaterThanOrEqualToMin;
    }

    private boolean fitsToOptionMax(V value) {
        final String maxAsString = maxOption.getValue();
        if (maxAsString.isEmpty()) {
            return true;
        }
        final V max = toNumber(maxAsString);
        final boolean isLessThanOrEqualToMax = value.compareTo(max) <= 0;
        return isLessThanOrEqualToMax;
    }

    private void validateDigitsOption(V value) {
        final int intDigitsMax = digitsOption.getIntegerMax();
        final int fractionDigitsMax = digitsOption.getFractionMax();
        if (intDigitsMax < 1 || fractionDigitsMax < 1) {
            return;
        }
        final V abs = getAbs(value);
        final String valueStr = String.valueOf(abs);
        final String[] parts = PATTERN_DOT.split(valueStr);
        final int intDigitsCount = parts[0].length();
        final int fractionDigitsCount = parts[1].length();
        final boolean isInvalid = (intDigitsCount > intDigitsMax) || (fractionDigitsCount > fractionDigitsMax);
        if (isInvalid) {
            assertFieldIsInvalid();
            addErrorMessage(digitsOption, intDigitsCount, fractionDigitsCount);
        }
    }

    private void addErrorMessage(DecimalMinOption option, V value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = formatDecimalRangeErrorMessage(format, value, option.getInclusive(), option.getValue());
        addErrorMessage(msg);
    }

    private void addErrorMessage(DecimalMaxOption option, V value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = formatDecimalRangeErrorMessage(format, value, option.getInclusive(), option.getValue());
        addErrorMessage(msg);
    }

    private void addErrorMessage(MinOption option, V value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = formatRangeErrorMessage(format, value, option.getValue());
        addErrorMessage(msg);
    }

    private void addErrorMessage(MaxOption option, V value) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = formatRangeErrorMessage(format, value, option.getValue());
        addErrorMessage(msg);
    }

    private String formatDecimalRangeErrorMessage(String format, V value, boolean inclusive, String minOrMax) {
        final String msg = format(format,
                getFieldName(),
                inclusive ? "or equal to " : "",
                minOrMax,
                value);
        return msg;
    }

    private String formatRangeErrorMessage(String format, V value, String minOrMax) {
        final String msg = format(format,
                getFieldName(),
                minOrMax,
                value);
        return msg;
    }

    private void addErrorMessage(DigitsOption option, int intDigitsCount, int fractionDigitsCount) {
        final String format = getErrorMessageFormat(option, option.getMsg());
        final String msg = format(format,
                getFieldName(),
                option.getIntegerMax(),
                option.getFractionMax(),
                intDigitsCount,
                fractionDigitsCount);
        addErrorMessage(msg);
    }
}
