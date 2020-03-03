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

package io.spine.server.entity.migration;

import io.spine.annotation.Experimental;
import io.spine.base.EntityState;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.DelegatingMigration;
import io.spine.server.entity.Migration;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.migration.UpdatePmColumns;
import io.spine.server.projection.Projection;
import io.spine.server.projection.migration.UpdateProjectionColumns;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.migration.UnsupportedEntityType.unsupportedEntityType;

/**
 * A migration operation that does the update of interface-based columns of an
 * {@link io.spine.server.entity.Entity Entity}.
 *
 * <p>When applied to an entity, this operation will trigger the recalculation of entity storage
 * fields according to the current implementation of
 * {@link io.spine.base.EntityWithColumns EntityWithColumns}-derived methods.
 *
 * <p>Such operation may be useful when the logic behind manually calculated columns changes as
 * well as when adding the new columns to an entity.
 *
 * @implNote The operation relies on the fact that column values are automatically calculated and
 *         propagated to the entity state on a transaction
 *         {@linkplain io.spine.server.entity.Transaction#commit() commit}.
 *         It thus does not change the entity state itself in {@link #apply(EntityState)}.
 *
 * @see io.spine.server.entity.storage.InterfaceBasedColumn
 * @see io.spine.server.entity.RecordBasedRepository#applyMigration(Object, Migration)
 */
@Experimental
public final class UpdateColumns<I,
                                 E extends TransactionalEntity<I, S, ?>,
                                 S extends EntityState>
        extends DelegatingMigration<I, E, S> {

    private UpdateColumns(Migration<I, E, S> migration) {
        super(migration);
    }

    /**
     * Creates a new instance of the migration for the passed entity class.
     */
    @SuppressWarnings("unchecked") // Checked at runtime.
    public static <I,
                   E extends TransactionalEntity<I, S, B>,
                   S extends EntityState,
                   B extends ValidatingBuilder<S>>
    UpdateColumns<I, E, S> of(Class<E> entityClass) {
        checkNotNull(entityClass);
        if (Projection.class.isAssignableFrom(entityClass)) {
            return new UpdateColumns<>((Migration<I, E, S>) new UpdateProjectionColumns<>());
        }
        if (ProcessManager.class.isAssignableFrom(entityClass)) {
            return new UpdateColumns<>((Migration<I, E, S>) new UpdatePmColumns<>());
        }
        throw unsupportedEntityType(entityClass);
    }
}
