/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.delivery.given;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.SerializableMessage;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.delivery.InboxMessage;
import io.spine.server.delivery.ShardIndex;
import io.spine.server.delivery.ShardObserver;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;

import java.security.SecureRandom;

/**
 * Test environment for {@link Delivery} tests.
 */
public class DeliveryTestEnv {

    /** Prevents instantiation of this utility class. */
    private DeliveryTestEnv() {
    }

    public static ImmutableSet<String> manyTargets(int size) {
        return new SecureRandom().ints(size)
                                 .mapToObj((i) -> "calc-" + i)
                                 .collect(ImmutableSet.toImmutableSet());
    }

    public static ImmutableSet<String> singleTarget() {
        return ImmutableSet.of("the-calculator");
    }

    public static class CalculatorRepository extends AggregateRepository<String, CalcAggregate> {

        @Override
        protected void setupImportRouting(EventRouting<String> routing) {
            routeByFirstField(routing);
        }

        @Override
        protected void setupEventRouting(EventRouting<String> routing) {
            routeByFirstField(routing);
        }

        private static void routeByFirstField(EventRouting<String> routing) {
            routing.replaceDefault(EventRoute.byFirstMessageField(String.class));
        }
    }

    /**
     * An observer of the messages delivered to shards, which remembers all the delivered messages.
     */
    public static class MessageMemoizer implements ShardObserver {

        private final ImmutableSet.Builder<SerializableMessage>
                observedMessages = ImmutableSet.builder();

        @Override
        public void onMessage(InboxMessage update) {
            if (update.hasCommand()) {
                Any packed = update.getCommand()
                                   .getMessage();
                CommandMessage cmdMessage =
                        (CommandMessage) AnyPacker.unpack(packed);
                observedMessages.add(cmdMessage);
            } else {
                Any packed = update.getEvent()
                                   .getMessage();
                EventMessage eventMessage =
                        (EventMessage) AnyPacker.unpack(packed);
                observedMessages.add(eventMessage);
            }
        }

        public ImmutableSet<SerializableMessage> messages() {
            return observedMessages.build();
        }
    }

    /**
     * An observer of the messages delivered to shards, which remembers all the shards involved.
     */
    public static class ShardIndexMemoizer implements ShardObserver {

        private final ImmutableSet.Builder<ShardIndex>
                observedShards = ImmutableSet.builder();

        @Override
        public void onMessage(InboxMessage update) {
            observedShards.add(update.getShardIndex());
        }

        public ImmutableSet<ShardIndex> shards() {
            return observedShards.build();
        }
    }
}
