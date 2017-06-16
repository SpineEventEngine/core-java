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

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Command;
import io.spine.base.CommandContext;
import io.spine.base.Identifier;
import io.spine.base.TenantId;
import io.spine.base.UserId;
import io.spine.client.ActorRequestFactory;
import io.spine.client.Query;
import io.spine.people.PersonName;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.TestActorRequestFactory;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AddTask;
import io.spine.test.aggregate.command.CreateProject;
import io.spine.test.aggregate.command.StartProject;
import io.spine.test.aggregate.event.ProjectCreated;
import io.spine.test.aggregate.event.ProjectStarted;
import io.spine.test.aggregate.event.TaskAdded;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.test.commandservice.customer.CustomerVBuilder;
import io.spine.test.commandservice.customer.command.CreateCustomer;
import io.spine.test.commandservice.customer.event.CustomerCreated;
import io.spine.time.LocalDate;
import io.spine.time.LocalDates;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.test.Values.newUserId;
import static io.spine.time.Time.getCurrentTime;

public class Given {

    private Given() {
    }

    static ProjectId newProjectId() {
        final String uuid = newUuid();
        return ProjectId.newBuilder()
                        .setId(uuid)
                        .build();
    }

    static class EventMessage {

        private EventMessage() {
        }

        static TaskAdded taskAdded(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    static class CommandMessage {

        private CommandMessage() {
        }

        public static CreateProject createProject(ProjectId id) {
            return CreateProject.newBuilder()
                                .setProjectId(id)
                                .build();
        }
    }

    static class ACommand {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = newProjectId();

        /* This hack is just for the testing purposes.
        The production code should use more sane approach to generating the IDs. */
        private static final AtomicInteger customerNumber = new AtomicInteger(1);

        private ACommand() {
        }

        /**
         * Creates a new {@link ACommand} with the given command message, userId and
         * timestamp using default {@link ACommand} instance.
         */
        static Command create(Message command, UserId userId, Timestamp when) {
            final TenantId generatedTenantId = TenantId.newBuilder()
                                                       .setValue(newUuid())
                                                       .build();
            final TestActorRequestFactory factory =
                    TestActorRequestFactory.newInstance(userId, generatedTenantId);
            final Command result = factory.createCommand(command, when);
            return result;
        }

        static Command createProject() {
            return createProject(getCurrentTime());
        }

        static Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        static Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        static Command createCustomer() {
            final LocalDate localDate = LocalDates.now();
            final CustomerId customerId = CustomerId.newBuilder()
                                                    .setRegistrationDate(localDate)
                                                    .setNumber(customerNumber.get())
                                                    .build();
            customerNumber.incrementAndGet();
            final PersonName personName = PersonName.newBuilder()
                                                    .setGivenName("Kreat")
                                                    .setFamilyName("C'Ustomer")
                                                    .setHonorificSuffix("Cmd")
                                                    .build();
            final Customer customer = Customer.newBuilder()
                                              .setId(customerId)
                                              .setName(personName)
                                              .build();
            final Message msg = CreateCustomer.newBuilder()
                                              .setCustomerId(customerId)
                                              .setCustomer(customer)
                                              .build();
            final UserId userId = newUserId(Identifier.newUuid());
            final Command result = create(msg, userId, getCurrentTime());
            return result;
        }
    }

    static class AQuery {


        private static final ActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(AQuery.class);

        private AQuery() {}

        static Query readAllProjects() {
            // DO NOT replace the type name with another Project class.
            final Query result = requestFactory.query()
                                               .all(io.spine.test.projection.Project.class);
            return result;
        }
    }

    static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
        ProjectAggregateRepository(BoundedContext boundedContext) {
            super();
        }
    }

    private static class ProjectAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {
        // an aggregate constructor must be public because it is used via reflection
        @SuppressWarnings("PublicConstructorInNonPublicClass")
        public ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        ProjectCreated handle(CreateProject cmd, CommandContext ctx) {
            return EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        TaskAdded handle(AddTask cmd, CommandContext ctx) {
            return EventMessage.taskAdded(cmd.getProjectId());
        }

        @Assign
        List<ProjectStarted> handle(StartProject cmd, CommandContext ctx) {
            final ProjectStarted message = EventMessage.projectStarted(cmd.getProjectId());
            return newArrayList(message);
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.CREATED)
                    .build();
        }

        @Apply
        private void event(TaskAdded event) {
        }

        @Apply
        private void event(ProjectStarted event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Status.STARTED)
                    .build();
        }
    }

    public static class CustomerAggregateRepository
            extends AggregateRepository<CustomerId, CustomerAggregate> {
        public CustomerAggregateRepository(BoundedContext boundedContext) {
            super();
        }
    }

    public static class CustomerAggregate
            extends Aggregate<CustomerId, Customer, CustomerVBuilder> {

        @SuppressWarnings("PublicConstructorInNonPublicClass")
        // by convention (as it's used by Reflection).
        public CustomerAggregate(CustomerId id) {
            super(id);
        }

        @Assign
        CustomerCreated handle(CreateCustomer cmd, CommandContext ctx) {
            final CustomerCreated event = CustomerCreated.newBuilder()
                                                         .setCustomerId(cmd.getCustomerId())
                                                         .setCustomer(cmd.getCustomer())
                                                         .build();
            return event;
        }

        @Apply
        private void event(CustomerCreated event) {
            getBuilder().mergeFrom(event.getCustomer());
        }
    }
}
