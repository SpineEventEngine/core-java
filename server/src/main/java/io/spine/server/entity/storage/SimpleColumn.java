/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.errorprone.annotations.Immutable;
import io.spine.base.EntityState;
import io.spine.code.proto.FieldDeclaration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * A simple entity-state-based column.
 *
 * <p>An entity-state-based column is:
 *    <ol>
 *         <li>Declared in Protobuf with {@code (column)} option:
 *             <pre>
 *             string task_description = 3 [(column) = true];
 *             </pre>
 *         <li>Updated in handler methods of the entity along with other entity state fields:
 *             <pre>
 *            {@literal @Subscribe}
 *             void on(TaskDescriptionUpdated event) {
 *                 builder().setTaskDescription(event.getNewTaskDescription())
 *             }
 *             </pre>
 *    </ol>
 */
final class SimpleColumn
        extends AbstractColumn
        implements ColumnDeclaredInProto {

    /**
     * A getter of the column from the entity state.
     */
    private final Getter getter;

    /**
     * The corresponding proto field declaration.
     */
    private final FieldDeclaration field;

    SimpleColumn(ColumnName name, Class<?> type, Getter getter, FieldDeclaration field) {
        super(name, type);
        this.getter = getter;
        this.field = field;
    }

    @Override
    public @Nullable Object valueIn(EntityState state) {
        return getter.apply(state);
    }

    @Override
    public FieldDeclaration protoField() {
        return field;
    }

    @Immutable
    interface Getter extends Function<EntityState, Object> {
    }
}
