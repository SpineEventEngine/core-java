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
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;

import java.util.List;

import static java.lang.String.format;

/**
 * Creates {@link FieldValidator}s.
 *
 * @author Alexander Litus
 */
/* package */ class FieldValidatorFactory {

    /**
     * Creates a new factory instance.
     */
    /* package */ static FieldValidatorFactory newInstance() {
        return new FieldValidatorFactory();
    }

    /**
     * Creates a new validator instance according to the field type and validates the field.
     *
     * @param descriptor a descriptor of the field to validate
     * @param fieldValue a value of the field to validate
     */
    /* package */ FieldValidator<?> create(FieldDescriptor descriptor, Object fieldValue) {
        final FieldValidator<?> validator;
        final JavaType fieldType = descriptor.getJavaType();
        switch (fieldType) {
            case MESSAGE:
                final ImmutableList<Message> messages = toValueList(fieldValue);
                validator = new MessageFieldValidator(descriptor, messages);
                break;
            case INT: // TODO:2016-03-30:alexander.litus: add int and float validators for correct value wrapping
            case LONG:
                final ImmutableList<Long> longs = toValueList(fieldValue);
                validator = new LongFieldValidator(descriptor, longs);
                break;
            case FLOAT:
            case DOUBLE:
                final ImmutableList<Double> doubles = toValueList(fieldValue);
                validator = new DoubleFieldValidator(descriptor, doubles);
                break;
            case STRING:
                final ImmutableList<String> strings = toValueList(fieldValue);
                validator = new StringFieldValidator(descriptor, strings);
                break;
            case BYTE_STRING:
                final ImmutableList<ByteString> byteStrings = toValueList(fieldValue);
                validator = new ByteStringFieldValidator(descriptor, byteStrings);
                break;
            case BOOLEAN:
            case ENUM:
            default:
                throw fieldTypeIsNotSupported(descriptor);
        }
        return validator;
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
