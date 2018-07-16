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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.server.transport.TransportFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An implementation of the sharding service, that manages all the data in-memory and runs only
 * within the current JVM process.
 *
 * <p>Must not be used in production mode. Designed primarily for local testing purposes.
 *
 * @author Alex Tymchenko
 */
public class InProcessSharding implements Sharding {

    private final TransportFactory transportFactory;
    private final ShardingRegistry registry = new ShardingRegistry();

    /**
     * Creates a new instance of in-process sharding service, which uses the given
     * transport factory to manage the channels for the sharded message streams.
     *
     * @param transportFactory the transport factory to use
     */
    public InProcessSharding(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void register(Shardable shardable) {
        Iterable<ShardedStreamConsumer<?, ?>> consumers = shardable.getMessageConsumers();
        if (!consumers.iterator()
                      .hasNext()) {
            throw newIllegalArgumentException("Cannot register the shardable with no consumers: %s",
                                              shardable);
        }
        BoundedContextName bcName = shardable.getBoundedContextName();
        Set<ShardingKey> keys = obtainKeys(shardable);

        for (ShardingKey key : keys) {
            Set<ShardedStream<?, ?, ?>> streams = bindWithKey(consumers, bcName, key);
            registry.register(shardable.getShardingStrategy(), streams);
        }
    }

    /**
     *
     * @param shardable a shardable instance to unregister
     */
    @Override
    public final void unregister(Shardable shardable) {
        Iterable<ShardedStreamConsumer<?, ?>> consumers = shardable.getMessageConsumers();
        for (ShardedStreamConsumer<?, ?> consumer : consumers) {
            registry.unregister(consumer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ShardingKey> pickKeysForNode(Shardable shardable, Set<ShardingKey> keys) {
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(DeliveryTag<E> tag, I targetId) {
        Set<ShardedStream<I, ?, E>> result = registry.find(tag, targetId);
        return result;
    }

    private Set<ShardingKey> obtainKeys(Shardable shardable) {
        Set<ShardIndex> allIndexes = shardable.getShardingStrategy()
                                              .allIndexes();

        ImmutableSet.Builder<ShardingKey> keySetBuilder = ImmutableSet.builder();
        for (ShardIndex shardIndex : allIndexes) {
            ShardingKey key = new ShardingKey(shardable.getShardedModelClass(),
                                              shardIndex);
            keySetBuilder.add(key);
        }
        Set<ShardingKey> allKeys = keySetBuilder.build();

        Set<ShardingKey> keysForThisNode = pickKeysForNode(shardable, allKeys);
        return keysForThisNode;
    }

    private Set<ShardedStream<?, ?, ?>>
    bindWithKey(Iterable<ShardedStreamConsumer<?, ?>> consumers,
                BoundedContextName bcName,
                ShardingKey shardingKey) {
        TransportBindFn fn = new TransportBindFn(bcName, shardingKey,
                                                 transportFactory);
        ImmutableSet<ShardedStream<?, ?, ?>> result = FluentIterable.from(consumers)
                                                                    .transform(fn)
                                                                    .toSet();
        return result;
    }

    /**
     * A function which takes a stream consumer as an argument and initializes it with the
     * transport.
     */
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
            ShardedStream<?, ?, ?> result = consumer.bindToTransport(bcName,
                                                                     shardingKey,
                                                                     transportFactory);
            return result;
        }
    }
}
