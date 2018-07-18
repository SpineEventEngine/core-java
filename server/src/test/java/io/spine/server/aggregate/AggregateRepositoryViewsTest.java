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

import io.spine.client.ActorRequestFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateRepositoryViewTestEnv.AggregateWithLifecycle;
import io.spine.server.aggregate.given.AggregateRepositoryViewTestEnv.RepoOfAggregateWithLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AggregateRepository views should")
class AggregateRepositoryViewsTest {

    /** The Aggregate ID used in all tests */
    private static final Long id = 100L;
    private final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());
    private BoundedContext boundedContext;
    /**
     * The default behaviour of an {@code AggregateRepository}.
     */
    private AggregateRepository<Long, AggregateWithLifecycle> repository;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // It's on purpose for tests.
    private Optional<AggregateWithLifecycle> aggregate;

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new RepoOfAggregateWithLifecycle();
        boundedContext.register(repository);

        // Create the aggregate instance.
        postCommand("createCommand");
    }

    /**
     * Creates a command and posts it to {@code CommandBus}
     * for being processed by the repository.
     */
    private void postCommand(String cmd) {
        Command command =
                requestFactory.command()
                              .create(RepoOfAggregateWithLifecycle.createCommandMessage(id, cmd));
        boundedContext.getCommandBus()
                      .post(command, StreamObservers.noOpObserver());
    }

    @Test
    @DisplayName("find aggregate if no status flags are set")
    void findAggregatesWithNoStatus() {
        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        AggregateWithLifecycle agg = aggregate.get();
        assertFalse(agg.isArchived());
        assertFalse(agg.isDeleted());
    }

    @Test
    @DisplayName("find aggregates with `archived` status")
    void findArchivedAggregates() {
        postCommand("archive");

        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        AggregateWithLifecycle agg = aggregate.get();
        assertTrue(agg.isArchived());
        assertFalse(agg.isDeleted());
    }

    @Test
    @DisplayName("find aggregates with `deleted` status")
    void findDeletedAggregates() {
        postCommand("delete");

        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        AggregateWithLifecycle agg = aggregate.get();
        assertFalse(agg.isArchived());
        assertTrue(agg.isDeleted());
    }
}
