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

package io.spine.server.aggregate;

import io.spine.core.EventEnvelope;

/**
 * Abstract base for endpoints that dispatch events to aggregates.
 *
 * <p>An aggregate may receive an event if it {@linkplain io.spine.server.event.React reacts} on it,
 * or if it {@linkplain io.spine.server.aggregate.Apply#allowImport() imports} it.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates
 *
 * @author Alexander Yevsyukov
 */
abstract class AggregateEventProxy<I, A extends Aggregate<I, ?, ?>>
        extends AggregateProxy<I, A, EventEnvelope> {

    AggregateEventProxy(AggregateRepository<I, A> repository, I aggregateId) {
        super(repository, aggregateId);
    }
}
