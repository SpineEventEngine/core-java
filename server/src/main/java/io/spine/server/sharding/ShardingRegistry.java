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
import io.spine.core.MessageEnvelope;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * @author Alex Tymchenko
 */
final class ShardingRegistry {

    private final Multimap<ShardingTag, ShardedStream<?, ?, ?>> streams =
            synchronizedMultimap(HashMultimap.<ShardingTag, ShardedStream<?, ?, ?>>create());

    void register(ShardedStreamConsumer streamConsumer) {
        final ShardingTag tag = streamConsumer.getTag();
        final ShardedStream stream = streamConsumer.getStream();
        streams.put(tag, stream);
    }

    void unregister(ShardedStreamConsumer streamConsumer) {
        final ShardingTag tag = streamConsumer.getTag();
        final ShardedStream stream = streamConsumer.getStream();
        streams.remove(tag, stream);
    }

    <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(final ShardingTag<E> id, final I targetId) {

        final Collection<ShardedStream<?, ?, ?>> shardedStreams = streams.get(id);

        final ImmutableSet.Builder<ShardedStream<I, ?, E>> builder = ImmutableSet.builder();
        for (ShardedStream<?, ?, ?> shardedStream : shardedStreams) {
            final boolean idMatches = shardedStream.getKey()
                                                   .applyToId(targetId);
            if (idMatches) {
                final ShardedStream<I, ?, E> result = (ShardedStream<I, ?, E>) shardedStream;
                builder.add(result);
            }
        }

        return builder.build();
    }
}
