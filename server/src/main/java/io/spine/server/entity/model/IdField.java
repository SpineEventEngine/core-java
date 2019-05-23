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

package io.spine.server.entity.model;

import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.code.proto.FieldDeclaration;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.Serializable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helps to initialize ID field of an entity state, if such a field is declared.
 */
@Immutable
@SuppressWarnings("SynchronizeOnThis") // Double-check idiom for lazy init.
public final class IdField implements Serializable {

    private static final long serialVersionUID = 0L;
    private final EntityClass<?> entityClass;

    @LazyInit
    private transient volatile @MonotonicNonNull Boolean declared;
    @LazyInit
    private transient volatile @MonotonicNonNull FieldDeclaration declaration;

    IdField(EntityClass<?> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Returns {@code true} if the entity state type declares a field which is the ID of the entity,
     * {@code false} otherwise.
     */
    public boolean declared() {
        Boolean result = declared;
        if (result == null) {
            synchronized (this) {
                result = declared;
                if (result == null) {
                    Message defaultState = entityClass.defaultState();
                    List<FieldDescriptor> fields =
                            defaultState.getDescriptorForType()
                                        .getFields();
                    if (fields.isEmpty()) {
                        /* The entity state is an empty message. This is weird for
                        production purposes, but can be used for test stubs. */
                        declared = false;
                    } else {
                        FieldDescriptor firstField = fields.get(0);
                        declaration = new FieldDeclaration(firstField);
                        Class<?> fieldClass = defaultState.getField(firstField)
                                                          .getClass();
                        declared = fieldClass.equals(entityClass.idClass());
                    }
                    result = declared;
                }
            }
        }
        return result;
    }

    /**
     * Initializes the ID field with the passed value if it was not set yet.
     *
     * @return updated state if the field was updated, otherwise the passed instance
     */
    public <I, S extends Message> S init(S state, I id) {
        checkNotNull(state);
        checkNotNull(id);
        checkState(declared());
        FieldDescriptor idField = declaration.descriptor();
        Object currentValue = state.getField(idField);
        if (declaration.isDefault(currentValue)) {
            @SuppressWarnings("unchecked") /* safe as converting the passed instance. */
            S updatedState = (S)
                state.toBuilder()
                     .setField(idField, id)
                     .build();
            return updatedState;
        }
        return state;
    }
}
