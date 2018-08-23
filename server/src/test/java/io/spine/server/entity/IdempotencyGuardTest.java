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

package io.spine.server.entity;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.server.entity.given.IgTestAggregate;
import io.spine.server.entity.given.IgTestAggregateRepository;
import io.spine.server.event.DuplicateEventException;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.test.entity.event.EntTaskRenamed;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.addTask;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.command;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.createProject;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.event;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.newProjectId;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.newTaskId;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.newTenantId;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.startProject;
import static io.spine.server.entity.given.IdempotencyGuardTestEnv.taskRenamed;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("IdempotencyGuard should")
class IdempotencyGuardTest {

    private BoundedContext boundedContext;
    private IgTestAggregateRepository repository;

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        repository = new IgTestAggregateRepository();
        boundedContext.register(repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Nested
    @DisplayName("check commands and")
    class Commands {

        @Test
        @DisplayName("throw DuplicateCommandException when a command was handled recently")
        void throwExceptionForDuplicateCommand() {
            TenantId tenantId = newTenantId();
            ProjectId projectId = newProjectId();
            Command createCommand = command(createProject(projectId), tenantId);

            CommandBus commandBus = boundedContext.getCommandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);

            IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);
            IdempotencyGuard guard = aggregate.idempotencyGuard();
            assertThrows(DuplicateCommandException.class,
                         () -> guard.check(CommandEnvelope.of(createCommand)));
        }

        @Test
        @DisplayName("not throw exception when command was handled but snapshot was made")
        void notThrowForCommandHandledAfterSnapshot() {
            Repositories.setIdempotencyLimits(repository, 1, 1);

            TenantId tenantId = newTenantId();
            ProjectId projectId = newProjectId();
            Command createCommand = command(createProject(projectId), tenantId);
            Command startCommand = command(startProject(projectId), tenantId);

            CommandBus commandBus = boundedContext.getCommandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);
            commandBus.post(startCommand, noOpObserver);

            IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);

            IdempotencyGuard guard = aggregate.idempotencyGuard();
            guard.check(CommandEnvelope.of(createCommand));
        }

        @Test
        @DisplayName("not throw exception if command was not handled")
        void notThrowForCommandNotHandled() {
            TenantId tenantId = newTenantId();
            ProjectId projectId = newProjectId();
            Command createCommand = command(createProject(projectId), tenantId);
            IgTestAggregate aggregate = new IgTestAggregate(projectId);

            IdempotencyGuard guard = aggregate.idempotencyGuard();
            guard.check(CommandEnvelope.of(createCommand));
        }

        @Test
        @DisplayName("not throw exception if another command was handled")
        void notThrowIfAnotherCommandHandled() {
            TenantId tenantId = newTenantId();
            ProjectId projectId = newProjectId();
            Command createCommand = command(createProject(projectId), tenantId);
            Command startCommand = command(startProject(projectId), tenantId);

            CommandBus commandBus = boundedContext.getCommandBus();
            StreamObserver<Ack> noOpObserver = noOpObserver();
            commandBus.post(createCommand, noOpObserver);

            IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);

            IdempotencyGuard guard = aggregate.idempotencyGuard();
            guard.check(CommandEnvelope.of(startCommand));
        }
    }

    @Nested
    @DisplayName("check events and")
    class Events {

        private ProjectId projectId;
        private TaskId taskId;
        private TenantId tenantId;

        @BeforeEach
        void setUp() {
            projectId = newProjectId();
            taskId = newTaskId();
            tenantId = newTenantId();
            Command createCommand = command(createProject(projectId), tenantId);

            CommandBus commandBus = boundedContext.getCommandBus();
            commandBus.post(createCommand, noOpObserver());
            commandBus.post(command(addTask(projectId, taskId), tenantId), noOpObserver());
        }

        @Test
        @DisplayName("throw DuplicateEventException when an event was handled recently")
        void throwExceptionForDuplicateEvent() {
            EntTaskRenamed eventMessage = taskRenamed(taskId, "New fancy name", projectId);
            Event taskRenamed = event(eventMessage, tenantId);
            boundedContext.getEventBus()
                          .post(taskRenamed);
            IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);
            IdempotencyGuard guard = aggregate.idempotencyGuard();
            assertThrows(DuplicateEventException.class,
                         () -> guard.check(EventEnvelope.of(taskRenamed)));
        }

        @Test
        @DisplayName("not throw exception if event was not handled")
        void notThrowForCommandNotHandled() {
            EntTaskRenamed eventMessage = taskRenamed(taskId, "Completely new name", projectId);
            Event taskRenamed = event(eventMessage, tenantId);

            IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);
            IdempotencyGuard guard = aggregate.idempotencyGuard();
            guard.check(EventEnvelope.of(taskRenamed));
        }

        @Test
        @DisplayName("not throw exception if another event was handled")
        void notThrowIfAnotherCommandHandled() {
            EntTaskRenamed eventMessage = taskRenamed(taskId, "Not new name", projectId);
            Event event = event(eventMessage, tenantId);
            Event anotherEvent = event(eventMessage, tenantId);
            boundedContext.getEventBus()
                          .post(event);
            IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);
            IdempotencyGuard guard = aggregate.idempotencyGuard();
            guard.check(EventEnvelope.of(anotherEvent));
        }
    }
}
