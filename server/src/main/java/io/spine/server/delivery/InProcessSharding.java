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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.SPI;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.server.sharding.ShardIndex;
import io.spine.server.transport.TransportFactory;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
public class InProcessSharding implements Sharding {

    private final TransportFactory transportFactory;
    private final ShardingRegistry registry = new ShardingRegistry();

    public InProcessSharding(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public final void register(Shardable shardable) {
        final Iterable<ShardedStreamConsumer<?, ?>> consumers = shardable.getMessageConsumers();
        if (!consumers.iterator()
                      .hasNext()) {
            return;
        }
        final BoundedContextName bcName = shardable.getBoundedContextName();
        final Set<ShardingKey> keys = obtainKeys(shardable);

        for (ShardingKey key : keys) {
            final Set<ShardedStream<?, ?, ?>> streams = bindWithKey(consumers, bcName, key);
            registry.register(shardable.getShardingStrategy(), streams);
        }
    }

    @Override
    public final void unregister(Shardable shardable) {
        final Iterable<ShardedStreamConsumer<?, ?>> consumers = shardable.getMessageConsumers();
        for (ShardedStreamConsumer<?, ?> consumer : consumers) {
            registry.unregister(consumer);
        }
    }

    @SPI
    @Override
    public Set<ShardingKey> pickKeysForNode(Shardable shardable, Set<ShardingKey> keys) {
        return keys;
    }

    @SPI
    @Override
    public <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(DeliveryTag<E> tag, I targetId) {
        final Set<ShardedStream<I, ?, E>> result = registry.find(tag, targetId);
        return result;
    }

    private Set<ShardingKey> obtainKeys(Shardable shardable) {
        final Set<ShardIndex> allIndexes = shardable.getShardingStrategy()
                                                    .allIndexes();

        final ImmutableSet.Builder<ShardingKey> keySetBuilder = ImmutableSet.builder();
        for (ShardIndex shardIndex : allIndexes) {
            final ShardingKey key = new ShardingKey(shardable.getShardedModelClass(),
                                                    shardIndex);
            keySetBuilder.add(key);
        }
        final Set<ShardingKey> allKeys = keySetBuilder.build();

        final Set<ShardingKey> keysForThisNode = pickKeysForNode(shardable, allKeys);
        return keysForThisNode;
    }

    private Set<ShardedStream<?, ?, ?>>
    bindWithKey(final Iterable<ShardedStreamConsumer<?, ?>> consumers,
                final BoundedContextName bcName,
                final ShardingKey shardingKey) {
        final TransportBindFn fn = new TransportBindFn(bcName, shardingKey,
                                                       transportFactory);
        final ImmutableSet<ShardedStream<?, ?, ?>> result = FluentIterable.from(consumers)
                                                                          .transform(fn)
                                                                          .toSet();
        return result;
    }

    private static final class TransportBindFn
            implements Function<ShardedStreamConsumer<?, ?>, ShardedStream<?, ?, ?>> {

        private final BoundedContextName bcName;
        private final ShardingKey shardingKey;
        private final TransportFactory transportFactory;

        private TransportBindFn(BoundedContextName bcName,
                                ShardingKey shardingKey,
                                TransportFactory transportFactory) {
            this.bcName = bcName;
            this.shardingKey = shardingKey;
            this.transportFactory = transportFactory;
        }

        @Override
        public ShardedStream<?, ?, ?> apply(@Nullable ShardedStreamConsumer<?, ?> consumer) {
            checkNotNull(
                    consumer);
            final ShardedStream<?, ?, ?> result = consumer.bindToTransport(bcName,
                                                                           shardingKey,
                                                                           transportFactory);
            return result;
        }
    }
}
