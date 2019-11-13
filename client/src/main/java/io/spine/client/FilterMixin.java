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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.EntityState;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.code.proto.FieldDeclaration;
import io.spine.type.TypeUrl;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.proto.ColumnOption.isColumn;
import static io.spine.util.Exceptions.newIllegalArgumentException;

@GeneratedMixin
public interface FilterMixin {

    @SuppressWarnings("override") // Implemented in the generated code.
    FieldPath getFieldPath();

    default Field field() {
        FieldPath fieldPath = getFieldPath();
        return Field.withPath(fieldPath);
    }

    default boolean fieldPresentIn(Descriptor message) {
        checkNotNull(message);
        Field field = field();
        boolean result = field.presentIn(message);
        return result;
    }

    default void checkFieldPresentIn(Descriptor message) {
        checkNotNull(message);
        if (!fieldPresentIn(message)) {
            throw newIllegalArgumentException(
                    "The field with path `%s` is not present in message type `%s`.",
                    field(), message.getFullName());
        }
    }

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

    default void checkFieldIsColumnIn(Descriptor message) {
        checkNotNull(message);
        if (!fieldIsColumnIn(message)) {
            throw newIllegalArgumentException(
                    "The entity column `%s` is not found in entity state type `%s`. " +
                            "Please check the field exists and is marked with `(column)` option.",
                    field(), message.getFullName());
        }
    }

    default boolean fieldIsTopLevel() {
        return !field().isNested();
    }

    default void checkFieldIsTopLevel() {
        if (!fieldIsTopLevel()) {
            throw newIllegalArgumentException(
                    "The entity filter contains a nested entity column `%s`. " +
                            "The nested entity columns are currently not supported.",
                    field()
            );
        }
    }

    default void validateAgainst(TypeUrl targetType) {
        checkNotNull(targetType);

        Class<Message> javaClass = targetType.getMessageClass();
        Descriptor descriptor = targetType.toTypeName()
                                          .messageDescriptor();
        if (EntityState.class.isAssignableFrom(javaClass)) {
            checkFieldIsTopLevel();
            checkFieldIsColumnIn(descriptor);
        } else {
            checkFieldPresentIn(descriptor);
        }
    }
}
