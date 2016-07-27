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
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.error.UnsupportedCommandException;
import org.spine3.test.clientservice.Project;
import org.spine3.test.clientservice.ProjectId;
import org.spine3.test.clientservice.command.AddTask;
import org.spine3.test.clientservice.command.CreateProject;
import org.spine3.test.clientservice.command.StartProject;
import org.spine3.test.clientservice.customer.Customer;
import org.spine3.test.clientservice.customer.CustomerId;
import org.spine3.test.clientservice.customer.command.CreateCustomer;
import org.spine3.test.clientservice.customer.event.CustomerCreated;
import org.spine3.test.clientservice.event.ProjectCreated;
import org.spine3.test.clientservice.event.ProjectStarted;
import org.spine3.test.clientservice.event.TaskAdded;
import org.spine3.testdata.TestCommandBusFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ClientServiceShould {

    private ClientService service;
    private Server server;

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
    private BoundedContext projectsContext;

    private BoundedContext customersContext;
    private final TestResponseObserver responseObserver = new TestResponseObserver();

    @Before
    public void setUp() {
        // Create Projects Bounded Context with one repository.
        projectsContext = newBoundedContext(spy(TestCommandBusFactory.create()));
        final ProjectAggregateRepository projectRepo = new ProjectAggregateRepository(projectsContext);
        projectsContext.register(projectRepo);
        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        customersContext = newBoundedContext(spy(TestCommandBusFactory.create()));
        final CustomerAggregateRepository customerRepo = new CustomerAggregateRepository(customersContext);
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        // Expose two Bounded Contexts via a Client Service.
        final ClientService.Builder builder = ClientService.newBuilder();
        for (BoundedContext context : boundedContexts) {
            builder.addBoundedContext(context);
        }
        service = spy(builder.build());
        server = mock(Server.class);
        doReturn(server).when(service).createGrpcServer(anyInt());
    }

    @After
    public void tearDown() throws Exception {
        if (!service.isShutdown()) {
            service.shutdown();
        }
        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    public void post_commands_to_appropriate_bounded_context() {
        verifyPostsCommand(Given.Command.createProject(), projectsContext.getCommandBus());
        verifyPostsCommand(Given.Command.createCustomer(), customersContext.getCommandBus());
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

        final Throwable exception = responseObserver.getThrowable().getCause();
        assertEquals(UnsupportedCommandException.class, exception.getClass());
    }

    @Test
    public void start_server() throws IOException {
        service.start();

        verify(server).start();
    }

    @Test
    public void throw_exception_if_started_already() throws IOException {
        service.start();
        try {
            service.start();
        } catch (IllegalStateException expected) {
            return;
        }
        fail("Exception must be thrown.");
    }

    @Test
    public void await_termination() throws IOException, InterruptedException {
        service.start();
        service.awaitTermination();

        verify(server).awaitTermination();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_call_await_termination_on_not_started_service() {
        service.awaitTermination();
    }

    @Test
    public void assure_service_is_shutdown() throws IOException {
        service.start();
        service.shutdown();

        assertTrue(service.isShutdown());
    }

    @Test
    public void assure_service_was_not_started() throws IOException {
        assertTrue(service.isShutdown());
    }

    @Test
    public void assure_service_is_not_shut_down() throws IOException {
        service.start();

        assertFalse(service.isShutdown());
    }

    @Test
    public void shutdown_itself() throws IOException, InterruptedException {
        service.start();
        service.shutdown();

        verify(server).shutdown();
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_call_shutdown_on_not_started_service() {
        service.shutdown();
    }

    @Test
    public void throw_exception_if_shutdown_already() throws IOException {
        service.start();
        service.shutdown();
        try {
            service.shutdown();
        } catch (IllegalStateException expected) {
            return;
        }
        fail("Expected an exception.");
    }

    /*
     * Stub repositories and aggregates
     ***************************************************/

    private static class ProjectAggregateRepository extends AggregateRepository<ProjectId, ProjectAggregate> {
        private ProjectAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class ProjectAggregate extends Aggregate<ProjectId, Project, Project.Builder> {
        // an aggregate constructor must be public because it is used via reflection
        @SuppressWarnings("PublicConstructorInNonPublicClass")
        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        public ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return Given.EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        public TaskAdded handle(AddTask cmd, CommandContext ctx) {
            return Given.EventMessage.taskAdded(cmd.getProjectId());
        }

        @Assign
        public List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            final ProjectStarted message =  Given.EventMessage.projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED)
                    .build();
        }

        @Apply
        private void event(TaskAdded event) {
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.STARTED)
                    .build();
        }
    }

    private static class CustomerAggregateRepository extends AggregateRepository<CustomerId, CustomerAggregate> {
        private CustomerAggregateRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }
    }

    private static class CustomerAggregate extends Aggregate<CustomerId, Customer, Customer.Builder> {

        @SuppressWarnings("PublicConstructorInNonPublicClass") // by convention (as it's used by Reflection).
        public CustomerAggregate(CustomerId id) {
            super(id);
        }

        @Assign
        public CustomerCreated handle(CreateCustomer cmd, CommandContext ctx) {
            final CustomerCreated event = CustomerCreated.newBuilder()
                                                         .setCustomerId(cmd.getCustomerId())
                                                         .setCustomer(cmd.getCustomer())
                                                         .build();
            return event;
        }

        @Apply
        private void event(CustomerCreated event) {
            incrementState(event.getCustomer());
        }
    }

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
