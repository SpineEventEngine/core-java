/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.aggregate.given.repo;

import com.google.common.collect.ImmutableSet;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRouting;
import io.spine.server.test.shared.LongIdAggregate;
import io.spine.test.aggregate.number.FloatEncountered;
import io.spine.test.aggregate.number.RejectNegativeInt;

/**
 * The repository of {@link io.spine.server.aggregate.given.repo.FailingAggregate}s.
 */
public final class FailingAggregateRepository
        extends AggregateRepository<Long, FailingAggregate, LongIdAggregate> {

    @Override
    protected void setupCommandRouting(CommandRouting<Long> routing) {
        super.setupCommandRouting(routing);
        routing.replaceDefault((msg, ctx) -> byAbsoluteValueOf(msg));
    }

    /**
     * Defines a routing function that takes absolute value as ID.
     */
    private static long byAbsoluteValueOf(CommandMessage message) {
        if (message instanceof RejectNegativeInt) {
            var event = (RejectNegativeInt) message;
            return Math.abs(event.getNumber());
        }
        return 0L;
    }

    @Override
    protected void setupEventRouting(EventRouting<Long> routing) {
        super.setupEventRouting(routing);
        routing.replaceDefault((message, context) -> newDefaultRouteFor(message));
    }

    /**
     * Returns several entity identifiers to check error isolation.
     *
     * @see io.spine.server.aggregate.given.repo.FailingAggregate#on(io.spine.test.aggregate.number.FloatEncountered)
     */
    private static ImmutableSet<Long> newDefaultRouteFor(EventMessage message) {
        if (message instanceof FloatEncountered) {
            var absValue = FailingAggregate.toId((FloatEncountered) message);
            return ImmutableSet.of(absValue, absValue + 100, absValue + 200);
        }
        return ImmutableSet.of(1L, 2L);
    }
}
