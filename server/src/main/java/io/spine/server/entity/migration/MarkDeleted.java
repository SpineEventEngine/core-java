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
import io.spine.server.entity.Entity;
import io.spine.server.entity.Migration;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.migration.MarkPmDeleted;
import io.spine.server.projection.Projection;
import io.spine.server.projection.migration.MarkProjectionDeleted;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.migration.UnsupportedEntityType.unsupportedEntityType;

/**
 * A migration operation that marks an {@link Entity} as {@link Entity#isDeleted() deleted}.
 *
 * <p>When applied, the migration operation will modify the {@code deleted} flag of a corresponding
 * storage record to be {@code true}.
 *
 * @see io.spine.server.entity.RecordBasedRepository#applyMigration(Object, Migration)
 */
@Experimental
public final class MarkDeleted<I,
                               E extends TransactionalEntity<I, S, ?>,
                               S extends EntityState> extends DelegatingMigration<I, E, S> {

    private MarkDeleted(Migration<I, E, S> delegate) {
        super(delegate);
    }

    /**
     * Creates a new instance of the migration for the passed entity class.
     */
    @SuppressWarnings("unchecked") // Checked at runtime.
    public static <I,
                   E extends TransactionalEntity<I, S, B>,
                   S extends EntityState,
                   B extends ValidatingBuilder<S>>
    MarkDeleted<I, E, S> of(Class<E> entityClass) {
        checkNotNull(entityClass);
        if (Projection.class.isAssignableFrom(entityClass)) {
            return new MarkDeleted<>((Migration<I, E, S>) new MarkProjectionDeleted<>());
        }
        if (ProcessManager.class.isAssignableFrom(entityClass)) {
            return new MarkDeleted<>((Migration<I, E, S>) new MarkPmDeleted<>());
        }
        throw unsupportedEntityType(entityClass);
    }
}
