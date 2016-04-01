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
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int32Value;
import org.spine3.base.FieldPath;
import org.spine3.validation.options.ConstraintViolation;
import org.spine3.validation.options.DecimalMaxOption;
import org.spine3.validation.options.DecimalMinOption;
import org.spine3.validation.options.DigitsOption;
import org.spine3.validation.options.MaxOption;
import org.spine3.validation.options.MinOption;
import org.spine3.validation.options.ValidationProto;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates fields of number types (protobuf: int32, double, etc).
 *
 * @param <V> the type of the field value
 * @author Alexander Litus
 */
/* package */ abstract class NumberFieldValidator<V extends Number & Comparable<V>> extends FieldValidator<V> {

    private static final Pattern PATTERN_DOT = Pattern.compile("\\.");
    private static final String OR_EQUAL_TO = "or equal to ";

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
     * @param fieldValues values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    protected NumberFieldValidator(FieldDescriptor descriptor, ImmutableList<V> fieldValues, FieldPath rootFieldPath) {
        super(descriptor, fieldValues, rootFieldPath);
        this.minDecimalOption = getFieldOption(ValidationProto.decimalMin);
        this.isMinDecimalInclusive = minDecimalOption.getInclusive();
        this.maxDecimalOption = getFieldOption(ValidationProto.decimalMax);
        this.isMaxDecimalInclusive = maxDecimalOption.getInclusive();
        this.minOption = getFieldOption(ValidationProto.min);
        this.maxOption = getFieldOption(ValidationProto.max);
        this.digitsOption = getFieldOption(ValidationProto.digits);
    }

    @Override
    protected List<ConstraintViolation> validate() {
        for (V value : getValues()) {
            validateRangeOptions(value);
            validateDigitsOption(value);
        }
        final List<ConstraintViolation> violations = super.validate();
        return violations;
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

    /**
     * Wraps a value to a corresponding message wrapper ({@link DoubleValue}, {@link Int32Value}, etc) and {@link Any}.
     */
    protected abstract Any wrap(V value);

    private void validateRangeOptions(V value) {
        if (!fitsToOptionDecimalMin(value)) {
            addViolation(newDecimalMinViolation(value));
        }
        if (!fitsToOptionDecimalMax(value)) {
            addViolation(newDecimalMaxViolation(value));
        }
        if (!fitsToOptionMin(value)) {
            addViolation(newMinViolation(value));
        }
        if (!fitsToOptionMax(value)) {
            addViolation(newMaxViolation(value));
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
        final String[] parts = PATTERN_DOT.split(String.valueOf(abs));
        final int intDigitsCount = parts[0].length();
        final int fractionDigitsCount = parts[1].length();
        final boolean isInvalid = (intDigitsCount > intDigitsMax) || (fractionDigitsCount > fractionDigitsMax);
        if (isInvalid) {
            addViolation(newDigitsViolation(value));
        }
    }

    private ConstraintViolation newDecimalMinViolation(V value) {
        final String msg = getErrorMsgFormat(minDecimalOption, minDecimalOption.getMsg());
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(minDecimalOption.getInclusive() ? OR_EQUAL_TO : "")
                .addParam(minDecimalOption.getValue())
                .setFieldPath(getFieldPath())
                .setFieldValue(wrap(value));
        return violation.build();
    }

    private ConstraintViolation newDecimalMaxViolation(V value) {
        final String msg = getErrorMsgFormat(maxDecimalOption, maxDecimalOption.getMsg());
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                 .setMsgFormat(msg)
                 .addParam(maxDecimalOption.getInclusive() ? OR_EQUAL_TO : "")
                 .addParam(maxDecimalOption.getValue())
                                                                         .setFieldPath(getFieldPath())
                 .setFieldValue(wrap(value));
        return violation.build();
    }

    private ConstraintViolation newMinViolation(V value) {
        final String msg = getErrorMsgFormat(minOption, minOption.getMsg());
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(minOption.getValue())
                .setFieldPath(getFieldPath())
                .setFieldValue(wrap(value));
        return violation.build();
    }

    private ConstraintViolation newMaxViolation(V value) {
        final String msg = getErrorMsgFormat(maxOption, maxOption.getMsg());
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(maxOption.getValue())
                .setFieldPath(getFieldPath())
                .setFieldValue(wrap(value));
        return violation.build();
    }

    private ConstraintViolation newDigitsViolation(V value) {
        final String msg = getErrorMsgFormat(digitsOption, digitsOption.getMsg());
        final String intMax = String.valueOf(digitsOption.getIntegerMax());
        final String fractionMax = String.valueOf(digitsOption.getFractionMax());
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(intMax)
                .addParam(fractionMax)
                .setFieldPath(getFieldPath())
                .setFieldValue(wrap(value));
        return violation.build();
    }
}
