/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.client.Query;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.people.PersonName;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.model.Nothing;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAddTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.event.AggTaskAdded;
import io.spine.test.bc.event.BcProjectCreated;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.commandservice.customer.CustomerId;
import io.spine.test.commandservice.customer.CustomerVBuilder;
import io.spine.test.commandservice.customer.command.CreateCustomer;
import io.spine.test.commandservice.customer.event.CustomerCreated;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.core.given.GivenUserId;
import io.spine.time.LocalDate;
import io.spine.time.LocalDates;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;

public class Given {

    private Given() {
    }

    static class EventMessage {

        private EventMessage() {
        }

        static AggTaskAdded taskAdded(ProjectId id) {
            return AggTaskAdded.newBuilder()
                               .setProjectId(id)
                               .build();
        }

        static AggProjectCreated projectCreated(ProjectId id) {
            return AggProjectCreated.newBuilder()
                                    .setProjectId(id)
                                    .build();
        }

        static AggProjectStarted projectStarted(ProjectId id) {
            return AggProjectStarted.newBuilder()
                                    .setProjectId(id)
                                    .build();
        }
    }

    static class CommandMessage {

        private CommandMessage() {
        }

        public static AggCreateProject createProject(ProjectId id) {
            return AggCreateProject.newBuilder()
                                   .setProjectId(id)
                                   .build();
        }
    }

    static class ACommand {

        private static final UserId USER_ID = GivenUserId.newUuid();
        private static final ProjectId PROJECT_ID = newProjectId();

        /* This hack is just for the testing purposes.
        The production code should use more sane approach to generating the IDs. */
        private static final AtomicInteger customerNumber = new AtomicInteger(1);

        private ACommand() {
        }

        /**
         * Creates a new {@code ACommand} with the given command message, userId and
         * timestamp using default {@code ACommand} instance.
         */
        private static Command create(io.spine.base.CommandMessage command, UserId userId, Timestamp when) {
            TenantId generatedTenantId = TenantId.newBuilder()
                                                 .setValue(newUuid())
                                                 .build();
            TestActorRequestFactory factory =
                    new TestActorRequestFactory(userId, generatedTenantId);
            Command result = factory.createCommand(command, when);
            return result;
        }

        static Command createProject() {
            return createProject(currentTime());
        }

        private static Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        private static Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            AggCreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        static Command createCustomer() {
            LocalDate localDate = LocalDates.now();
            CustomerId customerId = CustomerId
                    .newBuilder()
                    .setRegistrationDate(localDate)
                    .setNumber(customerNumber.get())
                    .build();
            customerNumber.incrementAndGet();
            PersonName personName = PersonName
                    .newBuilder()
                    .setGivenName("Kreat")
                    .setFamilyName("C'Ustomer")
                    .setHonorificSuffix("Cmd")
                    .build();
            Customer customer = Customer
                    .newBuilder()
                    .setId(customerId)
                    .setName(personName)
                    .build();
            io.spine.base.CommandMessage msg = CreateCustomer
                    .newBuilder()
                    .setCustomerId(customerId)
                    .setCustomer(customer)
                    .build();
            UserId userId = GivenUserId.of(Identifier.newUuid());
            Command result = create(msg, userId, currentTime());
            return result;
        }

        private static ProjectId newProjectId() {
            String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    static class AQuery {

        private static final ActorRequestFactory requestFactory =
                new TestActorRequestFactory(AQuery.class);

        private AQuery() {
        }

        static Query readAllProjects() {
            // DO NOT replace the type name with another Project class.
            Query result = requestFactory.query()
                                         .all(io.spine.test.projection.Project.class);
            return result;
        }

        static Query readUnknownType() {
            Query result = requestFactory.query()
                                         .all(Nothing.class);
            return result;
        }
    }

    static class ProjectAggregateRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {

        ProjectAggregateRepository() {
            super();
        }

        @Override
        protected EntityLifecycle lifecycleOf(ProjectId id) {
            return super.lifecycleOf(id);
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
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            return EventMessage.projectCreated(cmd.getProjectId());
        }

        @Assign
        AggTaskAdded handle(AggAddTask cmd, CommandContext ctx) {
            return EventMessage.taskAdded(cmd.getProjectId());
        }

        @Assign
        List<AggProjectStarted> handle(AggStartProject cmd, CommandContext ctx) {
            AggProjectStarted message = EventMessage.projectStarted(cmd.getProjectId());
            return ImmutableList.of(message);
        }

        @Apply
        private void event(AggProjectCreated event) {
            builder().setId(event.getProjectId())
                     .setStatus(Status.CREATED);
        }

        @Apply
        private void event(AggTaskAdded event) {
        }

        @Apply
        private void event(AggProjectStarted event) {
            builder().setId(event.getProjectId())
                     .setStatus(Status.STARTED);
        }
    }

    public static class CustomerAggregateRepository
            extends AggregateRepository<CustomerId, CustomerAggregate> {

        public CustomerAggregateRepository() {
            super();
        }

        @Override
        public EntityLifecycle lifecycleOf(CustomerId id) {
            return super.lifecycleOf(id);
        }
    }

    public static class CustomerAggregate
            extends Aggregate<CustomerId, Customer, CustomerVBuilder> {

        @Assign
        CustomerCreated handle(CreateCustomer cmd, CommandContext ctx) {
            CustomerCreated event = CustomerCreated
                    .newBuilder()
                    .setCustomerId(cmd.getCustomerId())
                    .setCustomer(cmd.getCustomer())
                    .build();
            return event;
        }

        @Apply
        private void event(CustomerCreated event) {
            builder().mergeFrom(event.getCustomer());
        }
    }

    /*
     * `QueryServiceTest` environment.
     ***************************************************/

    static final String PROJECTS_CONTEXT_NAME = "Projects";

    static class ProjectDetailsRepository
            extends ProjectionRepository<io.spine.test.commandservice.ProjectId,
                                         ProjectDetails,
                                         io.spine.test.projection.Project> {
    }

    static class ProjectDetails
            extends Projection<io.spine.test.commandservice.ProjectId,
                               io.spine.test.projection.Project,
                               io.spine.test.projection.ProjectVBuilder> {

        private ProjectDetails(io.spine.test.commandservice.ProjectId id) {
            super(id);
        }

        @SuppressWarnings("UnusedParameters") // OK for test method.
        @Subscribe
        void on(BcProjectCreated event, EventContext context) {
            // Do nothing.
        }
    }
}
