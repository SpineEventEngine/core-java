/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.base.EntityState;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.MessageEnvelope;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal utility class for assisting in aggregate tests.
 *
 * @apiNote This internal class is designed to be called only from Testutil Server library.
 *          Calling it other code would result in run-time error.
 */
public final class AggregateTestSupport {

    /** Prevents instantiation of this utility class. */
    private AggregateTestSupport() {
    }

    /**
     * Dispatches a command to an instance of an {@code Aggregate}.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     * @param <S> the type of {@code Aggregate} state
     * @return the list of produced event messages
     */
    public static <I, A extends Aggregate<I, S, ?>, S extends EntityState<I>> DispatchOutcome
    dispatchCommand(AggregateRepository<I, A, S> repository, A aggregate, CommandEnvelope command) {
        checkArguments(repository, aggregate, command);
        return dispatchAndCollect(new AggregateCommandEndpoint<>(repository, command),aggregate);
    }

    /**
     * Dispatches an event to an instance of {@code Aggregate} into its reactor methods.
     *
     * @param <I> the type of {@code Aggregate} identifier
     * @param <A> the type of {@code Aggregate}
     * @param <S> the type of {@code Aggregate} state
     * @return the list of produced event messages
     */
    public static <I, A extends Aggregate<I, S, ?>, S extends EntityState<I>> DispatchOutcome
    dispatchEvent(AggregateRepository<I, A, S> repository, A instance, EventEnvelope event) {
        checkArguments(repository, instance, event);
        return dispatchAndCollect(new AggregateEventReactionEndpoint<>(repository, event),instance);
    }

    private static <I, A extends Aggregate<I, ?, ?>> DispatchOutcome
    dispatchAndCollect(AggregateEndpoint<I, A, ?> endpoint, A aggregate) {
        return endpoint.handleAndApplyEvents(aggregate);
    }

    private static <I, A extends Aggregate<I, S, ?>, S extends EntityState<I>> void
    checkArguments(AggregateRepository<I, A, S> repository,
                   A aggregate,
                   MessageEnvelope<?, ?, ?> envelope) {
        checkNotNull(repository);
        checkNotNull(aggregate);
        checkNotNull(envelope);
    }
}
