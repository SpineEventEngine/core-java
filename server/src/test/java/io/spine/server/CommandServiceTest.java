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

package io.spine.server;

import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandValidationError;
import io.spine.core.Status;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.given.transport.TestGrpcServer;
import io.spine.test.commandservice.CmdServDontHandle;
import io.spine.testing.TestValues;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.Messages.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommandService should")
class CommandServiceTest {

    private CommandService service;

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
    private BoundedContext projectsContext;

    private BoundedContext customersContext;
    private final MemoizingObserver<Ack> responseObserver = memoizingObserver();

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        // Create Projects Bounded Context with one repository.
        projectsContext = BoundedContext.multitenant("Projects").build();
        Given.ProjectAggregateRepository projectRepo = new Given.ProjectAggregateRepository();
        projectsContext.register(projectRepo);
        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = BoundedContext.multitenant("Customers").build();
        Given.CustomerAggregateRepository customerRepo = new Given.CustomerAggregateRepository();
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        // Expose two Bounded Contexts via an instance of {@code CommandService}.
        CommandService.Builder builder = CommandService.newBuilder();
        for (BoundedContext context : boundedContexts) {
            builder.add(context);
        }
        service = builder.build();
    }

    @AfterEach
    void tearDown() throws Exception {
        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    @DisplayName("post commands to appropriate bounded context")
    void postCommandsToBc() {
        verifyPostsCommand(Given.ACommand.createProject());
        verifyPostsCommand(Given.ACommand.createCustomer());
    }

    @Test
    @DisplayName("never retrieve removed bounded contexts from builder")
    void notRetrieveRemovedBc() {
        CommandService.Builder builder = CommandService
                .newBuilder()
                .add(projectsContext)
                .add(customersContext)
                .remove(projectsContext);

        // Create BoundedContext map.
        CommandService service = builder.build();
        assertNotNull(service);

        assertTrue(builder.contains(customersContext));
        assertFalse(builder.contains(projectsContext));
    }

    private void verifyPostsCommand(Command cmd) {
        MemoizingObserver<Ack> observer = memoizingObserver();
        service.post(cmd, observer);

        assertNull(observer.getError());
        assertTrue(observer.isCompleted());
        Ack acked = observer.firstResponse();
        Any messageId = acked.getMessageId();
        assertEquals(cmd.getId(), Identifier.unpack(messageId));
    }

    @Test
    @DisplayName("return error status if command is unsupported")
    @MuteLogging
    void returnCommandUnsupportedError() {
        TestActorRequestFactory factory = new TestActorRequestFactory(getClass());

        Command unsupportedCmd = factory.createCommand(CmdServDontHandle.getDefaultInstance());

        service.post(unsupportedCmd, responseObserver);

        assertTrue(responseObserver.isCompleted());
        Ack result = responseObserver.firstResponse();
        assertNotNull(result);
        assertTrue(isNotDefault(result));
        Status status = result.getStatus();
        assertEquals(ERROR, status.getStatusCase());
        Error error = status.getError();
        assertEquals(CommandValidationError.getDescriptor().getFullName(), error.getType());
    }

    @Test
    @DisplayName("deploy to gRPC container")
    void deployToGrpcContainer() throws IOException {
        GrpcContainer grpcContainer = GrpcContainer.inProcess(TestValues.randomString())
                                                   .addService(service)
                                                   .build();
        grpcContainer.injectServer(new TestGrpcServer());
        assertTrue(grpcContainer.isScheduledForDeployment(service));

        grpcContainer.start();
        assertTrue(grpcContainer.isLive(service));

        grpcContainer.shutdown();
        assertFalse(grpcContainer.isLive(service));
    }
}
