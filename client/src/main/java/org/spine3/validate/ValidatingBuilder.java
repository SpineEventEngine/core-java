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

package org.spine3.validate;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import static org.spine3.util.Reflection.getGenericParameterType;

/**
 * An interface for all validating builders.
 *
 * <p>Validating builder is used to validate messages according
 * to the business rules during the {@code Message} creation.
 *
 * @param <T> the type of the message to build
 * @param <B> the type of the message builder
 * @author Illia Shepilov
 * @author Alex Tymchenko
 */
public interface ValidatingBuilder<T extends Message, B extends Message.Builder> {

    /**
     * Validates the field according to the protocol buffer message declaration.
     *
     * @param descriptor the {@code FieldDescriptor} of the field
     * @param fieldValue the value of the field
     * @param fieldName  the name of the field
     * @param <V>        the type of the field value
     * @throws ConstraintViolationThrowable if there are any constraint violations
     */
    <V> void validate(FieldDescriptor descriptor,
                      V fieldValue,
                      String fieldName) throws ConstraintViolationThrowable;

    /**
     * Validates and builds {@code Message}.
     *
     * @return the {@code Message} instance
     * @throws ConstraintViolationThrowable if there are any constraint violations
     */
    T build() throws ConstraintViolationThrowable;

    /**
     * Creates an instance of original {@code Message.Builder} for the {@code Message} being built.
     *
     * @return the {@code Builder} for the {@code Message} built.
     */
    B newOriginalBuilder();

    Class<T> getMessageClass();

    ValidatingBuilder<T, B> mergeFrom(T message);

    void clear();

    /**
     * Enumeration of generic type parameters of this interface.
     */
    enum GenericParameter {

        /**
         * The index of the declaration of the generic parameter type {@code <T>}
         * in {@link ValidatingBuilder}.
         */
        MESSAGE(0),

        /**
         * The index of the declaration of the generic parameter type {@code <B>}
         * in {@link ValidatingBuilder}
         */
        MESSAGE_BUILDER(1);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }
    }

    /**
     * Provides type information on classes implementing {@link ValidatingBuilder}.
     */
    class TypeInfo {

        private TypeInfo() {
            // Prevent construction from outside.
        }

        /**
         * Retrieves the state class of the passed entity class.
         *
         * @param builderClass the builder class to inspect
         * @param <T>          the state type
         * @return the entity state class
         */
        public static <T extends Message> Class<T> getMessageClass(
                Class<? extends ValidatingBuilder> builderClass) {
            final Class<T> result =
                    getGenericParameterType(builderClass,
                                            GenericParameter.MESSAGE.getIndex());
            return result;
        }
    }
}
