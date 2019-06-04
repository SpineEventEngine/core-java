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

package io.spine.server.entity;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.code.proto.FieldDeclaration;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.model.EntityClass;
import io.spine.validate.option.Required;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helps to initialize ID field of an entity state, if such a field is declared.
 */
@Immutable
final class IdField {

    private final @MonotonicNonNull FieldDeclaration declaration;

    IdField(EntityClass<?> entityClass) {
        Message defaultState = entityClass.defaultState();
        List<FieldDescriptor> fields =
                defaultState.getDescriptorForType()
                            .getFields();
        if (fields.isEmpty()) {
            /* The entity state is an empty message.
               This is weird for production purposes, but can be used for test stubs. */
            declaration = null;
        } else {
            FieldDescriptor firstField = fields.get(0);
            Class<?> fieldClass = defaultState.getField(firstField)
                                              .getClass();
            declaration = fieldClass.equals(entityClass.idClass())
                ? new FieldDeclaration(firstField)
                : null;
        }
    }

    /**
     * Returns {@code true} if the entity state type declares a field which is the ID of the entity,
     * {@code false} otherwise.
     */
    private boolean declared() {
        return declaration != null;
    }

    /**
     * Initializes the passed builder with the passed value of the entity ID,
     * <em>iff</em> the field is required.
     */
    <I, S extends Message, B extends ValidatingBuilder<S>>
    void initBuilder(B builder, I id) {
        checkNotNull(builder);
        checkNotNull(id);
        if (!declared()) {
            return;
        }
        FieldDescriptor idField = declaration.descriptor();
        @SuppressWarnings("Immutable") // all supported types of IDs are immutable.
        Required<I> required = Required.create(false);
        boolean isRequired = required.valueFrom(idField)
                                     .orElse(true); // assume required, if not set to false
        if (isRequired) {
            builder.setField(idField, id);
        }
    }
}
