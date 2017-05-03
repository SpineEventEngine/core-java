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

package org.spine3.server.commandbus;

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Failure;
import org.spine3.base.Response;
import org.spine3.envelope.CommandEnvelope;
import org.spine3.io.StreamObservers;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandHandler;
import org.spine3.server.event.EventBus;
import org.spine3.test.Tests;
import org.spine3.test.command.AddTask;
import org.spine3.test.command.event.TaskAdded;
import org.spine3.test.failure.InvalidProjectName;
import org.spine3.test.failure.ProjectId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.spine3.base.CommandValidationError.INVALID_COMMAND;
import static org.spine3.base.CommandValidationError.TENANT_INAPPLICABLE;
import static org.spine3.server.commandbus.Given.Command.addTask;
import static org.spine3.server.commandbus.Given.Command.createProject;
import static org.spine3.server.tenant.TenantAwareOperation.isTenantSet;

/**
 * @author Alexander Yevsyukov
 */
public class SingleTenantCommandBusShould extends AbstractCommandBusTestSuite {

    public SingleTenantCommandBusShould() {
        super(false);
    }

    @Override
    @Test
    public void setUp() {
        super.setUp();
        commandBus.register(createProjectHandler);
    }

    @Test
    public void post_command_and_do_not_set_current_tenant() {
        commandBus.post(newCommandWithoutTenantId(), responseObserver);

        assertFalse(isTenantSet());
    }

    @Test
    public void reject_invalid_command() {
        final Command cmd = newCommandWithoutContext();

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          INVALID_COMMAND,
                          InvalidCommandException.class,
                          cmd);
        assertTrue(responseObserver.getResponses().isEmpty());
    }

    @Test
    public void reject_multitenant_command_in_single_tenant_context() {
        // Create a multi-tenant command.
        final Command cmd = createProject();

        commandBus.post(cmd, responseObserver);

        checkCommandError(responseObserver.getThrowable(),
                          TENANT_INAPPLICABLE,
                          InvalidCommandException.class,
                          cmd);
        assertTrue(responseObserver.getResponses().isEmpty());
    }

    @Test
    public void do_nothing_in_handleDeadMessage() {
        commandBus.handleDeadMessage(Tests.<CommandEnvelope>nullRef(),
                                     Tests.<StreamObserver<Response>>nullRef());
    }

    @Test
    public void propagate_failures_to_failure_bus() {
        final FaultyHandler faultyHandler = new FaultyHandler(eventBus);
        commandBus.register(faultyHandler);

        final Command addTaskCommand = clearTenantId(addTask());
        commandBus.post(addTaskCommand, StreamObservers.<Response>noOpObserver());

        final InvalidProjectName failureThrowable = faultyHandler.getThrowable();
        final Failure expectedFailure = failureThrowable.toFailure(addTaskCommand);
        verify(failureBus).post(eq(expectedFailure),
                                ArgumentMatchers.<StreamObserver<Response>>any());
    }

    /**
     * A {@code CommandHandler}, which throws a failure upon a command.
     */
    private static class FaultyHandler extends CommandHandler {

        private final InvalidProjectName failure =
                new InvalidProjectName(ProjectId.getDefaultInstance());

        private FaultyHandler(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("unused")     // does nothing, but throws a failure.
        @Assign
        TaskAdded handle(AddTask msg, CommandContext context) throws InvalidProjectName {
            throw failure;
        }

        private InvalidProjectName getThrowable() {
            return failure;
        }
    }
}
