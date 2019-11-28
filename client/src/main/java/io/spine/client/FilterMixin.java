/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.EntityState;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.code.proto.FieldDeclaration;
import io.spine.protobuf.TypeConverter;
import io.spine.type.TypeUrl;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.OperatorEvaluator.eval;
import static io.spine.code.proto.ColumnOption.isColumn;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Augments {@link Filter} with useful methods.
 */
@GeneratedMixin
@SuppressWarnings("override") // to handle the absence of `@Override` in the generated code.
interface FilterMixin extends FilterOrBuilder, MessageFilter<Message> {

    /**
     * Obtains the target field.
     */
    default Field field() {
        FieldPath fieldPath = getFieldPath();
        return Field.withPath(fieldPath);
    }

    /**
     * Checks if the target field is present in the specified message type.
     */
    default boolean fieldPresentIn(Descriptor message) {
        checkNotNull(message);
        Field field = field();
        boolean result = field.presentIn(message);
        return result;
    }

    /**
     * Verifies that the target field is present in the passed message type.
     *
     * @throws IllegalArgumentException
     *         if the field is not present in the passed message type
     */
    default void checkFieldPresentIn(Descriptor message) {
        checkNotNull(message);
        if (!fieldPresentIn(message)) {
            throw newIllegalArgumentException(
                    "The field with path `%s` is not present in the message type `%s`.",
                    field(), message.getFullName());
        }
    }

    /**
     * Checks if the target field is an entity column in the passed message type.
     */
    default boolean fieldIsColumnIn(Descriptor message) {
        checkNotNull(message);
        Optional<FieldDescriptor> fieldDescriptor = field().findDescriptor(message);
        if (!fieldDescriptor.isPresent()) {
            return false;
        }
        FieldDeclaration declaration = new FieldDeclaration(fieldDescriptor.get());
        boolean result = isColumn(declaration);
        return result;
    }

    /**
     * Verifies that the target field is an entity column in the passed message type.
     *
     * @throws IllegalArgumentException
     *         if the field is not an entity column in the passed message type
     */
    default void checkFieldIsColumnIn(Descriptor message) {
        checkNotNull(message);
        checkFieldAtTopLevel();
        if (!fieldIsColumnIn(message)) {
            throw newIllegalArgumentException(
                    "The column `%s` is not found in entity state type `%s`. " +
                         "Please check the field exists and is marked with the `(column)` option.",
                    field(), message.getFullName());
        }
    }

    /**
     * Checks if the target field is a top-level field.
     */
    default boolean fieldAtTopLevel() {
        return !field().isNested();
    }

    /**
     * Verifies that the target field is a top-level field.
     *
     * @throws IllegalArgumentException
     *         if the field is not a top-level field
     */
    default void checkFieldAtTopLevel() {
        if (!fieldAtTopLevel()) {
            throw newIllegalArgumentException(
                    "The entity filter contains a nested entity column `%s`. " +
                            "Nested entity columns are currently not supported.",
                    field()
            );
        }
    }

    /**
     * Verifies that the filter can be applied to the given {@code target}.
     *
     * <p>Makes sure the field specified in the filter is a valid entity column or a message field
     * in the type enclosed by the {@code target}.
     *
     * @throws IllegalArgumentException
     *         if the field is not present in the target type or doesn't satisfy the constraints
     */
    default void checkCanApplyTo(Target target) {
        checkNotNull(target);

        TypeUrl targetType = target.typeUrl();

        Class<Message> javaClass = targetType.getMessageClass();
        Descriptor descriptor = targetType.toTypeName()
                                          .messageDescriptor();
        if (EntityState.class.isAssignableFrom(javaClass)) {
            checkFieldIsColumnIn(descriptor);
        } else {
            checkFieldPresentIn(descriptor);
        }
    }

    /**
     * Verifies if this filter passed the message.
     */
    @Override
    default boolean test(Message message) {
        Field field = Field.withPath(getFieldPath());
        Object actual = field.valueIn(message);
        Any requiredAsAny = getValue();
        Object required = TypeConverter.toObject(requiredAsAny, actual.getClass());
        try {
            return eval(actual, getOperator(), required);
        } catch (IllegalArgumentException e) {
            throw newIllegalArgumentException(
                    e,
                    "Filter value `%s` cannot be properly compared to" +
                            " the message field `%s` of the class `%s`.",
                    required, field, actual.getClass().getName()
            );
        }
    }
}
