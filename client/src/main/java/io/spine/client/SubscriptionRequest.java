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

package io.spine.client;

import io.spine.base.EntityState;
import io.spine.core.EmptyContext;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Allows to subscribe to updates of entity states using filtering conditions.
 *
 * @param <S>
 *         the type of entity state to subscribe
 */
public final class SubscriptionRequest<S extends EntityState>
        extends SubscribingRequest<S, EmptyContext, S, SubscriptionRequest<S>> {

    private final StateConsumers.Builder<S> consumers;

    SubscriptionRequest(ClientRequest parent, Class<S> type) {
        super(parent, type);
        this.consumers = StateConsumers.newBuilder();
    }

    public final SubscriptionRequest<S> where(EntityStateFilter... filter) {
        Filter[] filters = Arrays.stream(filter)
                                 .map(EntityStateFilter::filter)
                                 .toArray(Filter[]::new);
        builder().where(filters);
        return self();
    }

    public final SubscriptionRequest<S> where(CompositeEntityStateFilter... filter) {
        CompositeFilter[] filters = Arrays.stream(filter)
                                          .map(CompositeEntityStateFilter::filter)
                                          .toArray(CompositeFilter[]::new);
        builder().where(filters);
        return self();
    }

    @Override
    StateConsumers.Builder<S> consumers() {
        return consumers;
    }

    @Override
    StateConsumer<S> toMessageConsumer(Consumer<S> consumer) {
        return StateConsumer.from(consumer);
    }

    @Override
    SubscriptionRequest<S> self() {
        return this;
    }

    @Override
    Function<ActorRequestFactory, TopicBuilder> builderFn() {
        return (factory) -> factory.topic().select(messageType());
    }
}
