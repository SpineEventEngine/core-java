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

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.base.Identifiers;
import org.spine3.people.PersonName;
import org.spine3.test.clientservice.ProjectId;
import org.spine3.test.clientservice.command.CreateProject;
import org.spine3.test.clientservice.customer.Customer;
import org.spine3.test.clientservice.customer.CustomerId;
import org.spine3.test.clientservice.customer.command.CreateCustomer;
import org.spine3.test.clientservice.event.ProjectCreated;
import org.spine3.test.clientservice.event.ProjectStarted;
import org.spine3.test.clientservice.event.TaskAdded;
import org.spine3.time.LocalDate;
import org.spine3.time.LocalDates;
import org.spine3.users.UserId;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;


@SuppressWarnings("EmptyClass")
/* package */ class Given {

    /* package */ static class AggregateId {

        private AggregateId() {}

        /* package */ static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    /* package */ static class EventMessage {

        private EventMessage() {}

        /* package */ static TaskAdded taskAdded(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        /* package */ static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        /* package */ static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    /* package */ static class CommandMessage {

        private CommandMessage() {
        }

        public static CreateProject createProject(ProjectId id) {
            return CreateProject.newBuilder()
                                .setProjectId(id)
                                .build();
        }
    }

    /* package */ static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Command() {}

        /**
         * Creates a new {@link Command} with the given command message, userId and timestamp using default
         * {@link Command} instance.
         */
        /* package */ static org.spine3.base.Command create(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.create(command, context);
            return result;
        }

        /* package */ static org.spine3.base.Command createProject() {
            return createProject(getCurrentTime());
        }

        /* package */ static org.spine3.base.Command createProject(Timestamp when) {
            return createProject(USER_ID, PROJECT_ID, when);
        }

        /* package */ static org.spine3.base.Command createProject(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = CommandMessage.createProject(projectId);
            return create(command, userId, when);
        }

        @SuppressWarnings("StaticNonFinalField") /* This hack is just for the testing purposes.
        The production code should use more sane approach to generating the IDs. */
        private static int customerNumber = 1;

        /* package */ static org.spine3.base.Command createCustomer() {
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
            final org.spine3.base.Command result = Given.Command.create(msg, userId, TimeUtil.getCurrentTime());

            return result;
        }
    }
}
