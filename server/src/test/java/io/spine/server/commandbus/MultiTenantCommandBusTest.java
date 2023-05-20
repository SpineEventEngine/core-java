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

import com.google.common.testing.NullPointerTester;
import io.grpc.stub.StreamObserver;
import io.spine.core.Command;
import io.spine.core.CommandValidationError;
import io.spine.grpc.StreamObservers;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.commandbus.given.MultitenantCommandBusTestEnv.AddTaskDispatcher;
import io.spine.server.type.CommandClass;
import io.spine.test.commandbus.command.CmdBusAddTask;
import io.spine.test.commandbus.command.CmdBusCreateProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.core.CommandValidationError.TENANT_UNKNOWN;
import static io.spine.core.CommandValidationError.UNSUPPORTED_COMMAND;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.server.commandbus.Given.ACommand.addTask;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Multitenant CommandBus should")
class MultiTenantCommandBusTest extends AbstractCommandBusTestSuite {

    MultiTenantCommandBusTest() {
        super(true);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .setDefault(StreamObserver.class, StreamObservers.noOpObserver())
                .testAllPublicInstanceMethods(commandBus);
    }

    @Test
    @DisplayName("not accept default command on post")
    void notAcceptDefaultCommand() {
        assertThrows(IllegalArgumentException.class,
                     () -> commandBus.post(Command.getDefaultInstance(), observer));
    }

    @Test
    @DisplayName("verify tenant ID attribute if is multitenant")
    void requireTenantId() {
        commandBus.register(createProjectHandler);
        Command cmd = newCommandWithoutTenantId();

        commandBus.post(cmd, observer);

        checkCommandError(observer.firstResponse(),
                          TENANT_UNKNOWN,
                          InvalidCommandException.class,
                          cmd);
    }

    @Test
    @DisplayName("state command not supported when there is neither handler nor dispatcher for it")
    void requireHandlerOrDispatcher() {
        Command command = addTask();
        commandBus.post(command, observer);

        checkCommandError(observer.firstResponse(),
                          UNSUPPORTED_COMMAND,
                          CommandValidationError.getDescriptor()
                                                .getFullName(),
                          command);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            commandBus.register(new AddTaskDispatcher());

            commandBus.post(addTask(), observer);

            assertTrue(observer.isCompleted());
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            CreateProjectHandler handler = new CreateProjectHandler();
            commandBus.register(handler);
            handler.registerWith(context);

            commandBus.post(createProject(), observer);

            assertTrue(observer.isCompleted());
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            CommandDispatcher dispatcher = new AddTaskDispatcher();
            commandBus.register(dispatcher);
            commandBus.unregister(dispatcher);

            commandBus.post(addTask(), observer);

            assertEquals(ERROR, observer.firstResponse()
                                        .getStatus()
                                        .getStatusCase());
        }

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            AbstractCommandHandler handler = newCommandHandler();

            commandBus.register(handler);
            commandBus.unregister(handler);

            commandBus.post(createProject(), observer);

            assertEquals(ERROR, observer.firstResponse()
                                        .getStatus()
                                        .getStatusCase());
        }

        CreateProjectHandler newCommandHandler() {
            CreateProjectHandler handler = new CreateProjectHandler();
            handler.registerWith(context);
            return handler;
        }
    }

    @Test
    @DisplayName("post command and return `OK` response")
    void postCommand() {
        commandBus.register(createProjectHandler);

        Command command = createProject();
        commandBus.post(command, observer);

        checkResult(command);
    }

    @Nested
    @DisplayName("when command is posted, invoke")
    class Invoke {

        @Test
        @DisplayName("command handler")
        void commandHandler() {
            commandBus.register(createProjectHandler);

            commandBus.post(createProject(), observer);

            assertTrue(createProjectHandler.wasHandlerInvoked());
        }

        @Test
        @DisplayName("command dispatcher")
        void commandDispatcher() {
            AddTaskDispatcher dispatcher = new AddTaskDispatcher();
            commandBus.register(dispatcher);

            commandBus.post(addTask(), observer);

            assertTrue(dispatcher.wasDispatcherInvoked());
        }
    }

    @Test
    @DisplayName("expose supported classes")
    void exposeSupportedClasses() {
        commandBus.register(createProjectHandler);
        commandBus.register(new AddTaskDispatcher());

        Set<CommandClass> cmdClasses = commandBus.registeredCommandClasses();

        assertTrue(cmdClasses.contains(CommandClass.from(CmdBusCreateProject.class)));
        assertTrue(cmdClasses.contains(CommandClass.from(CmdBusAddTask.class)));
    }
}
