/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import io.spine.core.CommandEnvelope;
import io.spine.server.ServerEnvironment;
import io.spine.server.sharding.ShardConsumerId;
import io.spine.server.sharding.ShardedStream;
import io.spine.server.sharding.Sharding;

import java.util.Set;

/**
 * An abstract base for {@code Aggregate} {@linkplain AggregateCommandDelivery command
 * delivery strategies}.
 *
 * <p>The delivery strategy uses {@linkplain Sharding.Strategy sharding} as an approach
 * to group command messages and deliver them to the aggregate instances to a single consumer,
 * process them within a single JVM and thus avoid potential concurrent modification.
 *
 * @author Alex Tymchenko
 */
public class ShardedAggregateCommandDelivery<I, A extends Aggregate<I, ?, ?>>
                            extends AggregateCommandDelivery<I, A> {

    protected ShardedAggregateCommandDelivery(AggregateRepository<I, A> repository) {
        super(repository);

    }

    /**
     * {@inheritDoc}
     *
     * <p>Always postpone the message from the immediate delivery and forward them to one or more
     * shards instead.
     *
     * @param id       the ID of the entity the envelope is going to be dispatched.
     * @param envelope the envelope to be dispatched — now or later
     * @return
     */
    @Override
    public boolean shouldPostpone(I id, CommandEnvelope envelope) {
        sendToShards(id, envelope);
        return true;
    }

    private void sendToShards(I targetId, CommandEnvelope envelope) {
        final ShardConsumerId<CommandEnvelope> consumerId = getConsumerId();
        final Set<ShardedStream<I, CommandEnvelope>> streams =
                sharding().find(consumerId, targetId);

        for (ShardedStream<I, CommandEnvelope> shardedStream : streams) {

            shardedStream.post(targetId, envelope);
        }
    }

    /**
     * Obtains the sharding service instance for the current {@link ServerEnvironment server
     * environment}.
     *
     * <p>In order to allow switching to another sharding implementation at runtime, this API
     * element is designed as method, not as a class-level field.
     *
     * @return the instance of sharding service
     */
    private static Sharding sharding() {
        final Sharding result = ServerEnvironment.getInstance()
                                                 .getSharding();
        return result;
    }
}
