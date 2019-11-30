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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import io.spine.base.EntityState;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.code.proto.FieldDeclaration;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.code.proto.ColumnOption.isColumn;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A field of a target filter.
 */
final class FilteringField {

    private final Field field;

    FilteringField(FilterOrBuilder filter) {
        checkNotNull(filter);
        FieldPath fieldPath = filter.getFieldPath();
        this.field = Field.withPath(fieldPath);
    }

    /**
     * Verifies if the field is applicable to the passed target.
     *
     * <p>If the passed target represents an entity state, the field must be a reference to
     * a column (and annotated as such).
     *
     * <p>If the passed target is an event message, the field must be present in the event message.
     * or, the field must reference a field of {@code EventContext}.
     *
     * @throws IllegalStateException
     *         if the field does not apply to the passed target
     */
    void checkAppliesTo(Target target) {
        checkNotNull(target);
        Descriptor descriptor = target.messageDescriptor();
        boolean targetIsEntityState = EntityState.class.isAssignableFrom(target.messageClass());
        if (targetIsEntityState) {
            checkFieldIsColumnIn(descriptor);
        } else {
            // We're filtering event messages.
            if (refersToContext()) {
                checkPresentInContext();
            } else {
                checkPresentIn(descriptor);
            }
        }
    }

    private void checkPresentInContext() {
        //TODO:2019-11-29:alexander.yevsyukov: Implement.
    }

    private boolean refersToContext() {
        String firstInPath = field.path()
                                  .getFieldName(0);
        return firstInPath.equals(EventContextField.name());
    }

    private boolean isTopLevel() {
        return !field.isNested();
    }

    private void checkPresentIn(Descriptor message) {
        checkNotNull(message);
        if (!field.presentIn(message)) {
            throw newIllegalStateException(
                    "The field with path `%s` is not present in the message type `%s`.",
                    field, message.getFullName());
        }
    }

    private void checkFieldIsColumnIn(Descriptor message) {
        checkNotNull(message);
        checkFieldAtTopLevel();
        if (!isColumnIn(message)) {
            throw newIllegalStateException(
                    "The column `%s` is not found in entity state type `%s`. " +
                    "Please check the field exists and is marked with the `(column)` option.",
                    field, message.getFullName());
        }
    }

    private boolean isColumnIn(Descriptor message) {
        checkNotNull(message);
        Optional<Descriptors.FieldDescriptor> fieldDescriptor = field.findDescriptor(message);
        if (!fieldDescriptor.isPresent()) {
            return false;
        }
        FieldDeclaration declaration = new FieldDeclaration(fieldDescriptor.get());
        boolean result = isColumn(declaration);
        return result;
    }

    private void checkFieldAtTopLevel() {
        if (!isTopLevel()) {
            throw newIllegalStateException(
                    "The entity filter contains a nested entity column `%s`. " +
                            "Nested entity columns are currently not supported.",
                    field
            );
        }
    }
}
