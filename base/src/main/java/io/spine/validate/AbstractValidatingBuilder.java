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
import io.spine.base.ConversionException;
import io.spine.base.FieldPath;
import io.spine.protobuf.Messages;
import io.spine.string.Stringifiers;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;

/**
 * Serves as an abstract base for all {@linkplain ValidatingBuilder validating builders}.
 *
 * @author Illia Shepilov
 * @author Alex Tymchenko
 */
public abstract class AbstractValidatingBuilder<T extends Message, B extends Message.Builder>
        implements ValidatingBuilder<T, B> {

    /**
     * The builder for the original {@code Message}.
     */
    private final B messageBuilder;

    /**
     * The class of the {@code Message} being built.
     */
    private final Class<T> messageClass;

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

    protected AbstractValidatingBuilder() {
        this.messageClass = TypeInfo.getMessageClass(getClass());
        this.messageBuilder = createBuilder();
    }

    @Override
    public T build() throws ValidationException {
        final T message = internalBuild();
        validateResult(message);
        return message;
    }

    @Override
    public void clear() {
        messageBuilder.clear();
        originalState = null;
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
            throws ValidationException {
        final FieldPath fieldPath = FieldPath.newBuilder()
                                             .addFieldName(fieldName)
                                             .build();
        final FieldValidator<?> validator =
                FieldValidatorFactory.create(descriptor, fieldValue, fieldPath);
        final List<ConstraintViolation> violations = validator.validate();
        onViolations(violations);
    }

    /**
     * Checks whether any modifications have been made to the fields of message being built.
     *
     * @return {@code true} if any modifications have been made, {@code false} otherwise.
     */
    @Override
    public boolean isDirty() {
        final T message = internalBuild();

        final boolean result = originalState != null
                ? !originalState.equals(message)
                : Validate.isNotDefault(message);
        return result;
    }

    @Override
    public void setOriginalState(T state) {
        checkNotNull(state);
        this.originalState = state;

        messageBuilder.clear();
        messageBuilder.mergeFrom(state);
    }

    protected B getMessageBuilder() {
        return messageBuilder;
    }

    @Override
    public ValidatingBuilder<T, B> mergeFrom(T message) {
        messageBuilder.mergeFrom(message);
        return this;
    }

    /**
     * Builds a message without triggering its validation.
     *
     * <p>Exposed to those who wish to obtain the state anyway, e.g. for logging.
     *
     * @return the message built from the values set by the user
     */
    @Internal
    public T internalBuild() {
        @SuppressWarnings("unchecked")  // OK, as real types of `B`
                     // are always generated to be compatible with `T`.
        final T result = (T) getMessageBuilder().build();
        return result;
    }

    private B createBuilder() {
        @SuppressWarnings("unchecked")  // OK, since it is guaranteed by the class declaration.
        final B result = (B) Messages.newInstance(messageClass)
                                     .newBuilderForType();
        return result;
    }

    private void validateResult(T message) throws ValidationException {
        final List<ConstraintViolation> violations = MessageValidator.newInstance()
                                                                     .validate(message);
        onViolations(violations);
    }

    private static void onViolations(List<ConstraintViolation> violations)
            throws ValidationException {
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }
}
