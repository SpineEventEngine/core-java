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
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import org.spine3.base.FieldPath;

import java.util.List;

import static java.lang.String.format;

/**
 * Creates {@link FieldValidator}s.
 *
 * @author Alexander Litus
 */
/* package */ class FieldValidatorFactory {

    private FieldValidatorFactory() {
    }

    /**
     * Creates a new validator instance according to the field type and validates the field.
     *
     * @param descriptor    a descriptor of the field to validate
     * @param fieldValue    a value of the field to validate
     * @param rootFieldPath a path to the root field
     * @param strict        if {@code true} validators would always assume that the field is required
     */
    private static FieldValidator<?> create(FieldDescriptor descriptor,
            Object fieldValue,
            FieldPath rootFieldPath,
            boolean strict) {
        final JavaType fieldType = descriptor.getJavaType();
        switch (fieldType) {
            case MESSAGE:
                final ImmutableList<Message> messages = toValueList(fieldValue);
                return new MessageFieldValidator(descriptor, messages, rootFieldPath, strict);
            case INT:
                final ImmutableList<Integer> ints = toValueList(fieldValue);
                return new IntegerFieldValidator(descriptor, ints, rootFieldPath);
            case LONG:
                final ImmutableList<Long> longs = toValueList(fieldValue);
                return new LongFieldValidator(descriptor, longs, rootFieldPath);
            case FLOAT:
                final ImmutableList<Float> floats = toValueList(fieldValue);
                return new FloatFieldValidator(descriptor, floats, rootFieldPath);
            case DOUBLE:
                final ImmutableList<Double> doubles = toValueList(fieldValue);
                return new DoubleFieldValidator(descriptor, doubles, rootFieldPath);
            case STRING:
                final ImmutableList<String> strings = toValueList(fieldValue);
                return new StringFieldValidator(descriptor, strings, rootFieldPath, strict);
            case BYTE_STRING:
                final ImmutableList<ByteString> byteStrings = toValueList(fieldValue);
                return new ByteStringFieldValidator(descriptor, byteStrings, rootFieldPath);
            case BOOLEAN:
                final ImmutableList<Boolean> booleans = toValueList(fieldValue);
                return new BooleanFieldValidator(descriptor, booleans, rootFieldPath);
            case ENUM:
                final ImmutableList<Descriptors.EnumValueDescriptor> enums = toValueList(fieldValue);
                return new EnumFieldValidator(descriptor, enums, rootFieldPath);
            default:
                throw fieldTypeIsNotSupported(descriptor);
        }
    }

    /* package */
    static FieldValidator<?> create(FieldDescriptor descriptor,
            Object fieldValue,
            FieldPath rootFieldPath) {
        return create(descriptor, fieldValue, rootFieldPath, false);
    }

    /* package */
    static FieldValidator<?> createStrict(FieldDescriptor descriptor,
            Object fieldValue,
            FieldPath rootFieldPath) {
        return create(descriptor, fieldValue, rootFieldPath, true);
    }

    @SuppressWarnings({"unchecked", "IfMayBeConditional"})
    private static <T> ImmutableList<T> toValueList(Object fieldValue) {
        if (fieldValue instanceof List) {
            return ImmutableList.copyOf((List<T>) fieldValue);
        } else {
            return ImmutableList.of((T) fieldValue);
        }
    }

    private static IllegalArgumentException fieldTypeIsNotSupported(FieldDescriptor descriptor) {
        final String msg = format("The field type is not supported for validation: %s", descriptor.getType());
        throw new IllegalArgumentException(msg);
    }
}
