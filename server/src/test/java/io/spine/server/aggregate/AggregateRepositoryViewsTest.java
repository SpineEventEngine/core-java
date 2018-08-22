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

package io.spine.server.aggregate;

import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Command;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.aggregate.given.AggregateRepositoryViewTestEnv.AggregateWithLifecycle;
import io.spine.server.aggregate.given.AggregateRepositoryViewTestEnv.RepoOfAggregateWithLifecycle;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.Sharding;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.test.aggregate.command.AggCancelTask;
import io.spine.test.aggregate.command.AggCompleteTask;
import io.spine.test.aggregate.command.AggCreateTask;
import io.spine.test.aggregate.task.AggTaskId;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AggregateRepository views should")
class AggregateRepositoryViewsTest {

    /** The Aggregate ID used in all tests */
    private final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private AggregateWithLifecycle aggregate;
    private BoundedContext boundedContext;
    private AggregateRepository<AggTaskId, AggregateWithLifecycle> repository;
    private AggTaskId id;

    @BeforeEach
    void setUp() {
        ServerEnvironment serverEnvironment = ServerEnvironment.getInstance();
        Sharding sharding = new InProcessSharding(InMemoryTransportFactory.newInstance());
        serverEnvironment.replaceSharding(sharding);

        id = AggTaskId
                .newBuilder()
                .setId(newUuid())
                .build();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new RepoOfAggregateWithLifecycle();
        boundedContext.register(repository);

        // Create the aggregate instance.
        AggCreateTask createCommand = AggCreateTask
                .newBuilder()
                .setTaskId(id)
                .build();
        postCommand(createCommand);
    }

    /**
     * Creates a command and posts it to {@code CommandBus}
     * for being processed by the repository.
     */
    private void postCommand(Message commandMessage) {
        Command command = requestFactory.command()
                                        .create(commandMessage);
        boundedContext.getCommandBus()
                      .post(command, noOpObserver());
    }

    @Test
    @DisplayName("find aggregate if no status flags are set")
    void findAggregatesWithNoStatus() {
        ensureAggregate();

        assertFalse(aggregate.isArchived());
        assertFalse(aggregate.isDeleted());
    }

    @Test
    @DisplayName("find aggregates with `archived` status")
    void findArchivedAggregates() {
        AggCompleteTask complete = AggCompleteTask
                .newBuilder()
                .setTaskId(id)
                .build();
        postCommand(complete);
        ensureAggregate();

        assertTrue(aggregate.isArchived());
        assertFalse(aggregate.isDeleted());
    }

    @Test
    @DisplayName("find aggregates with `deleted` status")
    void findDeletedAggregates() {
        AggCancelTask cancel = AggCancelTask
                .newBuilder()
                .setTaskId(id)
                .build();
        postCommand(cancel);

        ensureAggregate();

        assertFalse(aggregate.isArchived());
        assertTrue(aggregate.isDeleted());
    }

    private void ensureAggregate() {
        aggregate = repository.find(id)
                              .orElseGet(Assertions::fail);
    }
}
