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

package io.spine.server.procman.migration;

import io.spine.annotation.Experimental;
import io.spine.base.EntityState;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.Migration;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerMigration;

/**
 * A migration operation that triggers the update of the {@link ProcessManager} state according
 * to the logic defined in {@code onBeforeCommit()} method, if it is defined.
 *
 * <p>{@code onBeforeCommit()} is designed to contain common logic on setting
 * the calculated state fields. In a normal operational mode it is executed after a process manager
 * handles some signal and before the respective transaction is committed.
 *
 * <p>However, in case this logic is changed, it may be inconvenient to feed all existing process
 * managers with some signal just to enforce the updated {@code onBeforeCommit()} to be executed.
 *
 * <p>{@code UpdatePmColumns} creates a transaction which does nothing other than running
 * the {@code onBeforeCommit()} and saving the state changes.
 *
 * @see io.spine.server.entity.RecordBasedRepository#applyMigration(Object, Migration)
 */
@Experimental
public final class UpdatePmState<I,
                                 P extends ProcessManager<I, S, B>,
                                 S extends EntityState<I>,
                                 B extends ValidatingBuilder<S>>
        extends ProcessManagerMigration<I, P, S, B> {

    @Override
    public S apply(S s) {
        return s;
    }
}
