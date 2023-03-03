/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.client;

import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.core.EmptyContext;
import io.spine.protobuf.AnyPacker;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Filters.extractFilters;

/**
 * Allows to subscribe to updates of entity states using filtering conditions.
 *
 * @param <S>
 *         the type of entity state to subscribe
 */
public final class SubscriptionRequest<S extends EntityState>
        extends SubscribingRequest<S, EmptyContext, S, SubscriptionRequest<S>> {

    private final StateConsumers.Builder<S> consumers;

    private @Nullable NoLongerMatchingConsumer<?> nlmConsumer;

    SubscriptionRequest(ClientRequest parent, Class<S> type) {
        super(parent, type);
        this.consumers = StateConsumers.newBuilder();
    }

    /**
     * Configures the request to return results matching all the passed filters.
     */
    public SubscriptionRequest<S> where(EntityStateFilter... filter) {
        builder().where(extractFilters(filter));
        return self();
    }

    /**
     * Configures the request to return results matching all the passed filters.
     */
    public SubscriptionRequest<S> where(CompositeEntityStateFilter... filter) {
        builder().where(extractFilters(filter));
        return self();
    }

    /**
     * Adds a consumer observing the entities which previously matched the subscription criteria,
     * but stopped to do so.
     *
     * <p>The consumer is fed with the ID of the entity in the use-cases which follow:
     *
     * <ul>
     *     <li>the value of entity fields is changed, so that the entity state does not pass
     *     the subscription filters;
     *     <li>entity is deleted;
     *     <li>entity is archived.</li>
     * </ul>
     *
     * @param consumer
     *         the consumer to notify
     * @param idType
     *         the type of entity identifiers
     * @param <I>
     *         type of entity identifiers, for covariance
     * @return this instance of {@code SubscriptionRequest}, for call chaining
     */
    public <I> SubscriptionRequest<S> whenNoLongerMatching(Class<I> idType, Consumer<I> consumer) {
        checkNotNull(idType);
        checkNotNull(consumer);
        nlmConsumer = new NoLongerMatchingConsumer<>(idType, consumer);
        return self();
    }

    @Override
    StateConsumers.Builder<S> consumers() {
        return consumers;
    }

    @Override
    protected Optional<StreamObserver<SubscriptionUpdate>> chain() {
        if(null == nlmConsumer) {
            return Optional.empty();
        }
        return Optional.of(new NlmObserver(nlmConsumer));
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

    private static final class NoLongerMatchingConsumer<I> implements Consumer<Any> {

        private final Class<I> idClass;
        private final Consumer<I> delegate;

        private NoLongerMatchingConsumer(Class<I> idClass, Consumer<I> delegate) {
            this.idClass = idClass;
            this.delegate = delegate;
        }

        @Override
        public void accept(Any packedId) {
            EntityId entityId = AnyPacker.unpack(packedId, EntityId.class);
            Any any = entityId.getId();
            I unpacked = Identifier.unpack(any, idClass);
            delegate.accept(unpacked);
        }
    }

    private static final class NlmObserver implements StreamObserver<SubscriptionUpdate> {

        private final NoLongerMatchingConsumer<?> consumer;

        private NlmObserver(NoLongerMatchingConsumer<?> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onNext(SubscriptionUpdate value) {
            if (value.hasEntityUpdates()) {
                value.getEntityUpdates()
                     .getUpdateList()
                     .stream()
                     .filter(update -> update.getKindCase() ==
                             EntityStateUpdate.KindCase.NO_LONGER_MATCHING)
                     .map(EntityStateUpdate::getId)
                     .forEach(consumer);
            }
        }

        @Override
        public void onError(Throwable t) {
            // do nothing.
        }

        @Override
        public void onCompleted() {
            // do nothing.
        }
    }
}
