/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandValidationError;
import io.spine.core.Rejection;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.event.EventBus;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.event.CmdTaskAdded;
import io.spine.test.reflect.InvalidProjectName;
import io.spine.test.reflect.ProjectId;
import org.junit.Test;

import static io.spine.core.CommandValidationError.INVALID_COMMAND;
import static io.spine.core.CommandValidationError.TENANT_INAPPLICABLE;
import static io.spine.core.Rejections.toRejection;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.server.tenant.TenantAwareOperation.isTenantSet;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
        commandBus.post(newCommandWithoutTenantId(), observer);

        assertFalse(isTenantSet());
    }

    @Test
    public void reject_invalid_command() {
        final Command cmd = newCommandWithoutContext();

        commandBus.post(cmd, observer);

        checkCommandError(observer.firstResponse(),
                          INVALID_COMMAND,
                          CommandValidationError.getDescriptor().getFullName(),
                          cmd);
    }

    @Test
    public void reject_multitenant_command_in_single_tenant_context() {
        // Create a multi-tenant command.
        final Command cmd = createProject();

        commandBus.post(cmd, observer);

        checkCommandError(observer.firstResponse(),
                          TENANT_INAPPLICABLE,
                          InvalidCommandException.class,
                          cmd);
    }

    @Test
    public void propagate_rejections_to_rejection_bus() {
        final FaultyHandler faultyHandler = new FaultyHandler(eventBus);
        commandBus.register(faultyHandler);

        final Command addTaskCommand = clearTenantId(addTask());
        final MemoizingObserver<Ack> observer = memoizingObserver();
        commandBus.post(addTaskCommand, observer);

        final InvalidProjectName throwable = faultyHandler.getThrowable();
        final Rejection expectedRejection = toRejection(throwable, addTaskCommand);
        final Ack ack = observer.firstResponse();
        final Rejection actualRejection = ack.getStatus()
                                             .getRejection();
        assertTrue(isNotDefault(actualRejection));
        assertEquals(unpack(expectedRejection.getMessage()), unpack(actualRejection.getMessage()));
    }

    @Test
    public void create_validator_once() {
        final EnvelopeValidator<CommandEnvelope> validator = commandBus.getValidator();
        assertNotNull(validator);
        assertSame(validator, commandBus.getValidator());
    }

    @Override
    protected Command newCommand() {
        final Message commandMessage = Given.CommandMessage.createProjectMessage();
        return TestActorRequestFactory.newInstance(SingleTenantCommandBusShould.class)
                                      .createCommand(commandMessage);
    }

    /**
     * A {@code CommandHandler}, which throws a rejection upon a command.
     */
    private static class FaultyHandler extends CommandHandler {

        private final InvalidProjectName rejection =
                new InvalidProjectName(ProjectId.getDefaultInstance());

        private FaultyHandler(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("unused")     // does nothing, but throws a rejection.
        @Assign
        CmdTaskAdded handle(CmdAddTask msg, CommandContext context) throws InvalidProjectName {
            throw rejection;
        }

        private InvalidProjectName getThrowable() {
            return rejection;
        }
    }
}
