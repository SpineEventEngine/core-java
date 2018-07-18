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

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.aggregate.IgTestAggregate;
import io.spine.server.aggregate.given.aggregate.IgTestAggregateRepository;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.DuplicateCommandException;
import io.spine.test.aggregate.ProjectId;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.core.CommandEnvelope.of;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.command;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.createProject;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.newProjectId;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.newTenantId;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.startProject;
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
        ModelTests.clearModel();
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

    @Test
    @DisplayName("throw DuplicateCommandException when command was handled since last snapshot")
    void throwExceptionForDuplicateCommand() {
        TenantId tenantId = newTenantId();
        ProjectId projectId = newProjectId();
        Command createCommand = command(createProject(projectId), tenantId);

        CommandBus commandBus = boundedContext.getCommandBus();
        StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);

        IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);
        IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        assertThrows(DuplicateCommandException.class, () -> guard.check(of(createCommand)));
    }

    @Test
    @DisplayName("not throw exception when command was handled but snapshot was made")
    void notThrowForCommandHandledAfterSnapshot() {
        repository.setSnapshotTrigger(1);

        TenantId tenantId = newTenantId();
        ProjectId projectId = newProjectId();
        Command createCommand = command(createProject(projectId), tenantId);

        CommandBus commandBus = boundedContext.getCommandBus();
        StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);

        IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);

        IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(createCommand));
    }

    @Test
    @DisplayName("not throw exception if command was not handled")
    void notThrowForCommandNotHandled() {
        TenantId tenantId = newTenantId();
        ProjectId projectId = newProjectId();
        Command createCommand = command(createProject(projectId), tenantId);
        IgTestAggregate aggregate = new IgTestAggregate(projectId);

        IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(createCommand));
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

        IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(startCommand));
    }
}
