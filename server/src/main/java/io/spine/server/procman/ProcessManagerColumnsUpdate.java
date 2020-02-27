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

package io.spine.server.procman;

import io.spine.annotation.Experimental;
import io.spine.base.EntityState;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.Migration;
import io.spine.server.entity.Transaction;

/**
 * A migration operation that does the update of interface-based columns of a process manager.
 *
 * <p>When
 * {@linkplain io.spine.server.entity.RecordBasedRepository#applyMigration(Object, Migration)
 * applied} to an entity, this operation will trigger the recalculation of entity storage fields
 * according to the current implementation of {@link io.spine.base.EntityWithColumns}-derived
 * methods.
 *
 * <p>The operation relies on the fact that column values are calculated and propagated to the
 * entity state on a transaction {@linkplain Transaction#commit() commit}.
 *
 * @apiNote An entity columns update is considered a purely technical procedure and is not a valid
 *        <strong>domain</strong> reason for an entity to change. Thus, it does not advance an
 *        entity version and does not trigger any standard routines that are invoked on entity
 *        change (distribution of system events, delivery of subscription updates, etc.).
 *
 * @see io.spine.server.entity.storage.InterfaceBasedColumn
 */
@Experimental
public final class ProcessManagerColumnsUpdate<I,
                                               P extends ProcessManager<I, S, B>,
                                               S extends EntityState,
                                               B extends ValidatingBuilder<S>>
        extends ProcessManagerMigration<I, S, B, P> {

    @Override
    public S apply(S s) {
        return s;
    }
}
