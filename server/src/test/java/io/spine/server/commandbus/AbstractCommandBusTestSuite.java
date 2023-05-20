/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.server.commandbus;

import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.CommandValidationError;
import io.spine.core.Status;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.ServerEnvironment;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.commandbus.given.DirectScheduledExecutor;
import io.spine.server.commandbus.given.MemoizingCommandFlowWatcher;
import io.spine.server.event.EventBus;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.system.server.SystemWriteSide;
import io.spine.test.commandbus.command.CmdBusCreateProject;
import io.spine.test.commandbus.event.CmdBusProjectCreated;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.core.CommandValidationError.INVALID_COMMAND;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.testing.core.given.GivenTenantId.generate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract base for test suites of {@code CommandBus}.
 */
@SuppressWarnings("ProtectedField") // for brevity of derived tests.
abstract class AbstractCommandBusTestSuite {

    private final boolean multitenant;

    protected ActorRequestFactory requestFactory;

    protected CommandBus commandBus;
    protected EventBus eventBus;
    protected CreateProjectHandler createProjectHandler;
    protected MemoizingObserver<Ack> observer;
    protected MemoizingCommandFlowWatcher watcher;
    protected TenantIndex tenantIndex;
    protected SystemWriteSide systemWriteSide;
    protected BoundedContext context;

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
        Command.Builder commandBuilder = cmd.toBuilder();
        commandBuilder
                .getContextBuilder()
                .getActorContextBuilder()
                .setTenantId(TenantId.getDefaultInstance());
        return commandBuilder.vBuild();
    }

    protected static Command clearTenantId(Command cmd) {
        Command.Builder result = cmd.toBuilder();
        result.getContextBuilder()
              .getActorContextBuilder()
              .clearTenantId();
        return result.vBuild();
    }

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();

        ScheduledExecutorService executorService = new DirectScheduledExecutor();
        CommandScheduler scheduler = new ExecutorCommandScheduler(executorService);
        ServerEnvironment.instance()
                         .scheduleCommandsUsing(() -> scheduler);

        context = createContext();

        BoundedContext.InternalAccess contextAccess = context.internalAccess();
        tenantIndex = contextAccess.tenantIndex();
        systemWriteSide = NoOpSystemWriteSide.INSTANCE;

        eventBus = context.eventBus();
        watcher = new MemoizingCommandFlowWatcher();
        commandBus = CommandBus
                .newBuilder()
                .setMultitenant(this.multitenant)
                .injectContext(context)
                .injectSystem(systemWriteSide)
                .injectTenantIndex(tenantIndex)
                .setWatcher(watcher)
                .build();
        requestFactory =
                multitenant
                ? new TestActorRequestFactory(getClass(), generate())
                : new TestActorRequestFactory(getClass());
        createProjectHandler = new CreateProjectHandler();
        contextAccess.registerCommandDispatcher(createProjectHandler);
        observer = memoizingObserver();
    }

    private BoundedContext createContext() {
        Class<? extends AbstractCommandBusTestSuite> cls = getClass();
        String name = cls.getSimpleName();
        BoundedContextBuilder builder =
                multitenant
                ? BoundedContext.multitenant(name)
                : BoundedContext.singleTenant(name);
        return builder.build();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
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

        commandBus.post(commands, memoizingObserver());

        assertTrue(createProjectHandler.received(first.enclosedMessage()));
        assertTrue(createProjectHandler.received(second.enclosedMessage()));
        commandBus.unregister(createProjectHandler);
    }

    protected Command newCommand() {
        return Given.ACommand.createProject();
    }

    protected void checkResult(Command cmd) {
        assertNull(observer.getError());
        assertTrue(observer.isCompleted());
        Ack ack = observer.firstResponse();
        Any messageId = ack.getMessageId();
        assertEquals(cmd.getId(), Identifier.unpack(messageId));
    }

    /**
     * A sample command handler that tells whether a handler was invoked.
     */
    final class CreateProjectHandler extends AbstractCommandHandler {

        private boolean handlerInvoked = false;
        private final Set<CommandMessage> receivedCommands = newHashSet();

        @Override
        public final void registerWith(BoundedContext context) {
            super.registerWith(context);
        }

        @Assign
        CmdBusProjectCreated handle(CmdBusCreateProject command, CommandContext ctx) {
            handlerInvoked = true;
            receivedCommands.add(command);
            return CmdBusProjectCreated.getDefaultInstance();
        }

        boolean received(CommandMessage command) {
            return receivedCommands.contains(command);
        }

        boolean wasHandlerInvoked() {
            return handlerInvoked;
        }
    }
}
