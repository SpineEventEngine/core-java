/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventBusShould;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.event.Project;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.ProjectVBuilder;
import io.spine.test.event.Task;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.command.CreateProject;
import io.spine.test.event.command.EvAddTasks;
import io.spine.test.event.command.EvArchiveProject;
import io.spine.testdata.Sample;

import java.util.List;

import static io.spine.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * Test environment classes for the {@code server.event} package.
 */
public class EventBusTestEnv {

    private static final TenantId TENANT_ID = tenantId();
    private static final ProjectId PROJECT_ID = projectId();

    public static final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EventBusShould.class, TENANT_ID);

    private EventBusTestEnv() {
        // Prevent instantiation.
    }

    private static ProjectId projectId() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build();
        return id;
    }

    public static TenantId tenantId() {
        final String value = EventBusTestEnv.class.getName();
        final TenantId id = TenantId.newBuilder()
                                    .setValue(value)
                                    .build();
        return id;
    }

    public static CreateProject createProject() {
        final CreateProject command =
                ((CreateProject.Builder) Sample.builderForType(CreateProject.class))
                        .setProjectId(PROJECT_ID)
                        .build();
        return command;
    }

    /**
     * Returns an {@link EvArchiveProject} command with an unfilled required 
     * {@link EvArchiveProject#getReason()} field.
     */
    public static EvArchiveProject invalidArchiveProject() {
        final EvArchiveProject command =
                ((EvArchiveProject.Builder) Sample.builderForType(EvArchiveProject.class))
                        .setProjectId(PROJECT_ID)
                        .build();
        return command;
    }

    public static Command command(Message message) {
        return requestFactory.command()
                             .create(message);
    }

    /**
     * Reads all events from the event bus event store for a tenant specified by
     * the {@link EventBusTestEnv#TENANT_ID}.
     */
    public static List<Event> readEvents(final EventBus eventBus) {
        final MemoizingObserver<Event> observer = memoizingObserver();
        final TenantAwareOperation operation = new TenantAwareOperation(TENANT_ID) {
            @Override
            public void run() {
                eventBus.getEventStore()
                        .read(allEventsQuery(), observer);
            }
        };
        operation.execute();

        final List<Event> results = observer.responses();
        return results;
    }

    public static class ProjectRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
    }

    static class ProjectAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        private static ProjectCreated projectCreated(ProjectId projectId) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(projectId)
                                 .build();
        }

        private static TaskAdded taskAdded(ProjectId projectId, Task task) {
            return TaskAdded.newBuilder()
                            .setProjectId(projectId)
                            .setTask(task)
                            .build();
        }

        @Assign
        ProjectCreated on(CreateProject command, CommandContext ctx) {
            final ProjectCreated event = projectCreated(command.getProjectId());
            return event;
        }

        @Assign
        List<TaskAdded> on(EvAddTasks command, CommandContext ctx) {
            final ImmutableList.Builder<TaskAdded> events = ImmutableList.builder();

            for (Task task : command.getTaskList()) {
                final TaskAdded event = taskAdded(command.getProjectId(), task);
                events.add(event);
            }

            return events.build();
        }

        @Apply
        private void event(ProjectCreated event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .setStatus(Project.Status.CREATED);
        }

        @Apply
        private void event(TaskAdded event) {
            getBuilder()
                    .setId(event.getProjectId())
                    .addTask(event.getTask());
        }
    }

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    public static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    private static class EventMessage {

        private static final ProjectStarted PROJECT_STARTED = projectStarted(PROJECT_ID);

        private EventMessage() {}

        private static ProjectStarted projectStarted() {
            return PROJECT_STARTED;
        }

        private static ProjectCreated projectCreated(ProjectId id) {
            return ProjectCreated.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }

        private static ProjectStarted projectStarted(ProjectId id) {
            return ProjectStarted.newBuilder()
                                 .setProjectId(id)
                                 .build();
        }
    }

    public static class GivenEvent {

        private static final TestEventFactory factory =
                TestEventFactory.newInstance(pack(PROJECT_ID), GivenEvent.class);

        private GivenEvent() {}

        private static TestEventFactory eventFactory() {
            return factory;
        }

        public static Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static Event projectStarted() {
            final ProjectStarted msg = EventMessage.projectStarted();
            final Event event = eventFactory().createEvent(msg);
            return event;
        }

        public static Event projectCreated(ProjectId projectId) {
            final ProjectCreated msg = EventMessage.projectCreated(projectId);
            final Event event = eventFactory().createEvent(msg);
            return event;
        }
    }
}
