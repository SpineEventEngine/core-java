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
package io.spine.server.aggregate;

import io.spine.annotation.SPI;
import io.spine.core.CommandEnvelope;
import io.spine.server.sharding.CommandShardedStream;

/**
 * A strategy on delivering the commands to the instances of a certain aggregate type.
 *
 * @param <I> the ID type of aggregate, to which commands are being delivered
 * @param <A> the type of aggregate
 * @author Alex Tymchenko
 */
@SPI
public abstract class AggregateCommandDelivery<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEndpointDelivery<I, A, CommandEnvelope,
                                        CommandShardedStream<I>, CommandShardedStream.Builder<I>> {

    protected AggregateCommandDelivery(AggregateRepository<I, A> repository) {
        super(repository);
    }

    @Override
    protected AggregateMessageEndpoint<I, A, CommandEnvelope, ?> getEndpoint(
            CommandEnvelope envelope) {
        return AggregateCommandEndpoint.of(repository(), envelope);
    }

    public static <I, A extends Aggregate<I, ?, ?>>
    AggregateCommandDelivery<I, A> directDelivery(AggregateRepository<I, A> repository) {
        return new Direct<>(repository);
    }

    @Override
    protected CommandShardedStream.Builder<I> newShardedStreamBuilder() {
        return CommandShardedStream.newBuilder();
    }

    /**
     * Direct delivery which does not postpone dispatching.
     *
     * @param <I> the type of aggregate IDs
     * @param <A> the type of aggregate
     */
    public static class Direct<I, A extends Aggregate<I, ?, ?>>
            extends AggregateCommandDelivery<I, A> {

        private Direct(AggregateRepository<I, A> repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(I id, CommandEnvelope envelope) {
            return false;
        }
    }
}
