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
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.protobuf.Messages;
import io.spine.util.GenericTypeIndex;

import java.lang.reflect.Method;

import static io.spine.util.Exceptions.illegalArgumentWithCauseOf;
import static io.spine.validate.ValidatingBuilder.GenericParameter.MESSAGE;

/**
 * An interface for all validating builders.
 *
 * <p>Validating builder is used to validate messages according to their Protobuf definition
 * during the {@code Message} creation.
 *
 * <p>Non-abstract implementations must declare {@code public static TYPE newBuilder()} method,
 * returning an instance of the implementation class.
 *
 * @param <T> the type of the message to build
 * @param <B> the type of the message builder
 * @author Illia Shepilov
 * @author Alex Tymchenko
 */
public interface ValidatingBuilder<T extends Message, B extends Message.Builder> {

    /**
     * Validates the field according to the Protobuf message declaration.
     *
     * @param descriptor the {@code FieldDescriptor} of the field
     * @param fieldValue the value of the field
     * @param fieldName  the name of the field
     * @param <V>        the type of the field value
     * @throws ValidationException if there are any constraint violations
     */
    <V> void validate(FieldDescriptor descriptor,
                      V fieldValue,
                      String fieldName) throws ValidationException;

    /**
     * Validates and builds {@code Message}.
     *
     * @return the {@code Message} instance
     * @throws ValidationException if there are any constraint violations
     */
    T build() throws ValidationException;

    /**
     * Merges the message currently built with the fields of a given {@code message}.
     *
     * @param message the message to merge into the state this builder
     * @return the builder instance
     */
    ValidatingBuilder<T, B> mergeFrom(T message);

    /**
     * Sets an original state for this builder to allow building the new value on top of some
     * pre-defined value.
     *
     * <p>The validation of the passed {@code state} is NOT performed.
     *
     * @param state the new state
     */
    @Internal
    void setOriginalState(T state);

    /**
     * Determines if the current message state has been modified comparing to its original state.
     *
     * @return {@code true} if it is modified, {@code false} otherwise
     */
    @Internal
    boolean isDirty();

    /**
     * Clears the state of this builder.
     *
     * <p>In particular, the state of the message being built is reset to the default.
     * Also the original state (if it {@linkplain #setOriginalState(Message) has been set}
     * previously) is cleared.
     */
    @Internal
    void clear();

    /**
     * Enumeration of generic type parameters of this interface.
     */
    enum GenericParameter implements GenericTypeIndex<ValidatingBuilder> {

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

        @Override
        public int getIndex() {
            return this.index;
        }

        @Override
        public Class<?> getArgumentIn(Class<? extends ValidatingBuilder> cls) {
            return Default.getArgument(this, cls);
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
            @SuppressWarnings("unchecked") // The type is ensured by the class declaration.
            final Class<T> result = (Class<T>)MESSAGE.getArgumentIn(builderClass);
            return result;
        }

        // as the method names are the same, but methods are different.
        public static Method getNewBuilderMethod(Class<? extends ValidatingBuilder<?, ?>> cls) {
            try {
                return cls.getMethod(Messages.METHOD_NEW_BUILDER);
            } catch (NoSuchMethodException e) {
                throw illegalArgumentWithCauseOf(e);
            }
        }
    }
}
