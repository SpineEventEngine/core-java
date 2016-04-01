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

/**
 * A base for floating point number field validators.
 *
 * @author Alexander Litus
 */
/* package */ abstract class FloatFieldValidatorBase<V extends Number & Comparable<V>> extends NumberFieldValidator<V> {

    private static final String INVALID_ID_TYPE_MSG = "Entity ID field must not be a floating point number.";

    /**
     * Creates a new validator instance.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValues values to validate
     * @param rootFieldPath a path to the root field (if present)
     */
    protected FloatFieldValidatorBase(FieldDescriptor descriptor, ImmutableList<V> fieldValues, FieldPath rootFieldPath) {
        super(descriptor, fieldValues, rootFieldPath);
    }

    @Override
    @SuppressWarnings("RefusedBequest")
    protected void validateEntityId() {
        final V value = getValues().get(0);
        final ConstraintViolation violation = ConstraintViolation.newBuilder()
                .setMessage(INVALID_ID_TYPE_MSG)
                .setFieldPath(getFieldPath())
                .setFieldValue(wrap(value))
                .build();
        addViolation(violation);
    }
}
