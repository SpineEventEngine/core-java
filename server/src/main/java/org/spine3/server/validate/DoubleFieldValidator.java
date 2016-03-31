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

import static java.lang.Math.abs;
import static org.spine3.protobuf.Messages.newDoubleValue;
import static org.spine3.protobuf.Messages.toAny;

/**
 * Validates fields of {@link Double} types.
 *
 * @author Alexander Litus
 */
/* package */ class DoubleFieldValidator extends FloatFieldValidatorBase<Double> {

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues field values to validate
     */
    /* package */ DoubleFieldValidator(FieldDescriptor descriptor, ImmutableList<Double> fieldValues) {
        super(descriptor, fieldValues);
    }

    @Override
    protected Double toNumber(String value) {
        final Double min = Double.valueOf(value);
        return min;
    }

    @Override
    protected Double getAbs(Double value) {
        final Double abs = abs(value);
        return abs;
    }

    @Override
    protected Any wrapToMsgAndAny(Double value) {
        final Any any = toAny(newDoubleValue(value));
        return any;
    }
}
