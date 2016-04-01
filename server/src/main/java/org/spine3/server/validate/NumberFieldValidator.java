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
import com.google.protobuf.Message;
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

    private final DecimalMinOption minDecimalOpt;
    private final boolean isMinDecimalInclusive;

    private final DecimalMaxOption maxDecimalOpt;
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
        this.minDecimalOpt = getFieldOption(ValidationProto.decimalMin);
        this.isMinDecimalInclusive = minDecimalOpt.getInclusive();
        this.maxDecimalOpt = getFieldOption(ValidationProto.decimalMax);
        this.isMaxDecimalInclusive = maxDecimalOpt.getInclusive();
        this.minOption = getFieldOption(ValidationProto.min);
        this.maxOption = getFieldOption(ValidationProto.max);
        this.digitsOption = getFieldOption(ValidationProto.digits);
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
    protected boolean isValueNotSet(V value) {
        final int intValue = value.intValue();
        final boolean isNotSet = intValue == 0;
        return isNotSet;
    }

    private void validateRangeOptions(V value) {
        if (notFitToDecimalMin(value)) {
            addViolation(
                    newDecimalViolation(value, minDecimalOpt, minDecimalOpt.getMsg(),
                            minDecimalOpt.getInclusive(), minDecimalOpt.getValue()));
        }
        if (notFitToDecimalMax(value)) {
            addViolation(
                    newDecimalViolation(value, maxDecimalOpt, maxDecimalOpt.getMsg(),
                            maxDecimalOpt.getInclusive(), maxDecimalOpt.getValue()));
        }
        if (notFitToMin(value)) {
            addViolation(newMinOrMaxViolation(value, minOption, minOption.getMsg(), minOption.getValue()));
        }
        if (notFitToMax(value)) {
            addViolation(newMinOrMaxViolation(value, maxOption, maxOption.getMsg(), maxOption.getValue()));
        }
    }

    private boolean notFitToDecimalMin(V value) {
        final String minAsString = minDecimalOpt.getValue();
        if (minAsString.isEmpty()) {
            return false;
        }
        final V min = toNumber(minAsString);
        final int comparisonResult = value.compareTo(min);
        final boolean fits = isMinDecimalInclusive ?
                             comparisonResult >= 0 :
                             comparisonResult > 0;
        final boolean notFit = !fits;
        return notFit;
    }

    private boolean notFitToDecimalMax(V value) {
        final String maxAsString = maxDecimalOpt.getValue();
        if (maxAsString.isEmpty()) {
            return false;
        }
        final V max = toNumber(maxAsString);
        final boolean fits = isMaxDecimalInclusive ?
                             value.compareTo(max) <= 0 :
                             value.compareTo(max) < 0;
        final boolean notFit = !fits;
        return notFit;
    }

    private boolean notFitToMin(V value) {
        final String minAsString = minOption.getValue();
        if (minAsString.isEmpty()) {
            return false;
        }
        final V min = toNumber(minAsString);
        final boolean isGreaterThanOrEqualToMin = value.compareTo(min) >= 0;
        final boolean notFits = !isGreaterThanOrEqualToMin;
        return notFits;
    }

    private boolean notFitToMax(V value) {
        final String maxAsString = maxOption.getValue();
        if (maxAsString.isEmpty()) {
            return false;
        }
        final V max = toNumber(maxAsString);
        final boolean isLessThanOrEqualToMax = value.compareTo(max) <= 0;
        final boolean notFit = !isLessThanOrEqualToMax;
        return notFit;
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

    private ConstraintViolation newDecimalViolation(V value,
                                                    Message option,
                                                    String customMsg,
                                                    boolean isInclusive,
                                                    String minOrMax) {
        final String msg = getErrorMsgFormat(option, customMsg);
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(isInclusive ? "or equal to " : "")
                .addParam(minOrMax)
                .setFieldPath(getFieldPath())
                .setFieldValue(wrap(value));
        return violation.build();
    }

    private ConstraintViolation newMinOrMaxViolation(V value, Message option, String customMsg, String minOrMax) {
        final String msg = getErrorMsgFormat(option, customMsg);
        final ConstraintViolation.Builder violation = ConstraintViolation.newBuilder()
                .setMsgFormat(msg)
                .addParam(minOrMax)
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
