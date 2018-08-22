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

import io.spine.base.Error;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.CommandId;
import io.spine.core.CommandValidationError;
import io.spine.core.Status;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.event.EventBus;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.NoOpSystemGateway;
import io.spine.system.server.SystemGateway;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.event.CmdProjectCreated;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.model.ModelTests;
import io.spine.testing.server.tenant.TenantAwareTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.BoundedContextNames.newName;
import static io.spine.core.CommandValidationError.INVALID_COMMAND;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.testing.Verify.assertContainsAll;
import static io.spine.testing.Verify.assertSize;
import static io.spine.testing.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Abstract base for test suites of {@code CommandBus}.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ProtectedField") // OK for brevity of derived tests.
abstract class AbstractCommandBusTestSuite {

    private final boolean multitenant;

    protected ActorRequestFactory requestFactory;

    protected CommandBus commandBus;
    protected EventBus eventBus;
    protected ExecutorCommandScheduler scheduler;
    protected CreateProjectHandler createProjectHandler;
    protected MemoizingObserver<Ack> observer;
    protected TenantIndex tenantIndex;
    protected SystemGateway systemGateway;

    /**
     * A public constructor for derived test cases.
     *
     * @param multitenant the multi-tenancy status of the {@code CommandBus} under tests
     */
    AbstractCommandBusTestSuite(boolean multitenant) {
        this.multitenant = multitenant;
    }

    static Command newCommandWithoutContext() {
        Command cmd = createProject();
        Command invalidCmd = cmd.toBuilder()
                                .setContext(CommandContext.getDefaultInstance())
                                .build();
        return invalidCmd;
    }

    static void checkCommandError(Ack sendingResult,
                                  CommandValidationError validationError,
                                  Class<? extends CommandException> exceptionClass,
                                  Command cmd) {
        checkCommandError(sendingResult,
                          validationError,
                          exceptionClass.getCanonicalName(),
                          cmd);
    }

    static void checkCommandError(Ack sendingResult,
                                  CommandValidationError validationError,
                                  String errorType,
                                  Command cmd) {
        Status status = sendingResult.getStatus();
        assertEquals(status.getStatusCase(), Status.StatusCase.ERROR);
        CommandId commandId = cmd.getId();
        assertEquals(commandId, unpack(sendingResult.getMessageId()));

        Error error = status.getError();
        assertEquals(errorType,
                     error.getType());
        assertEquals(validationError.getNumber(), error.getCode());
        assertFalse(error.getMessage()
                         .isEmpty());
        if (validationError == INVALID_COMMAND) {
            assertFalse(error.getValidationError()
                             .getConstraintViolationList()
                             .isEmpty());
        }
    }

    protected static Command newCommandWithoutTenantId() {
        Command cmd = createProject();
        ActorContext.Builder withNoTenant = ActorContext
                .newBuilder()
                .setTenantId(TenantId.getDefaultInstance());
        Command invalidCmd =
                cmd.toBuilder()
                   .setContext(cmd.getContext()
                                  .toBuilder()
                                  .setActorContext(withNoTenant))
                   .build();
        return invalidCmd;
    }

    protected static Command clearTenantId(Command cmd) {
        ActorContext.Builder withNoTenant = ActorContext
                .newBuilder()
                .setTenantId(TenantId.getDefaultInstance());
        Command result = cmd.toBuilder()
                            .setContext(cmd.getContext()
                                           .toBuilder()
                                           .setActorContext(withNoTenant))
                            .build();
        return result;
    }

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        Class<? extends AbstractCommandBusTestSuite> cls = getClass();
        InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.newInstance(newName(cls.getSimpleName()), multitenant);
        tenantIndex = TenantAwareTest.createTenantIndex(multitenant, storageFactory);
        scheduler = spy(new ExecutorCommandScheduler());
        systemGateway = NoOpSystemGateway.INSTANCE;
        eventBus = EventBus.newBuilder()
                           .setStorageFactory(storageFactory)
                           .build();
        commandBus = CommandBus
                .newBuilder()
                .setMultitenant(this.multitenant)
                .setCommandScheduler(scheduler)
                .injectEventBus(eventBus)
                .injectSystemGateway(systemGateway)
                .injectTenantIndex(tenantIndex)
                .build();
        requestFactory =
                multitenant
                ? TestActorRequestFactory.newInstance(getClass(), newUuid())
                : TestActorRequestFactory.newInstance(getClass());
        createProjectHandler = new CreateProjectHandler();
        observer = memoizingObserver();
    }

    @AfterEach
    void tearDown() throws Exception {
        eventBus.close();
    }

    @Test
    @DisplayName("post commands in bulk")
    void postCommandsInBulk() {
        Command first = newCommand();
        Command second = newCommand();
        List<Command> commands = newArrayList(first, second);

        // Some derived test suite classes may register the handler in setUp().
        // This prevents the repeating registration (which is an illegal operation).
        commandBus.unregister(createProjectHandler);
        commandBus.register(createProjectHandler);

        CommandBus spy = spy(commandBus);
        spy.post(commands, memoizingObserver());

        @SuppressWarnings("unchecked") ArgumentCaptor<Iterable<Command>> storingCaptor = forClass(Iterable.class);
        verify(spy).store(storingCaptor.capture());
        Iterable<Command> storingArgs = storingCaptor.getValue();
        assertSize(commands.size(), storingArgs);
        assertContainsAll(storingArgs, first, second);

        ArgumentCaptor<CommandEnvelope> postingCaptor = forClass(CommandEnvelope.class);
        verify(spy, times(2)).dispatch(postingCaptor.capture());
        List<CommandEnvelope> postingArgs = postingCaptor.getAllValues();
        assertSize(commands.size(), postingArgs);
        assertEquals(commands.get(0), postingArgs.get(0).getCommand());
        assertEquals(commands.get(1), postingArgs.get(1).getCommand());

        commandBus.unregister(createProjectHandler);
    }

    protected Command newCommand() {
        return Given.ACommand.createProject();
    }

    protected void checkResult(Command cmd) {
        assertNull(observer.getError());
        assertTrue(observer.isCompleted());
        CommandId commandId = unpack(observer.firstResponse()
                                             .getMessageId());
        assertEquals(cmd.getId(), commandId);
    }

    /**
     * A sample command handler that tells whether a handler was invoked.
     */
    class CreateProjectHandler extends AbstractCommandHandler {

        private boolean handlerInvoked = false;

        CreateProjectHandler() {
            super(eventBus);
        }

        @Assign
        CmdProjectCreated handle(CmdCreateProject command, CommandContext ctx) {
            handlerInvoked = true;
            return CmdProjectCreated.getDefaultInstance();
        }

        boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }
}
