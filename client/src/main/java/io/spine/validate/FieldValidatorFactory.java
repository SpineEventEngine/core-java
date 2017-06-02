/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.validate;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import io.spine.base.FieldPath;

import static java.lang.String.format;

/**
 * Creates {@link FieldValidator}s.
 *
 * @author Alexander Litus
 */
class FieldValidatorFactory {

    private FieldValidatorFactory() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a new validator instance according to the field type and validates the field.
     *
     * @param descriptor    a descriptor of the field to validate
     * @param fieldValue    a value of the field to validate
     * @param rootFieldPath a path to the root field
     * @param strict        if {@code true} validators would always assume that the field is
     *                      required
     */
    @SuppressWarnings("OverlyComplexMethod")
        // OK since an alternative map-based impl. would be a lot more code that would do the same.
    private static FieldValidator<?> create(FieldDescriptor descriptor,
                                            Object fieldValue,
                                            FieldPath rootFieldPath,
                                            boolean strict) {
        final JavaType fieldType = descriptor.getJavaType();
        switch (fieldType) {
            case MESSAGE:
                return new MessageFieldValidator(descriptor, fieldValue, strict, rootFieldPath);
            case INT:
                return new IntegerFieldValidator(descriptor, fieldValue, rootFieldPath);
            case LONG:
                return new LongFieldValidator(descriptor, fieldValue, rootFieldPath);
            case FLOAT:
                return new FloatFieldValidator(descriptor, fieldValue, rootFieldPath);
            case DOUBLE:
                return new DoubleFieldValidator(descriptor, fieldValue, rootFieldPath);
            case STRING:
                return new StringFieldValidator(descriptor, fieldValue, rootFieldPath, strict);
            case BYTE_STRING:
                return new ByteStringFieldValidator(descriptor, fieldValue, rootFieldPath);
            case BOOLEAN:
                return new BooleanFieldValidator(descriptor, fieldValue, rootFieldPath);
            case ENUM:
                return new EnumFieldValidator(descriptor, fieldValue, rootFieldPath);
            default:
                throw fieldTypeIsNotSupported(descriptor);
        }
    }

    static FieldValidator<?> create(FieldDescriptor descriptor,
                                    Object fieldValue,
                                    FieldPath rootFieldPath) {
        return create(descriptor, fieldValue, rootFieldPath, false);
    }

    static FieldValidator<?> createStrict(FieldDescriptor descriptor,
                                          Object fieldValue,
                                          FieldPath rootFieldPath) {
        return create(descriptor, fieldValue, rootFieldPath, true);
    }

    private static IllegalArgumentException fieldTypeIsNotSupported(FieldDescriptor descriptor) {
        final String msg = format("The field type is not supported for validation: %s",
                                  descriptor.getType());
        throw new IllegalArgumentException(msg);
    }
}
