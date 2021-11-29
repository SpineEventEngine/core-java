/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.aggregate.given.repo.AggregateWithLifecycle;
import io.spine.server.aggregate.given.repo.RepoOfAggregateWithLifecycle;
import io.spine.server.test.shared.LongIdAggregate;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`AggregateRepository` views should")
class AggregateRepositoryViewsTest {

    /** The Aggregate ID used in all tests. */
    private static final Long id = 100L;
    private final ActorRequestFactory requestFactory = new TestActorRequestFactory(getClass());
    private BoundedContext context;
    /**
     * The default behaviour of an {@code AggregateRepository}.
     */
    private AggregateRepository<Long, AggregateWithLifecycle, LongIdAggregate> repository;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // It's on purpose for tests.
    private Optional<AggregateWithLifecycle> aggregate;

    @BeforeEach
    void setUp() {
        context = BoundedContextBuilder
                .assumingTests()
                .build();
        repository = new RepoOfAggregateWithLifecycle();
        context.internalAccess()
               .register(repository);

        // Create the aggregate instance.
        postCommand("createCommand");
    }

    /**
     * Creates a command and posts it to {@code CommandBus}
     * for being processed by the repository.
     */
    private void postCommand(String cmd) {
        var command =
                requestFactory.command()
                              .create(RepoOfAggregateWithLifecycle.createCommandMessage(id, cmd));
        context.commandBus()
               .post(command, StreamObservers.noOpObserver());
    }

    @Test
    @DisplayName("find aggregate if no status flags are set")
    void findAggregatesWithNoStatus() {
        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        var agg = aggregate.get();
        assertFalse(agg.isArchived());
        assertFalse(agg.isDeleted());
    }

    @Test
    @DisplayName("find aggregates with `archived` status")
    void findArchivedAggregates() {
        postCommand("archive");

        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        var agg = aggregate.get();
        assertTrue(agg.isArchived());
        assertFalse(agg.isDeleted());
    }

    @Test
    @DisplayName("find aggregates with `deleted` status")
    void findDeletedAggregates() {
        postCommand("delete");

        aggregate = repository.find(id);

        assertTrue(aggregate.isPresent());
        var agg = aggregate.get();
        assertFalse(agg.isArchived());
        assertTrue(agg.isDeleted());
    }
}
