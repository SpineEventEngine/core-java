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
package io.spine.server.delivery.given;

import com.google.common.collect.Lists;
import com.google.protobuf.StringValue;
import io.spine.core.BoundedContextName;
import io.spine.core.React;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.command.Assign;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.Shardable;
import io.spine.server.delivery.ShardedStreamConsumer;
import io.spine.server.delivery.Sharding;
import io.spine.server.delivery.ShardingStrategy;
import io.spine.server.delivery.UniformAcrossTargets;
import io.spine.server.entity.model.EntityClass;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCancelled;
import io.spine.test.aggregate.event.AggProjectPaused;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.rejection.AggCannotReassignUnassignedTask;
import io.spine.validate.StringValueVBuilder;

import java.util.Optional;

import static io.spine.core.BoundedContextNames.newName;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;

/**
 * An abstract base for environments, which are created to ease the message delivery testing.
 *
 * @author Alex Tymchenko
 */
public class MessageDeliveryTestEnv {

    /**
     * The time to wait until all the messages dispatched to entities
     * are processed in several threads, in milliseconds.
     *
     * <p>"Sleeping" down the main thread is a simpler choice to ensure the messages were delivered.
     * The alternatives would imply injecting multiple mocks that would send reports
     * down the dispatching route. Which seems to be much more complex.
     */
    private static final int DISPATCH_WAIT_TIME = 3_500;

    /** Prevents instantiation of this test environment class. */
    private MessageDeliveryTestEnv() {
    }

    public static void setShardingTransport(InMemoryTransportFactory transport) {
        Sharding inProcessSharding = new InProcessSharding(transport);
        ServerEnvironment.getInstance()
                         .replaceSharding(inProcessSharding);
    }

    /**
     * The time for the main thread to wait for the messages to finish dispatching in other threads.
     *
     * <p>The value returned should be fine for the most of current cases.
     */
    public static int dispatchWaitTime() {
        return DISPATCH_WAIT_TIME;
    }

    /**
     * An aggregate used to test the {@link io.spine.server.delivery.DeliveryTag DeliveryTag}
     * features.
     */
    public static class DeliveryEqualityProject
            extends Aggregate<ProjectId, StringValue, StringValueVBuilder> {

        protected DeliveryEqualityProject(ProjectId id) {
            super(id);
        }

        @Assign
        AggProjectStarted on(AggStartProject cmd) {
            return AggProjectStarted.getDefaultInstance();
        }

        @Apply
        private void on(AggProjectStarted event) {
            //Do nothing for this test.
        }

        @React
        public Optional<AggProjectCancelled> on(AggProjectCancelled event) {
            return Optional.empty();
        }

        @React
        public Optional<AggProjectPaused> on(AggCannotReassignUnassignedTask rejection) {
            return Optional.empty();
        }
    }

    /**
     * An aggregate repository for{@link io.spine.server.delivery.DeliveryTag DeliveryTag} tests.
     */
    public static class DeliveryEqualityRepository extends AggregateRepository<ProjectId,
            DeliveryEqualityProject> {

        /**
         * Overriding the name of the {@code BoundedContext} to avoid the repository registration
         * in the real bounded context and thus simplify the test environment.
         */
        @Override
        public BoundedContextName getBoundedContextName() {
            return newName("Delivery tests");
        }
    }

    /**
     * A shardable which declares no message consumers.
     */
    public static class EmptyShardable implements Shardable {
        @Override
        public ShardingStrategy getShardingStrategy() {
            return UniformAcrossTargets.singleShard();
        }

        @Override
        public Iterable<ShardedStreamConsumer<?, ?>> getMessageConsumers() {
            return Lists.newArrayList();
        }

        @Override
        public BoundedContextName getBoundedContextName() {
            return newName("EmptyShardables");
        }

        @Override
        public EntityClass getShardedModelClass() {
            Class<DeliveryEqualityProject> someAggregate = DeliveryEqualityProject.class;
            AggregateClass<?> result = asAggregateClass(someAggregate);
            return result;
        }
    }

}
