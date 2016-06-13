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


import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.EventContext;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.clientservice.service.ProjectId;
import org.spine3.test.clientservice.service.command.AddTask;
import org.spine3.test.clientservice.service.command.CreateProject;
import org.spine3.test.clientservice.service.command.StartProject;
import org.spine3.test.clientservice.service.event.ProjectCreated;
import org.spine3.test.clientservice.service.event.ProjectStarted;
import org.spine3.test.clientservice.service.event.TaskAdded;
import org.spine3.users.UserId;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.mockito.Mockito.spy;
import static org.spine3.base.Commands.create;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.UserUtil.newUserId;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestCommandContextFactory.createCommandContext;

/* package */ class Given {

    /* package */ static class AggregateId {

        private AggregateId() {
        }

        public static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }

    }

    /* package */ static class EventMessage {

        private EventMessage() {
        }

        public static TaskAdded taskAddedMsg(ProjectId id) {
            return TaskAdded.newBuilder()
                            .setProjectId(id)
                            .build();
        }

        public static ProjectCreated projectCreatedMsg(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        public static ProjectStarted projectStartedMsg(ProjectId id) {
            return ProjectStarted.newBuilder().setProjectId(id).build();
        }

    }

    /* package */ static class Command {

        private static final UserId USER_ID = newUserId(newUuid());
        private static final ProjectId PROJECT_ID = AggregateId.newProjectId();

        private Command() {
        }

        /**
         * Creates a new command bus with the given storage factory.
         */
        public static CommandBus newCommandBus(StorageFactory storageFactory) {
            final CommandStore store = new CommandStore(storageFactory.createCommandStorage());
            final CommandBus commandBus = CommandBus.newInstance(store);
            return commandBus;
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given command, userId and timestamp using default
         * {@link CommandId} instance.
         */
        public static org.spine3.base.Command createCommand(Message command, UserId userId, Timestamp when) {
            final CommandContext context = createCommandContext(userId, Commands.generateId(), when);
            final org.spine3.base.Command result = Commands.create(command, context);
            return result;
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with default properties (current time etc).
         */
        public static org.spine3.base.Command createProjectCmd() {
            return createProjectCmd(getCurrentTime());
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given timestamp.
         */
        public static org.spine3.base.Command createProjectCmd(Timestamp when) {
            return createProjectCmd(USER_ID, PROJECT_ID, when);
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given delay.
         */
        public static org.spine3.base.Command createProjectCmd(Duration delay) {
            final org.spine3.base.Command cmd = create(createProjectMsg(), createCommandContext(delay));
            return cmd;
        }

        /**
         * Creates a new {@link org.spine3.base.Command} with the given userId, projectId and timestamp.
         */
        public static org.spine3.base.Command createProjectCmd(UserId userId, ProjectId projectId, Timestamp when) {
            final CreateProject command = createProjectMsg(projectId);
            return createCommand(command, userId, when);
        }

        /**
         * Creates a new {@link CreateProject} command with the generated project ID.
         */
        public static CreateProject createProjectMsg() {
            return CreateProject.newBuilder()
                                .setProjectId(AggregateId.newProjectId())
                                .build();
        }

        /**
         * Creates a new {@link CreateProject} command with the given project ID.
         */
        public static CreateProject createProjectMsg(ProjectId id) {
            return CreateProject.newBuilder()
                                .setProjectId(id)
                                .build();
        }

        /**
         * Creates {@link org.spine3.test.project.command.CreateProject} command for the passed project ID.
         */
        public static CreateProject createProjectMsg(String projectId) {
            return CreateProject.newBuilder()
                                .setProjectId(
                                        ProjectId.newBuilder()
                                                 .setId(projectId)
                                                 .build())
                                .build();
        }
    }

    /* package */ static class Event {

        private Event() {
        }

        /**
         * Creates a new event bus with the given storage factory.
         */
        public static EventBus newEventBus(StorageFactory storageFactory) {
            final EventStore store = EventStore.newBuilder()
                                               .setStreamExecutor(MoreExecutors.directExecutor())
                                               .setStorage(storageFactory.createEventStorage())
                                               .build();
            final EventBus eventBus = EventBus.newInstance(store);
            return eventBus;
        }
    }

    /* package */ static class BoundedContextTestStubs {

        public static BoundedContext create() {
            final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
            return create(storageFactory);
        }

        public static BoundedContext create(StorageFactory storageFactory) {
            return create(BoundedContext.DEFAULT_NAME, storageFactory);
        }

        public static BoundedContext create(String name, StorageFactory storageFactory) {
            final CommandBus commandBus = Command.newCommandBus(storageFactory);
            final EventBus eventBus = Event.newEventBus(storageFactory);
            final BoundedContext.Builder builder = BoundedContext.newBuilder()
                                                                 .setName(name)
                                                                 .setStorageFactory(storageFactory)
                                                                 .setCommandBus(spy(commandBus))
                                                                 .setEventBus(spy(eventBus));
            return builder.build();

        }

        private BoundedContextTestStubs() {}
    }
}
