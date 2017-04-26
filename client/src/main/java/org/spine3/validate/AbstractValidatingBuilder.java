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

import com.google.common.base.Optional;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.spine3.annotations.Internal;
import org.spine3.base.ConversionException;
import org.spine3.base.FieldPath;
import org.spine3.string.Stringifiers;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;

/**
 * Serves as an abstract base for all validating builders.
 *
 * @author Illia Shepilov
 * @see ValidatingBuilder
 */
public abstract class AbstractValidatingBuilder<T extends Message> implements ValidatingBuilder<T> {

    /**
     * The state of the message, serving as a base value for this {@code ValidatingBuilder}.
     *
     * <p>Used to verify if any modifications were made by a user via {@code ValidatingBuilder}
     * public API calls.
     *
     * <p>Has {@code null} value if not set via {@linkplain #setOriginalState(Message)
     * setOriginalState(..)}.
     */
    @Nullable
    private T originalState;

    /**
     * Builds a message without triggering its validation.
     *
     * @return the message built from the values set by the user
     */
    protected abstract T internalBuild();

    @Override
    public T build() throws ConstraintViolationThrowable {
        final T message = internalBuild();
        validateResult(message);
        return message;
    }

    /**
     * Converts the passed `raw` value and returns it.
     *
     * @param <V>   the type of the converted value
     * @param value the value to convert
     * @param type  the key of the {@code StringifierRegistry} storage
     *              to obtain the {@code Stringifier}
     * @return the converted value
     * @throws ConversionException if passed value cannot be converted
     */
    public <V> V convert(String value, Type type) throws ConversionException {
        try {
            final V convertedValue = Stringifiers.fromString(value, type);
            return convertedValue;
        } catch (RuntimeException ex) {
            final Throwable rootCause = getRootCause(ex);
            throw new ConversionException(ex.getMessage(), rootCause);
        }
    }

    @Override
    public <V> void validate(FieldDescriptor descriptor, V fieldValue, String fieldName)
            throws ConstraintViolationThrowable {
        final FieldPath fieldPath = FieldPath.newBuilder()
                                             .addFieldName(fieldName)
                                             .build();
        final FieldValidator<?> validator =
                FieldValidatorFactory.create(descriptor, fieldValue, fieldPath);
        final List<ConstraintViolation> violations = validator.validate();
        onViolations(violations);
    }

    /**
     * Checks whether any modifications has been made to the fields of message being built.
     *
     * @return {@code true} if any modifications has been made, {@code false} otherwise.
     */
    @Internal
    public boolean isDirty() {
        final T message = internalBuild();

        final boolean result = originalState != null
                ? originalState.equals(message)
                : Validate.isNotDefault(message);
        return result;
    }

    /**
     * Sets an original state for this builder to allow building the new value on top of some
     * pre-defined value.
     *
     * @param state the new state
     */
    @Internal
    public void setOriginalState(T state) {
        checkNotNull(state);
        this.originalState = state;

        onOriginalStateChanged(state);
    }

    /**
     * Notifies the descendants on the change of the original state.
     *
     * @param state the new original state for this builder
     *              @see #setOriginalState(Message)
     *
     */
    protected abstract void onOriginalStateChanged(T state);

    /**
     * Obtains the original message state, if it has been set via
     * {@linkplain #setOriginalState(Message) setOriginalState(..)}.
     *
     * @return original state as {@code Optional} value if it is set,
     *         or {@code Optional.absent()} otherwise
     */
    protected Optional<T> getOriginalState() {
        return Optional.fromNullable(originalState);
    }

    private void validateResult(T message) throws ConstraintViolationThrowable {
        final List<ConstraintViolation> violations = MessageValidator.newInstance()
                                                                     .validate(message);
        onViolations(violations);
    }

    private static void onViolations(List<ConstraintViolation> violations)
            throws ConstraintViolationThrowable {
        if (!violations.isEmpty()) {
            throw new ConstraintViolationThrowable(violations);
        }
    }
}
