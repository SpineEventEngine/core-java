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
package io.spine.server.delivery;

import io.spine.core.ActorMessageEnvelope;
import io.spine.core.BoundedContextName;
import io.spine.core.TenantId;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.transport.TransportFactory;

/**
 * The consuming part of the {@linkplain Delivery}.
 *
 * <p>As long as the delivery process is sharded, the messages are transferred via a
 * {@linkplain ShardedStream sharded stream}.
 *
 * @author Alex Tymchenko
 */
public abstract class Consumer<I,
                               E extends Entity<I, ?>,
                               M extends ActorMessageEnvelope<?, ?, ?>,
                               S extends ShardedStream<I, ?, M>,
                               B extends ShardedStream.AbstractBuilder<I, M, B, S>>
                        implements ShardedStreamConsumer<I, M> {

    private final Repository<I, E> repository;
    private final DeliveryTag<M> deliveryTag;

    protected Consumer(DeliveryTag<M> tag, Repository<I, E> repository) {
        this.deliveryTag = tag;
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShardedStream<I, ?, M> bindToTransport(BoundedContextName name,
                                                  ShardingKey key,
                                                  TransportFactory transportFactory) {
        S stream = newShardedStreamBuilder().setBoundedContextName(name)
                                                  .setKey(key)
                                                  .setTag(deliveryTag)
                                                  .setTargetIdClass(repository.getIdClass())
                                                  .setConsumer(this)
                                                  .build(transportFactory);
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNext(I targetId, M messageEnvelope) {
        deliverNow(targetId, messageEnvelope);
    }

    /**
     * Delivers the envelope to the entity of the given ID taking into account
     * the target tenant.
     *
     * <p>Use this method to deliver the previously postponed messages.
     *
     * @param id              an ID of an entity to deliver the envelope to
     * @param envelopeMessage an envelope to deliver
     */
    protected void deliverNow(I id, M envelopeMessage) {
        TenantId tenantId = envelopeMessage.getActorContext()
                                                 .getTenantId();
        TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                passToEndpoint(id, envelopeMessage);
            }
        };

        operation.execute();
    }

    /**
     * Creates a new instance of a builder for the sharded stream.
     *
     * <p>Descendants must implement this method, as long as the exact type of {@code B} is only
     * available at the end of the inheritance tree.
     *
     * @return the new instance of sharded stream builder
     */
    protected abstract B newShardedStreamBuilder();

    /**
     * {@inheritDoc}
     */
    @Override
    public DeliveryTag<M> getTag() {
        return deliveryTag;
    }

    /**
     * Calls the dispatching method of endpoint directly.
     *
     * @param id              an ID of an entity to deliver th envelope to
     * @param envelopeMessage an envelope to delivery
     */
    protected abstract void passToEndpoint(I id, M envelopeMessage);

    /**
     * Obtains an endpoint to dispatch the given envelope.
     *
     * @param messageEnvelope the envelope to obtain the endpoint for
     * @return the message endpoint
     */
    protected abstract EntityMessageEndpoint<I, E, M, ?> getEndpoint(M messageEnvelope);

    protected Repository<I, E> repository() {
        return repository;
    }
}
