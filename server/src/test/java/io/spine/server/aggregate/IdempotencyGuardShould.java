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
import io.spine.server.model.ModelTests;
import io.spine.test.aggregate.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.spine.core.CommandEnvelope.of;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.command;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.createProject;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.newProjectId;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.newTenantId;
import static io.spine.server.aggregate.given.IdempotencyGuardTestEnv.startProject;

/**
 * @author Mykhailo Drachuks
 */
public class IdempotencyGuardShould {

    private BoundedContext boundedContext;
    private IgTestAggregateRepository repository;

    @Before
    public void setUp() {
        ModelTests.clearModel();
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();

        repository = new IgTestAggregateRepository();
        boundedContext.register(repository);
    }
    
    @After
    public void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Test(expected = DuplicateCommandException.class)
    public void throw_DuplicateCommandException_when_the_command_was_handled_since_last_snapshot() {
        final TenantId tenantId = newTenantId();
        final ProjectId projectId = newProjectId();
        final Command createCommand = command(createProject(projectId), tenantId);

        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);

        final IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);
        final IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(createCommand));
    }

    @Test
    public void not_throw_when_the_command_was_handled_but_the_snapshot_was_made() {
        repository.setSnapshotTrigger(1);

        final TenantId tenantId = newTenantId();
        final ProjectId projectId = newProjectId();
        final Command createCommand = command(createProject(projectId), tenantId);

        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);

        final IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);

        final IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(createCommand));
    }

    @Test
    public void not_throw_if_the_command_was_not_handled() {
        final TenantId tenantId = newTenantId();
        final ProjectId projectId = newProjectId();
        final Command createCommand = command(createProject(projectId), tenantId);
        final IgTestAggregate aggregate = new IgTestAggregate(projectId);

        final IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(createCommand));
    }

    @Test
    public void not_throw_if_another_command_was_handled() {
        final TenantId tenantId = newTenantId();
        final ProjectId projectId = newProjectId();
        final Command createCommand = command(createProject(projectId), tenantId);
        final Command startCommand = command(startProject(projectId), tenantId);

        final CommandBus commandBus = boundedContext.getCommandBus();
        final StreamObserver<Ack> noOpObserver = noOpObserver();
        commandBus.post(createCommand, noOpObserver);

        final IgTestAggregate aggregate = repository.loadAggregate(tenantId, projectId);

        final IdempotencyGuard guard = new IdempotencyGuard(aggregate);
        guard.check(of(startCommand));
    }


}
