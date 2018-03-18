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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.spine.annotation.SPI;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.server.entity.EntityClass;
import io.spine.server.transport.TransportFactory;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * @author Alex Tymchenko
 */
public class InProcessSharding implements Sharding {

    private final TransportFactory transportFactory;
    private final ShardedStreamRegistry registry = new ShardedStreamRegistry();

    public InProcessSharding(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public final void register(Shardable<?> shardable) throws NoShardAvailableException {
        final Iterable<ShardedStreamConsumer<?, ?>> consumers = shardable.getMessageConsumers();
        if (!consumers.iterator()
                      .hasNext()) {
            return;
        }
        final BoundedContextName bcName = shardable.getBoundedContextName();
        final ShardingKey shardingKey = getShardingKey(shardable.getShardedModelClass(),
                                                       shardable.getShardingStrategy());
        for (ShardedStreamConsumer<?, ?> consumer : consumers) {
            consumer.bindToTransport(bcName, shardingKey, transportFactory);
            registry.register(consumer);
        }
    }

    @Override
    public final void unregister(Shardable<?> shardable) {
        final Iterable<ShardedStreamConsumer<?, ?>> consumers = shardable.getMessageConsumers();
        for (ShardedStreamConsumer<?, ?> consumer : consumers) {
            consumer.close();
            registry.unregister(consumer);
        }

    }

    @SPI
    @Override
    public <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(ShardingTag<E> tag, I targetId) throws NoShardAvailableException {
        final Set<ShardedStream<I, ?, E>> result = registry.find(tag, targetId);
        return result;
    }

    private ShardingKey getShardingKey(EntityClass<?> modelClass,
                                       Sharding.Strategy strategy) {
        final ShardingKey key = new ShardingKey(modelClass,
                                                toIdPredicate(modelClass, strategy));
        return key;
    }

    @SPI
    @Override
    public IdPredicate toIdPredicate(EntityClass<?> entityClass, Strategy strategy) {
        final IdPredicate result = IdPredicate.newBuilder()
                                              .setAllIds(true)
                                              .build();
        return result;
    }

    private static final class ShardedStreamRegistry {

        private final Multimap<ShardingTag, ShardedStream<?, ?, ?>> streams =
                synchronizedMultimap(HashMultimap.<ShardingTag, ShardedStream<?, ?, ?>>create());

        private void register(ShardedStreamConsumer streamConsumer) {
            final ShardingTag tag = streamConsumer.getTag();
            final ShardedStream stream = streamConsumer.getStream();
            streams.put(tag, stream);
        }

        private void unregister(ShardedStreamConsumer streamConsumer) {
            final ShardingTag tag = streamConsumer.getTag();
            final ShardedStream stream = streamConsumer.getStream();
            streams.remove(tag, stream);
        }

        private <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
        find(final ShardingTag<E> id, final I targetId) {

            final Collection<ShardedStream<?, ?, ?>> shardedStreams = streams.get(id);

            final ImmutableSet.Builder<ShardedStream<I, ?, E>> builder = ImmutableSet.builder();
            for (ShardedStream<?, ?, ?> shardedStream : shardedStreams) {
                final boolean idMatches = shardedStream.getKey()
                                               .applyToId(targetId);
                if(idMatches) {
                    final ShardedStream<I, ?, E> result = (ShardedStream<I, ?, E>) shardedStream;
                    builder.add(result);
                }
            }

            return builder.build();
        }
    }
}
