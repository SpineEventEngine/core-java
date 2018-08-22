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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Status;
import io.spine.core.Subscribe;
import io.spine.core.TenantId;
import io.spine.grpc.MemoizingObserver;
import io.spine.json.Json;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.bus.BusFilter;
import io.spine.server.command.Assign;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.Enricher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventBusTest;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.event.EBProjectArchived;
import io.spine.test.event.EBProjectCreated;
import io.spine.test.event.EBTaskAdded;
import io.spine.test.event.Project;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.ProjectVBuilder;
import io.spine.test.event.Task;
import io.spine.test.event.command.EBAddTasks;
import io.spine.test.event.command.EBArchiveProject;
import io.spine.test.event.command.EBCreateProject;
import io.spine.testdata.Sample;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.core.EventValidationError.UNSUPPORTED_EVENT_VALUE;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.bus.Buses.reject;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test environment classes for the {@code server.event} package.
 */
public class EventBusTestEnv {

    private static final TenantId TENANT_ID = tenantId();
    private static final ProjectId PROJECT_ID = projectId();

    public static final ActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EventBusTest.class, TENANT_ID);

    private EventBusTestEnv() {
        // Prevent instantiation.
    }

    private static ProjectId projectId() {
        ProjectId id = ProjectId.newBuilder()
                                .setId(newUuid())
                                .build();
        return id;
    }

    private static TenantId tenantId() {
        String value = EventBusTestEnv.class.getName();
        TenantId id = TenantId.newBuilder()
                              .setValue(value)
                              .build();
        return id;
    }

    public static EBCreateProject createProject() {
        EBCreateProject command =
                ((EBCreateProject.Builder) Sample.builderForType(EBCreateProject.class))
                        .setProjectId(PROJECT_ID)
                        .build();
        return command;
    }

    public static EBAddTasks addTasks(Task... tasks) {
        EBAddTasks.Builder builder =
                ((EBAddTasks.Builder) Sample.builderForType(EBAddTasks.class))
                        .setProjectId(PROJECT_ID)
                        .clearTask();
        for (Task task : tasks) {
            builder.addTask(task);
        }
        EBAddTasks command = builder.build();
        return command;
    }

    public static Task newTask(boolean done) {
        Task task = ((Task.Builder) Sample.builderForType(Task.class))
                                          .setDone(done)
                                          .build();
        return task;
    }

    /**
     * Returns an {@link EBArchiveProject} command with an unfilled required
     * {@link EBArchiveProject#getReason()} field.
     */
    public static EBArchiveProject invalidArchiveProject() {
        EBArchiveProject command =
                ((EBArchiveProject.Builder) Sample.builderForType(EBArchiveProject.class))
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
    public static List<Event> readEvents(EventBus eventBus) {
        MemoizingObserver<Event> observer = memoizingObserver();
        TenantAwareOperation operation = new TenantAwareOperation(TENANT_ID) {
            @Override
            public void run() {
                eventBus.getEventStore()
                        .read(allEventsQuery(), observer);
            }
        };
        operation.execute();

        List<Event> results = observer.responses();
        return results;
    }

    @SuppressWarnings("CheckReturnValue") // Conditionally calling builder.
    public static EventBus.Builder eventBusBuilder(@Nullable Enricher enricher) {
        EventBus.Builder busBuilder = EventBus
                .newBuilder()
                .appendFilter(new TaskCreatedFilter());
        if (enricher != null) {
            busBuilder.setEnricher(enricher);
        }
        return busBuilder;
    }

    public static class ProjectRepository
            extends AggregateRepository<ProjectId, ProjectAggregate> {
    }

    static class ProjectAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private ProjectAggregate(ProjectId id) {
            super(id);
        }

        @Assign
        EBProjectCreated on(EBCreateProject command, CommandContext ctx) {
            EBProjectCreated event = projectCreated(command.getProjectId());
            return event;
        }

        @Assign
        List<EBTaskAdded> on(EBAddTasks command, CommandContext ctx) {
            ImmutableList.Builder<EBTaskAdded> events = ImmutableList.builder();

            for (Task task : command.getTaskList()) {
                EBTaskAdded event = taskAdded(command.getProjectId(), task);
                events.add(event);
            }

            return events.build();
        }

        @Apply
        void event(EBProjectCreated event) {
            getBuilder().setId(event.getProjectId())
                        .setStatus(Project.Status.CREATED);
        }

        @Apply
        void event(EBTaskAdded event) {
            getBuilder().setId(event.getProjectId())
                        .addTask(event.getTask());
        }

        private static EBProjectCreated projectCreated(ProjectId projectId) {
            return EBProjectCreated.newBuilder()
                                   .setProjectId(projectId)
                                   .build();
        }

        private static EBTaskAdded taskAdded(ProjectId projectId, Task task) {
            return EBTaskAdded.newBuilder()
                              .setProjectId(projectId)
                              .setTask(task)
                              .build();
        }
    }

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    /**
     * Filters out the {@link EBTaskAdded} events which have their {@link Task#getDone()}
     * property set to {@code true}.
     */
    public static class TaskCreatedFilter implements BusFilter<EventEnvelope> {

        private static final EventClass TASK_ADDED_CLASS = EventClass.from(EBTaskAdded.class);

        @Override
        public Optional<Ack> accept(EventEnvelope envelope) {
            if (TASK_ADDED_CLASS.equals(envelope.getMessageClass())) {
                EBTaskAdded message = (EBTaskAdded) envelope.getMessage();
                Task task = message.getTask();
                if (task.getDone()) {
                    Error error = error();
                    Any packedId = Identifier.pack(envelope.getId());
                    Ack result = reject(packedId, error);
                    return Optional.of(result);
                }
            }
            return empty();
        }

        private static Error error() {
            return Error.newBuilder()
                        .setMessage("The task cannot be created in a 'completed' state.")
                        .build();
        }
    }

    /**
     * {@link EBProjectCreated} subscriber that does nothing.
     *
     * <p>Can be used for the event to get pass
     * the {@link io.spine.server.bus.DeadMessageFilter DeadMessageFilter}.
     */
    public static class EBProjectCreatedNoOpSubscriber extends AbstractEventSubscriber {

        @Subscribe
        void on(EBProjectCreated message, EventContext context) {
            // Do nothing.
        }
    }

    public static class EBProjectArchivedSubscriber extends AbstractEventSubscriber {

        private Message eventMessage;

        @Subscribe
        void on(EBProjectArchived message, EventContext ignored) {
            this.eventMessage = message;
        }

        public Message getEventMessage() {
            return eventMessage;
        }
    }

    public static class ProjectCreatedSubscriber extends AbstractEventSubscriber {

        private Message eventMessage;
        private EventContext eventContext;

        @Subscribe
        void on(ProjectCreated eventMsg, EventContext context) {
            this.eventMessage = eventMsg;
            this.eventContext = context;
        }

        public Message getEventMessage() {
            return eventMessage;
        }

        public EventContext getEventContext() {
            return eventContext;
        }
    }

    /**
     * {@link EBTaskAdded} subscriber that does nothing. Can be used for the event to get pass the
     * the {@link io.spine.server.bus.DeadMessageFilter DeadMessageFilter}.
     */
    public static class EBTaskAddedNoOpSubscriber extends AbstractEventSubscriber {

        @Subscribe
        void on(EBTaskAdded message, EventContext context) {
            // Do nothing.
        }
    }

    public static class EBExternalTaskAddedSubscriber extends AbstractEventSubscriber {

        @Subscribe(external = true)
        void on(EBTaskAdded message, EventContext context) {
            if (!context.getExternal()) {
                fail(format(
                        "Domestic event %s was delivered to an external subscriber.",
                        message.getClass()
                ));
            }
        }

        /**
         * Must be present in order for the subscriber to be valid for EventBus registration.
         *
         * <p>This subscriber should never be called.
         *
         * @param event ignored
         */
        @Subscribe
        void on(ProjectCreated event) {
            fail("Unexpected event " + Json.toJson(event));
        }
    }

    /**
     * A simple dispatcher class, which only dispatch and does not have own event
     * subscribing methods.
     */
    public static class BareDispatcher implements EventDispatcher<String> {

        private boolean dispatchCalled = false;

        @Override
        public Set<EventClass> getMessageClasses() {
            return ImmutableSet.of(EventClass.from(ProjectCreated.class));
        }

        @Override
        public Set<String> dispatch(EventEnvelope event) {
            dispatchCalled = true;
            return identity();
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        public boolean isDispatchCalled() {
            return dispatchCalled;
        }
    }

    public static class EventMessage {

        private static final ProjectStarted PROJECT_STARTED = projectStarted(PROJECT_ID);

        private EventMessage() {
        }

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

        private GivenEvent() {
        }

        private static TestEventFactory eventFactory() {
            return factory;
        }

        public static Event projectCreated() {
            return projectCreated(PROJECT_ID);
        }

        public static Event projectStarted() {
            ProjectStarted msg = EventMessage.projectStarted();
            Event event = eventFactory().createEvent(msg);
            return event;
        }

        public static Event projectCreated(ProjectId projectId) {
            ProjectCreated msg = EventMessage.projectCreated(projectId);
            Event event = eventFactory().createEvent(msg);
            return event;
        }
    }

    public static class UnsupportedEventAckObserver implements StreamObserver<Ack> {

        private boolean intercepted;
        private boolean completed;

        public UnsupportedEventAckObserver() {
            this.intercepted = false;
            this.completed = false;
        }

        @Override
        public void onNext(Ack value) {
            Status status = value.getStatus();
            if (status.getStatusCase() == ERROR) {
                Error error = status.getError();
                int code = error.getCode();
                assertEquals(UNSUPPORTED_EVENT_VALUE, code);
            } else {
                fail(Json.toJson(value));
            }
            intercepted = true;
        }

        @Override
        public void onError(Throwable t) {
            fail(t);
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        public boolean isCompleted() {
            return completed;
        }

        public boolean observedUnsupportedEvent() {
            return intercepted;
        }
    }
}
