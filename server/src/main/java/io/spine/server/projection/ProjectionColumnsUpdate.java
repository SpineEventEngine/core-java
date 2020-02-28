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

package io.spine.server.projection;

import io.spine.annotation.Experimental;
import io.spine.base.EntityState;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.Migration;
import io.spine.server.entity.Transaction;

/**
 * A migration operation that does the update of interface-based columns of a {@link Projection}.
 *
 * <p>When applied to an entity, this operation will trigger the recalculation of entity storage
 * fields according to the current implementation of {@link io.spine.base.EntityWithColumns}-derived
 * methods.
 *
 * <p>The operation relies on the fact that column values are calculated and propagated to the
 * entity state on a transaction {@linkplain Transaction#commit() commit} and thus does not change
 * the entity state itself in {@link #apply(EntityState)}.
 *
 * @see io.spine.server.entity.storage.InterfaceBasedColumn
 * @see io.spine.server.entity.RecordBasedRepository#applyMigration(Object, Migration)
 */
@Experimental
public final class ProjectionColumnsUpdate<I,
                                           P extends Projection<I, S, B>,
                                           S extends EntityState,
                                           B extends ValidatingBuilder<S>>
        extends ProjectionMigration<I, P, S, B> {

    @Override
    public S apply(S s) {
        return s;
    }
}
