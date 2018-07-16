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

package io.spine.server.command.given;

import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.spine.client.grpc.CommandServiceGrpc;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.CommandService;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.AggregateTest;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.transport.GrpcContainer;
import io.spine.test.command.DCmdCreateProject;
import io.spine.test.command.Project;
import io.spine.test.command.ProjectId;
import io.spine.test.command.ProjectVBuilder;
import io.spine.test.command.event.DCmdProjectCreated;
import io.spine.testing.client.TestActorRequestFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static io.spine.base.Identifier.newUuid;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Mykhailo Drachuk
 */
public class DuplicateCommandTestEnv {

    public static final String SERVICE_HOST = "localhost";
    static final int SHUTDOWN_TIMEOUT_SEC = 5;

    /**
     * Prevents instantiation of this class.
     */
    private DuplicateCommandTestEnv() {
        // Does nothing.
    }

    public static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    private static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    public static DCmdCreateProject createProject() {
        return DCmdCreateProject.newBuilder()
                                .setProjectId(newProjectId())
                                .build();
    }

    public static Command command(Message commandMessage, TenantId tenantId) {
        return newRequestFactory(tenantId).command()
                                          .create(commandMessage);
    }

    private static TestActorRequestFactory newRequestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(AggregateTest.class, tenantId);
    }

    /**
     * Runs the server until its shutdown using {@link TestServer#shutdown()}.
     */
    public static void runServer(final TestServer server) throws Exception {
        final CountDownLatch serverStartLatch = new CountDownLatch(1);

        final Thread serverThread = new Thread(() -> {
            try {
                server.start();
                server.awaitTermination();
                serverStartLatch.countDown();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

        serverThread.start();
        serverStartLatch.await(SHUTDOWN_TIMEOUT_SEC, SECONDS);
    }

    static class DCmdProjectAggregateRepository
            extends AggregateRepository<ProjectId, DCmdProjectAggregate> {
    }

    static class DCmdProjectAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        protected DCmdProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        DCmdProjectCreated handle(DCmdCreateProject command) {
            final ProjectId id = command.getProjectId();
            final DCmdProjectCreated event = projectCreated(id);
            return event;
        }

        private static DCmdProjectCreated projectCreated(ProjectId id) {
            return DCmdProjectCreated.newBuilder()
                                     .setProjectId(id)
                                     .build();
        }

        @Apply
        void event(DCmdProjectCreated event) {
            getBuilder().setId(event.getProjectId());
        }

    }

    /**
     * A client that sends commands to the server specified by its host and port.
     */
    public static class TestClient {
        private final CommandServiceGrpc.CommandServiceBlockingStub blockingClient;
        private final ManagedChannel channel;

        public TestClient(String host, int port) {
            channel = ManagedChannelBuilder.forAddress(host, port)
                                           .usePlaintext()
                                           .build();
            blockingClient = CommandServiceGrpc.newBlockingStub(channel);
        }

        /**
         * Posts the provided command to a remote server.
         *
         * @param command a command to be posted to the server
         * @return acknowledgement of the command dispatch
         */
        public Ack post(Command command) {
            Ack result = blockingClient.post(command);
            return result;
        }

        /**
         * Shuts down the connection channel.
         *
         * @throws InterruptedException if waiting is interrupted.
         */
        public void shutdown() throws InterruptedException {
            channel.shutdown()
                   .awaitTermination(SHUTDOWN_TIMEOUT_SEC, SECONDS);
        }
    }

    /**
     * A test implementation of a server accepting commands with a registered
     * {@link DCmdProjectAggregateRepository project repository}.
     */
    public static class TestServer {

        private final GrpcContainer grpcContainer;
        private final BoundedContext boundedContext;

        public TestServer(int port) {
            // Create a bounded context.
            this.boundedContext =
                    BoundedContext.newBuilder()
                                  .setMultitenant(true)
                                  .build();
            // Create and register a repository with the bounded context.
            final DCmdProjectAggregateRepository repository = new DCmdProjectAggregateRepository();
            boundedContext.register(repository);

            // Create a command service with this bounded context.
            final CommandService commandService =
                    CommandService.newBuilder()
                                  .add(boundedContext)
                                  .build();

            // Pass the service to a GRPC container
            this.grpcContainer = GrpcContainer.newBuilder()
                                              .addService(commandService)
                                              .setPort(port)
                                              .build();
        }

        /**
         * Starts the gRPC container.
         *
         * @throws IOException if unable to bind to a port
         */
        public void start() throws IOException {
            grpcContainer.start();
            grpcContainer.addShutdownHook();
        }

        /**
         * Awaits gRPC container termination.
         */
        public void awaitTermination() {
            grpcContainer.awaitTermination();
        }

        /**
         * Shuts down the connection channel.
         *
         * @throws InterruptedException if waiting is interrupted.
         */
        public void shutdown() throws Exception {
            grpcContainer.shutdown();
            boundedContext.close();
        }
    }
}
