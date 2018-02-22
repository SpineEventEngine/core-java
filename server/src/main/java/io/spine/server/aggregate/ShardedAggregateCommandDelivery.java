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

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.SPI;
import io.spine.core.CommandEnvelope;
import io.spine.server.ServerEnvironment;
import io.spine.server.sharding.Shard;
import io.spine.server.sharding.ShardedMessage;
import io.spine.server.sharding.ShardedMessages;
import io.spine.server.sharding.Sharding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static io.spine.protobuf.TypeConverter.toMessage;
import static java.lang.String.format;

/**
 * An abstract base for {@code Aggregate} {@linkplain AggregateCommandDelivery command
 * delivery strategies}, which use {@linkplain io.spine.server.transport.MessageChannel channels}
 * to bring command messages to the aggregate instances.
 *
 * <p>The implementations of this class provide their strategy on how to define the channel IDs
 * per message per target entity ID.
 *
 * @author Alex Tymchenko
 */
@SPI
public abstract class ShardedAggregateCommandDelivery<I, A extends Aggregate<I, ?, ?>>
        extends AggregateCommandDelivery<I, A> {

    protected ShardedAggregateCommandDelivery(AggregateRepository<I, A> repository) {
        super(repository);
        final Shard shard = sharding().ofDestination(repository);
        shard.addConsumer(new Consumer());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always postpone the message from the immediate delivery and forward them to one or more
     * channels instead.
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

    private void sendToShards(I id, CommandEnvelope envelope) {
        final Message commandMessage = envelope.getMessage();
        final Set<Shard> shards = sharding().find(id, commandMessage);

        final Message idAsMessage = toMessage(id);
        final ShardedMessage shardedMessage = ShardedMessages.of(idAsMessage, envelope);

        for (Shard shard : shards) {
            shard.post(shardedMessage);
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

    private class Consumer implements StreamObserver<ShardedMessage> {

        @Override
        public void onNext(ShardedMessage value) {
            final CommandEnvelope commandEnvelope = ShardedMessages.getCommandEnvelope(value);
            final I targetId = ShardedMessages.getTargetId(value, repository().getIdClass());
            deliverNow(targetId, commandEnvelope);
        }

        @Override
        public void onError(Throwable t) {
            final String errorMsg = format("Unexpected error consuming the sharded messages. " +
                                                   "Repository: %s", repository().getClass());
            log().error(errorMsg, t);
        }

        @Override
        public void onCompleted() {
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ShardedAggregateCommandDelivery.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
