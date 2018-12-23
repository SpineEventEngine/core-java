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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.spine.core.MessageEnvelope;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * The registry of sharded message streams.
 */
final class ShardingRegistry {

    private final Multimap<DeliveryTag, Entry> entries =
            synchronizedMultimap(HashMultimap.create());

    void register(ShardingStrategy strategy, Set<ShardedStream<?, ?, ?>> streams) {
        for (ShardedStream<?, ?, ?> stream : streams) {
            Entry entry = new Entry(strategy, stream);
            DeliveryTag tag = stream.getTag();
            entries.put(tag, entry);
        }
    }

    void unregister(ShardedStreamConsumer streamConsumer) {
        DeliveryTag tag = streamConsumer.getTag();
        Collection<Entry> entriesForTag = entries.get(tag);
        for (Entry entry : entriesForTag) {
            entry.stream.close();
        }
        entries.removeAll(tag);
    }

    <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(DeliveryTag tag, I targetId) {

        Collection<Entry> entriesForTag = entries.get(tag);

        ImmutableSet.Builder<ShardedStream<I, ?, E>> builder = ImmutableSet.builder();
        for (Entry entry : entriesForTag) {

            ShardIndex shardIndex = entry.strategy.indexForTarget(targetId);
            ShardIndex entryIndex = entry.stream.getKey()
                                                .getIndex();
            if(shardIndex.equals(entryIndex)) {
                @SuppressWarnings("unchecked") //TODO:2018-12-23:alexander.yevsyukov: Document why the cast is safe.
                ShardedStream<I, ?, E> stream = (ShardedStream<I, ?, E>) entry.stream;
                builder.add(stream);
            }
        }

        return builder.build();
    }

    private static class Entry {
        private final ShardingStrategy strategy;
        private final ShardedStream<?, ?, ?> stream;

        private Entry(ShardingStrategy strategy, ShardedStream<?, ?, ?> stream) {
            this.strategy = strategy;
            this.stream = stream;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry entry = (Entry) o;
            return Objects.equals(strategy, entry.strategy) &&
                    Objects.equals(stream, entry.stream);
        }

        @Override
        public int hashCode() {
            return Objects.hash(strategy, stream);
        }
    }
}
