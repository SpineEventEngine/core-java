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

package org.spine3.server.clientservice;


import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.TimeUtil;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Identifiers;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.people.PersonName;
import org.spine3.server.BoundedContext;
import org.spine3.server.ClientService;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.AggregateRepository;
import org.spine3.server.aggregate.Apply;
import org.spine3.server.command.Assign;
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
import org.spine3.time.LocalDate;
import org.spine3.time.LocalDates;
import org.spine3.users.UserId;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ClientServiceShould {

    private final Set<BoundedContext> boundedContexts = Sets.newHashSet();
    @Before
    public void setUp() {
        // Create Projects Bounded Context with one repository.
        final BoundedContext projectsContext = newBoundedContext();
        final ProjectAggregateRepository projectRepo = new ProjectAggregateRepository(projectsContext);
        projectsContext.register(projectRepo);
        boundedContexts.add(projectsContext);

        // Create Customers Bounded Context with one repository.
        final BoundedContext customersContext = newBoundedContext();
        final CustomerAggregateRepository customerRepo = new CustomerAggregateRepository(customersContext);
        customersContext.register(customerRepo);
        boundedContexts.add(customersContext);

        // Expose two Bounded Contexts via a Client Service.
        final ClientService.Builder builder = ClientService.newBuilder();
        for (BoundedContext context : boundedContexts) {
            builder.addBoundedContext(context);
        }

        clientService = builder.build();
    }

    private ClientService clientService;

    @After
    public void tearDown() throws Exception {
        if (!clientService.isShutdown()) {
            clientService.shutdown();
        }

        for (BoundedContext boundedContext : boundedContexts) {
            boundedContext.close();
        }
    }

    @Test
    public void accept_commands_for_linked_bounded_contexts() {
        final TestResponseObserver responseObserver = new TestResponseObserver();
        final Command createProject = Given.Command.createProject();
        clientService.post(createProject, responseObserver);
        assertEquals(Responses.ok(), responseObserver.getResponseHandled());

        final Command createCustomer = createCustomerCmd();
        clientService.post(createCustomer, responseObserver);
        assertEquals(Responses.ok(), responseObserver.getResponseHandled());
    }

    @Test
    public void return_error_if_command_is_unsupported() {
        final TestResponseObserver responseObserver = new TestResponseObserver();
        final Command unsupportedCmd = Commands.create(StringValue.getDefaultInstance(), createCommandContext());

        clientService.post(unsupportedCmd, responseObserver);

        final Throwable exception = responseObserver.getThrowable()
                                                    .getCause();
        assertEquals(UnsupportedCommandException.class, exception.getClass());
    }

    /*
     * Commands stubs
     *********************/

    @SuppressWarnings("StaticNonFinalField") /* This hack is just for the testing purposes. The production code should
                                                use more sane approach to generating the IDs. */
    private static int customerNumber = 1;

    private static Command createCustomerCmd() {
        final LocalDate localDate = LocalDates.today();
        final CustomerId customerId = CustomerId.newBuilder()
                                                .setRegistrationDate(localDate)
                                                .setNumber(customerNumber)
                                                .build();
        customerNumber++;
        final Message msg = CreateCustomer.newBuilder()
                                          .setCustomerId(customerId)
                                          .setCustomer(Customer.newBuilder()
                                                               .setId(customerId)
                                                               .setName(PersonName.newBuilder()
                                                                                  .setGivenName("Kreat")
                                                                                  .setFamilyName("C'Ustomer")
                                                                                  .setHonorificSuffix("Cmd")))
                                          .build();
        final UserId userId = newUserId(Identifiers.newUuid());
        final Command result = Given.Command.create(msg, userId, TimeUtil.getCurrentTime());

        return result;
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
        }

        public Response getResponseHandled() {
            return responseHandled;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }

}
