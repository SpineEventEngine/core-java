/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.annotation.Internal;
import io.spine.core.ActorMessageEnvelope;
import io.spine.server.entity.Entity;
import io.spine.server.sharding.ShardedStream;


/**
 * A strategy on delivering the messages to the instances of a certain entity type.
 *
 * @param <I> the ID type of entity, to which the messages are being delivered
 * @param <E> the type of entity
 * @param <M> the type of message envelope, which is used for message delivery
 *
 * @author Alex Tymchenko
 */
@Internal
public abstract class EndpointDelivery<I,
                                       E extends Entity<I, ?>,
                                       M extends ActorMessageEnvelope<?, ?, ?>,
                                       S extends ShardedStream<I, ?, M>,
                                       B extends ShardedStream.AbstractBuilder<I, B, S>> {

    private final Sender<I, M> sender;
    private final Consumer<I, E, M, S, B> consumer;

    protected EndpointDelivery(Consumer<I, E, M, S, B> consumer) {
        this.consumer = consumer;
        this.sender = new Sender<>(consumer.getTag());
    }

    public Sender<I, M> getSender() {
        return sender;
    }

    public Consumer<I, E, M, S, B> getConsumer() {
        return consumer;
    }

    //    public void deliver(I id, M message) {
//        final ShardingTag<M> consumerId = getTag();
//        final Set<ShardedStream<I, M>> streams = sharding().find(consumerId, id);
//
//        for (ShardedStream<I, M> shardedStream : streams) {
//            shardedStream.post(id, message);
//        }
//    }

//    /**
//     * Obtains an endpoint to dispatch the given envelope.
//     *
//     * @param messageEnvelope the envelope to obtain the endpoint for
//     * @return the message endpoint
//     */
//    protected abstract EntityMessageEndpoint<I, E, M, ?> getEndpoint(M messageEnvelope);

//    protected abstract B newShardedStreamBuilder();

//    @Override
//    public ShardingTag<M> getTag() {
//        return shardingTag;
//    }
//
//    /**
//     * Delivers the envelope to the entity of the given ID taking into account
//     * the target tenant.
//     *
//     * <p>Use this method to deliver the previously postponed messages.
//     *
//     * @param id       an ID of an entity to deliver the envelope to
//     * @param envelopeMessage an envelope to deliver
//     */
//    protected void deliverNow(final I id, final M envelopeMessage) {
//        final TenantId tenantId = envelopeMessage.getActorContext()
//                                                 .getTenantId();
//        final TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
//            @Override
//            public void run() {
//                passToEndpoint(id, envelopeMessage);
//            }
//        };
//
//        operation.run();
//    }

//    /**
//     * Calls the dispatching method of endpoint directly.
//     *
//     * @param id an ID of an entity to deliver th envelope to
//     * @param envelopeMessage an envelope to delivery
//     */
//    protected abstract void passToEndpoint(I id, M envelopeMessage);
//


//    @Override
//    public ShardedStream<I, M> bindToTransport(BoundedContextName name,
//                                               ShardingKey key,
//                                               TransportFactory transportFactory) {
//        final ShardedStream<I, M> stream =
//                newShardedStreamBuilder().setBoundedContextName(name)
//                                         .setKey(key)
//                                         .build(transportFactory);
//        stream.setConsumer(this);
//        return stream;
//    }


//    @Override
//    public void onNext(I targetId, M messageEnvelope) {
//        deliverNow(targetId, messageEnvelope);
//    }

//    private enum LogSingleton {
//        INSTANCE;
//        @SuppressWarnings("NonSerializableFieldInSerializableClass")
//        private final Logger value = LoggerFactory.getLogger(EndpointDelivery.class);
//    }
//
//    private static Logger log() {
//        return LogSingleton.INSTANCE.value;
//    }
}
