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

package io.spine.server.commandbus;

import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandValidationError;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.commandbus.given.SingleTenantCommandBusTestEnv.CommandPostingHandler;
import io.spine.server.commandbus.given.SingleTenantCommandBusTestEnv.FaultyHandler;
import io.spine.test.command.FirstCmdCreateProject;
import io.spine.test.command.SecondCmdStartProject;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.core.CommandValidationError.INVALID_COMMAND;
import static io.spine.core.CommandValidationError.TENANT_INAPPLICABLE;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.server.commandbus.Given.ACommand.firstCreateProject;
import static io.spine.server.commandbus.Given.ACommand.removeTask;
import static io.spine.server.commandbus.Given.ACommand.secondStartProject;
import static io.spine.server.tenant.TenantAwareOperation.isTenantSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("Single tenant CommandBus should")
class SingleTenantCommandBusTest extends AbstractCommandBusTestSuite {

    SingleTenantCommandBusTest() {
        super(false);
    }

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        commandBus.register(createProjectHandler);
    }

    @Test
    @DisplayName("post command and do not set current tenant")
    void postCommandWithoutTenant() {
        commandBus.post(newCommandWithoutTenantId(), observer);

        assertFalse(isTenantSet());
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("invalid command")
        void invalidCmd() {
            Command cmd = newCommandWithoutContext();

            commandBus.post(cmd, observer);

            checkCommandError(observer.firstResponse(),
                              INVALID_COMMAND,
                              CommandValidationError.getDescriptor().getFullName(),
                              cmd);
        }

        @Test
        @DisplayName("multitenant command in single tenant context")
        void multitenantCmdIfSingleTenant() {
            // Create a multi-tenant command.
            Command cmd = createProject();

            commandBus.post(cmd, observer);

            checkCommandError(observer.firstResponse(),
                              TENANT_INAPPLICABLE,
                              InvalidCommandException.class,
                              cmd);
        }
    }

    @Test
    @DisplayName("post commands in FIFO order")
    void doPostCommandsInFIFO() {
        Command secondCommand = clearTenantId(secondStartProject());
        CommandPostingHandler handler = new CommandPostingHandler(eventBus, commandBus,
                                                                  secondCommand);
        commandBus.register(handler);

        Command firstCommand = clearTenantId(firstCreateProject());
        MemoizingObserver<Ack> observer = memoizingObserver();
        commandBus.post(firstCommand, observer);

        List<Message> handledCommands = handler.handledCommands();
        assertEquals(2, handledCommands.size());
        assertTrue(FirstCmdCreateProject.class.isInstance(handledCommands.get(0)));
        assertTrue(SecondCmdStartProject.class.isInstance(handledCommands.get(1)));
    }

    @Test
    @DisplayName("do not propagate dispatching errors")
    void doNotPropagateExceptions() {
        FaultyHandler faultyHandler = new FaultyHandler(eventBus);
        commandBus.register(faultyHandler);

        Command remoteTaskCommand = clearTenantId(removeTask());
        MemoizingObserver<Ack> observer = memoizingObserver();
        commandBus.post(remoteTaskCommand, observer);

        Ack ack = observer.firstResponse();
        assertTrue(ack.getStatus()
                      .hasOk());
    }

    @Test
    @DisplayName("create validator once")
    void createValidatorOnce() {
        EnvelopeValidator<CommandEnvelope> validator = commandBus.getValidator();
        assertNotNull(validator);
        assertSame(validator, commandBus.getValidator());
    }

    @Override
    protected Command newCommand() {
        Message commandMessage = Given.CommandMessage.createProjectMessage();
        return TestActorRequestFactory.newInstance(SingleTenantCommandBusTest.class)
                                      .createCommand(commandMessage);
    }
}
