/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.base.CommandMessage;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandValidationError;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.commandbus.given.SingleTenantCommandBusTestEnv.FaultyHandler;
import io.spine.server.type.CommandEnvelope;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.mute.MuteLogging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.core.CommandValidationError.INVALID_COMMAND;
import static io.spine.core.CommandValidationError.TENANT_INAPPLICABLE;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.commandbus.Given.ACommand.createProject;
import static io.spine.server.commandbus.Given.ACommand.removeTask;
import static io.spine.server.tenant.TenantAwareOperation.isTenantSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Single tenant CommandBus should")
class SingleTenantCommandBusTest extends AbstractCommandBusTestSuite {

    private static final TestActorRequestFactory requestFactory =
            new TestActorRequestFactory(SingleTenantCommandBusTest.class);

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
                              CommandValidationError.getDescriptor()
                                                    .getFullName(),
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

    @MuteLogging
    @Test
    @DisplayName("do not propagate dispatching errors")
    void doNotPropagateExceptions() {
        FaultyHandler faultyHandler = FaultyHandler.initializedHandler();
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
        EnvelopeValidator<CommandEnvelope> validator = commandBus.validator();
        assertNotNull(validator);
        assertSame(validator, commandBus.validator());
    }

    @Override
    protected Command newCommand() {
        CommandMessage commandMessage = Given.CommandMessage.createProjectMessage();
        return requestFactory.createCommand(commandMessage);
    }
}
