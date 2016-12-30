/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server;

import com.google.common.collect.Sets;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.Commands;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.server.transport.GrpcContainer;
import org.spine3.testdata.TestCommandBusFactory;

import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

@SuppressWarnings("InstanceMethodNamingConvention")
public class CommandServiceShould {

    private CommandService service;

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
    private BoundedContext projectsContext;

    private BoundedContext customersContext;
    private final TestResponseObserver responseObserver = new TestResponseObserver();

    @Before
    public void setUp() {
        // Create Projects Bounded Context with one repository.
        projectsContext = newBoundedContext(spy(TestCommandBusFactory.create()));
        final Given.ProjectAggregateRepository projectRepo = new Given.ProjectAggregateRepository(projectsContext);
        projectsContext.register(projectRepo);
        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = newBoundedContext(spy(TestCommandBusFactory.create()));
        final Given.CustomerAggregateRepository customerRepo = new Given.CustomerAggregateRepository(customersContext);
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        // Expose two Bounded Contexts via an instance of {@code CommandService}.
        final CommandService.Builder builder = CommandService.newBuilder();
        for (BoundedContext context : boundedContexts) {
            builder.add(context);
        }
        service = spy(builder.build());
    }

    @After
    public void tearDown() throws Exception {
        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    public void post_commands_to_appropriate_bounded_context() {
        verifyPostsCommand(Given.Command.createProject(), projectsContext.getCommandBus());
        verifyPostsCommand(Given.Command.createCustomer(), customersContext.getCommandBus());
    }

    @Test
    public void never_retrieve_removed_bounded_contexts_from_builder() {
        final CommandService.Builder builder = CommandService.newBuilder()
                                                             .add(projectsContext)
                                                             .add(customersContext)
                                                             .remove(projectsContext);

        final CommandService service = builder.build(); // Creates BoundedContext map
        assertNotNull(service);

        assertTrue(builder.contains(customersContext));
        assertFalse(builder.contains(projectsContext));
    }

    private void verifyPostsCommand(Command cmd, CommandBus commandBus) {
        service.post(cmd, responseObserver);

        assertEquals(Responses.ok(), responseObserver.getResponseHandled());
        assertTrue(responseObserver.isCompleted());
        assertNull(responseObserver.getThrowable());
        verify(commandBus).post(cmd, responseObserver);
    }

    @Test
    public void return_error_if_command_is_unsupported() {
        final Command unsupportedCmd = Commands.create(StringValue.getDefaultInstance(), createCommandContext());

        service.post(unsupportedCmd, responseObserver);

        final Throwable exception = responseObserver.getThrowable()
                                                    .getCause();
        assertEquals(UnsupportedCommandException.class, exception.getClass());
    }

    @Test
    public void deploy_to_grpc_container() throws IOException {
        final GrpcContainer grpcContainer = GrpcContainer.newBuilder()
                                                         .addService(service)
                                                         .build();
        try {
            assertTrue(grpcContainer.isScheduledForDeployment(service));

            grpcContainer.start();
            assertTrue(grpcContainer.isLive(service));

            grpcContainer.shutdown();
            assertFalse(grpcContainer.isLive(service));
        } finally {
            if (!grpcContainer.isShutdown()) {
                grpcContainer.shutdown();
            }
        }
    }

    /*
     * Stub repositories and aggregates
     ***************************************************/

    private static class TestResponseObserver implements StreamObserver<Response> {

        private Response responseHandled;
        private Throwable throwable;
        private boolean isCompleted = false;

        @Override
        public void onNext(Response response) {
            this.responseHandled = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.isCompleted = true;
        }

        /* package */ Response getResponseHandled() {
            return responseHandled;
        }

        /* package */ Throwable getThrowable() {
            return throwable;
        }

        /* package */ boolean isCompleted() {
            return isCompleted;
        }
    }
}
