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

package io.spine.server.aggregate;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.aggregate.IgTestAggregate;
import io.spine.server.aggregate.given.aggregate.IgTestAggregateRepository;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.event.DuplicateEventException;
import io.spine.server.event.EventBus;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.test.aggregate.ProjectId;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.command;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.createProject;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.event;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.newProjectId;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.projectPaused;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.startProject;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.taskStarted;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("IdempotencyGuard should")
class IdempotencyGuardTest {

    private BoundedContext boundedContext;
    private IgTestAggregateRepository repository;
    private ProjectId projectId;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder().build();
        repository = new IgTestAggregateRepository();
        boundedContext.register(repository);
        projectId = newProjectId();
    }

    @AfterEach
    void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Nested
    @DisplayName("check commands and")

    class Commands {
        private IgTestAggregate aggregate() {
            return repository.loadAggregate(projectId);
        }

        @Test
        @DisplayName("throw DuplicateCommandException when command was handled since last snapshot")
        void throwExceptionForDuplicateCommand() {
            Command createCommand = command(createProject(projectId));

            post(createCommand);

            IgTestAggregate aggregate = repository.loadAggregate(projectId);
            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            assertThrows(DuplicateCommandException.class, () -> check(guard, createCommand));
        }

        @Test
        @DisplayName("not throw exception when command was handled but snapshot was made")
        void notThrowForCommandHandledAfterSnapshot() {
            repository.setSnapshotTrigger(1);
            Command createCommand = command(createProject(projectId));
            post(createCommand);

            IgTestAggregate aggregate = aggregate();

            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            check(guard, createCommand);
        }

        @Test
        @DisplayName("not throw exception if command was not handled")
        void notThrowForCommandNotHandled() {
            Command createCommand = command(createProject(projectId));
            IgTestAggregate aggregate = new IgTestAggregate(projectId);

            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            guard.check(CommandEnvelope.of(createCommand));
        }

        @Test
        @DisplayName("not throw exception if another command was handled")
        void notThrowIfAnotherCommandHandled() {
            Command createCommand = command(createProject(projectId));
            Command startCommand = command(startProject(projectId));

            post(createCommand);

            IgTestAggregate aggregate = aggregate();

            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            check(guard, startCommand);
        }

        private void post(Command command) {
            CommandBus commandBus = boundedContext.getCommandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(command, noOpObserver);
        }

        private void check(IdempotencyGuard guard, Command command) {
            CommandEnvelope envelope = CommandEnvelope.of(command);
            guard.check(envelope);
        }
    }

    @Nested
    @DisplayName("check events and")
    class Events {

        @BeforeEach
        void setUp() {
            boundedContext.getCommandBus()
                          .post(command(createProject(projectId)), noOpObserver());
        }

        @Test
        @DisplayName("throw DuplicateEventException when event was handled since last snapshot")
        void throwExceptionForDuplicateCommand() {
            Event event = event(taskStarted(projectId));
            post(event);

            IgTestAggregate aggregate = repository.loadAggregate(projectId);
            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            assertThrows(DuplicateEventException.class, () -> check(guard, event));
        }

        @Test
        @DisplayName("not throw exception when event was handled but snapshot was made")
        void notThrowForCommandHandledAfterSnapshot() {
            repository.setSnapshotTrigger(1);

            Event event = event(taskStarted(projectId));
            post(event);

            IgTestAggregate aggregate = repository.loadAggregate(projectId);
            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            check(guard, event);
        }

        @Test
        @DisplayName("not throw exception if event was not handled")
        void notThrowForCommandNotHandled() {
            Event event = event(taskStarted(projectId));
            IgTestAggregate aggregate = new IgTestAggregate(projectId);

            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            check(guard, event);
        }

        @Test
        @DisplayName("not throw exception if another event was handled")
        void notThrowIfAnotherCommandHandled() {
            Event taskEvent = event(taskStarted(projectId));
            Event projectEvent = event(projectPaused(projectId));

            EventBus eventBus = boundedContext.getEventBus();
            eventBus.post(taskEvent);

            IgTestAggregate aggregate = repository.loadAggregate(projectId);

            IdempotencyGuard guard = new IdempotencyGuard(aggregate);
            check(guard, projectEvent);
        }

        private void post(Event event) {
            boundedContext.getEventBus()
                          .post(event);
        }

        private void check(IdempotencyGuard guard, Event event) {
            EventEnvelope envelope = EventEnvelope.of(event);
            guard.check(envelope);
        }
    }
}
