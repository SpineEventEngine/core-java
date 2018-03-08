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
package io.spine.server.sharding;

import com.google.common.collect.Maps;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.server.model.ModelClass;
import io.spine.server.transport.TransportFactory;
import io.spine.type.ClassName;

import java.util.Map;
import java.util.Set;

/**
 * @author Alex Tymchenko
 */
public class InProcessSharding implements Sharding {

    private final TransportFactory transportFactory;
    private final Map<ShardConsumerId, ShardedStream<?, ?>> streams = Maps.newConcurrentMap();

    public InProcessSharding(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public void register(Shardable<?> shardable) throws NoShardAvailableException {
        final Iterable<ShardedStreamConsumer> consumers = shardable.getMessageConsumers();
        if (!consumers.iterator()
                      .hasNext()) {
            return;
        }
        final BoundedContextName bcName = shardable.getBoundedContextName();
        final ShardingKey shardingKey = getShardingKey(shardable.getModelClass(),
                                                       shardable.getShardingStrategy());
        for (ShardedStreamConsumer consumer : consumers) {
            final ShardConsumerId consumerId = consumer.getConsumerId();
            final ClassName className = consumerId.getObservedMsgType();
            if (className.equals(ClassName.of(CommandEnvelope.class))) {
                //TODO:2018-03-8:alex.tymchenko: deal with the proper creation.
                // Static methods of the supported-message-types enum?
                final CommandShardedStream<Object> stream =
                        new CommandShardedStream<>(shardingKey,
                                                   transportFactory,
                                                   bcName);
                stream.setConsumer(consumer);
                streams.put(consumerId,
                            stream);
            }
        }
    }

    @Override
    public <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, E>>
    find(I targetId, Class<E> envelopeClass)
            throws NoShardAvailableException {
        return null;
    }

    private ShardingKey getShardingKey(ModelClass<?> modelClass,
                                       Sharding.Strategy strategy) {
        final ShardingKey key = new ShardingKey(modelClass,
                                                toIdPredicate(modelClass, strategy));
        return key;
    }

    @Override
    public IdPredicate toIdPredicate(ModelClass<?> modelClass, Strategy strategy) {
        final IdPredicate result = IdPredicate.newBuilder()
                                              .setAllIds(true)
                                              .build();
        return result;
    }
}
