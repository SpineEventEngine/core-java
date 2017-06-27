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

package io.spine.server;

import com.google.common.collect.Sets;
import com.google.protobuf.StringValue;
import io.spine.base.Error;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.CommandValidationError;
import io.spine.core.IsSent;
import io.spine.core.Status;
import io.spine.grpc.StreamObservers.MemoizingObserver;
import io.spine.protobuf.AnyPacker;
import io.spine.server.transport.GrpcContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

public class CommandServiceShould {

    private CommandService service;

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
    private BoundedContext projectsContext;

    private BoundedContext customersContext;
    private final MemoizingObserver<IsSent> responseObserver = memoizingObserver();

    @Before
    public void setUp() {
        // Create Projects Bounded Context with one repository.
        projectsContext = BoundedContext.newBuilder()
                                        .setMultitenant(true)
                                        .build();
        final Given.ProjectAggregateRepository projectRepo =
                new Given.ProjectAggregateRepository(projectsContext);
        projectsContext.register(projectRepo);
        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = BoundedContext.newBuilder()
                                         .setMultitenant(true)
                                         .build();
        final Given.CustomerAggregateRepository customerRepo =
                new Given.CustomerAggregateRepository(customersContext);
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
        verifyPostsCommand(Given.ACommand.createProject());
        verifyPostsCommand(Given.ACommand.createCustomer());
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

    private void verifyPostsCommand(Command cmd) {
        final MemoizingObserver<IsSent> observer = memoizingObserver();
        service.post(cmd, observer);

        assertNull(observer.getError());
        assertTrue(observer.isCompleted());
        final IsSent acked = observer.firstResponse();
        final CommandId id = AnyPacker.unpack(acked.getMessageId());
        assertEquals(cmd.getId(), id);
    }

    @Test
    public void return_error_status_if_command_is_unsupported() {
        final TestActorRequestFactory factory = TestActorRequestFactory.newInstance(getClass());

        final Command unsupportedCmd = factory.createCommand(StringValue.getDefaultInstance());

        service.post(unsupportedCmd, responseObserver);

        assertTrue(responseObserver.isCompleted());
        final IsSent result = responseObserver.firstResponse();
        assertNotNull(result);
        assertTrue(isNotDefault(result));
        final Status status = result.getStatus();
        assertEquals(ERROR, status.getStatusCase());
        final Error error = status.getError();
        assertEquals(CommandValidationError.getDescriptor().getFullName(), error.getType());
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
}
