/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import io.spine.core.EventEnvelope;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The context which {@link EntityVersioning} uses for version increment.
 */
final class EntityVersioningContext {

    private final Transaction transaction;
    private final @Nullable EventEnvelope event;

    /**
     * Creates an {@code EntityVersioningContext} based on the given transaction.
     *
     * <p>The event field remains {@code null}, so such context isn't suitable for the versioning
     * strategies which are based on the handled event version.
     */
    EntityVersioningContext(Transaction transaction) {
        this.transaction = transaction;
        this.event = null;
    }

    /**
     * Creates an {@code EntityVersioningContext} from the given transaction and event.
     */
    EntityVersioningContext(Transaction transaction, EventEnvelope event) {
        this.transaction = transaction;
        this.event = event;
    }

    /**
     * A transaction during which the version increment occurs.
     */
    Transaction transaction() {
        return transaction;
    }

    /**
     * An event which is handled by the entity during transaction.
     *
     * <p>The event is optional in the versioning context as the version change can occur upon
     * command handling (see {@link io.spine.server.procman.ProcessManager}).
     */
    @Nullable EventEnvelope event() {
        return event;
    }
}
