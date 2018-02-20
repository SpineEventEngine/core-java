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

package io.spine.server.aggregate.given;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.React;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.tuple.Pair;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggAssignTask;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggCreateTask;
import io.spine.test.aggregate.command.AggReassignTask;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggTaskAssigned;
import io.spine.test.aggregate.event.AggTaskCreated;
import io.spine.test.aggregate.event.AggUserNotified;
import io.spine.test.aggregate.rejection.AggCannotReassignUnassignedTask;
import io.spine.test.aggregate.rejection.Rejections;
import io.spine.test.aggregate.task.AggTask;
import io.spine.test.aggregate.task.AggTaskId;
import io.spine.test.aggregate.task.AggTaskVBuilder;
import io.spine.test.aggregate.user.User;
import io.spine.test.aggregate.user.UserVBuilder;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.List;

import static io.spine.Identifier.newUuid;
import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;

/**
 * @author Alexander Yevsyukov
 * @author Mykhailo Drachuk
 */
public class AggregateTestEnv {

    private AggregateTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    public static List<Event> readAllEvents(final BoundedContext boundedContext, TenantId tenantId) {
        final MemoizingObserver<Event> queryObserver = memoizingObserver();
        final TenantAwareOperation operation = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                boundedContext.getEventBus()
                              .getEventStore()
                              .read(allEventsQuery(), queryObserver);
            }
        };
        operation.execute();

        final List<Event> responses = queryObserver.responses();
        return responses;
    }

    /**
     * Creates a new {@link EventStreamQuery} without any filters.
     */
    private static EventStreamQuery allEventsQuery() {
        return EventStreamQuery.newBuilder()
                               .build();
    }

    private static AggTaskId newTaskId() {
        return AggTaskId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    private static UserId newUserId() {
        return UserId.newBuilder()
                     .setValue(newUuid())
                     .build();
    }
 
    public static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    /**
     * Creates a new multitenant bounded context with a registered {@link TaskAggregateRepository}.
     */
    public static BoundedContext newTaskBoundedContext() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setMultitenant(true)
                                                            .build();
        boundedContext.register(new TaskAggregateRepository());
        return boundedContext;
    }

    public static AggCreateTask createTask() {
        return AggCreateTask.newBuilder()
                            .setTaskId(newTaskId())
                            .build();
    }

    public static AggAssignTask assignTask() {
        return AggAssignTask.newBuilder()
                            .setTaskId(newTaskId())
                            .setAssignee(newUserId())
                            .build();
    }

    public static AggReassignTask reassignTask() {
        return AggReassignTask.newBuilder()
                            .setTaskId(newTaskId())
                            .setAssignee(newUserId())
                            .build();
    }

    /**
     * Obtains the {@link TypeUrl} of the message from the provided event.
     */
    public static TypeUrl typeUrlOf(Event event) {
        final Any message = event.getMessage();
        final TypeUrl result = TypeUrl.parse(message.getTypeUrl());
        return result;
    }

    /**
     * The test environment class for test of missing command handler or missing event applier.
     */
    public static class AggregateWithMissingApplier
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        private boolean createProjectCommandHandled = false;

        public AggregateWithMissingApplier(ProjectId id) {
            super(id);
        }

        /** There is no event applier for ProjectCreated event (intentionally). */
        @Assign
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            createProjectCommandHandled = true;
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        public boolean isCreateProjectCommandHandled() {
            return createProjectCommandHandled;
        }
    }

    /**
     * An aggregate with {@code Integer} ID.
     */
    public static class IntAggregate extends Aggregate<Integer, Project, ProjectVBuilder> {
        public IntAggregate(Integer id) {
            super(id);
        }
    }

    /**
     * The test environment class for checking raising and catching exceptions.
     */
    public static class FaultyAggregate
            extends Aggregate<ProjectId, Project, ProjectVBuilder> {

        public static final String BROKEN_HANDLER = "broken_handler";
        public static final String BROKEN_APPLIER = "broken_applier";

        private final boolean brokenHandler;
        private final boolean brokenApplier;

        public FaultyAggregate(ProjectId id, boolean brokenHandler, boolean brokenApplier) {
            super(id);
            this.brokenHandler = brokenHandler;
            this.brokenApplier = brokenApplier;
        }

        @Assign
        AggProjectCreated handle(AggCreateProject cmd, CommandContext ctx) {
            if (brokenHandler) {
                throw new IllegalStateException(BROKEN_HANDLER);
            }
            return projectCreated(cmd.getProjectId(), cmd.getName());
        }

        @Apply
        private void event(AggProjectCreated event) {
            if (brokenApplier) {
                throw new IllegalStateException(BROKEN_APPLIER);
            }

            getBuilder().setStatus(Status.CREATED);
        }
    }

    /**
     * The test environment aggregate for testing validation during aggregate state transition.
     */
    public static class UserAggregate extends Aggregate<String, User, UserVBuilder> {
        private UserAggregate(String id) {
            super(id);
        }
    }

    public static class TaskAggregateRepository
            extends AggregateRepository<AggTaskId, TaskAggregate> { }

    /**
     * An aggregate that fires a {@linkplain Pair pair} with an optional upon handling a command, 
     * an event or a rejection.
     *
     * @see io.spine.server.aggregate.AggregateShould#create_single_event_for_a_pair_of_events_with_empty_for_a_command_dispatch
     * @see io.spine.server.aggregate.AggregateShould#create_single_event_for_a_pair_of_events_with_empty_for_an_event_react
     * @see io.spine.server.aggregate.AggregateShould#create_single_event_for_a_pair_of_events_with_empty_for_a_rejection_react
     */
    public static class TaskAggregate extends Aggregate<AggTaskId, AggTask, AggTaskVBuilder> {

        private static final UserId EMPTY_USER_ID = UserId.getDefaultInstance();

        protected TaskAggregate(AggTaskId id) {
            super(id);
        }

        /**
         * A command handler that returns a pair with an optional second element.
         *
         * <p>{@link AggTaskAssigned} is present when the command contains an
         * {@link AggCreateTask#getAssignee() assignee}.
         */
        @Assign
        Pair<AggTaskCreated, Optional<AggTaskAssigned>> handle(AggCreateTask command) {
            final AggTaskId id = command.getTaskId();
            final AggTaskCreated createdEvent = taskCreated(id);

            final UserId assignee = command.getAssignee();
            final AggTaskAssigned assignedEvent = taskAssignedOrNull(id, assignee);

            return Pair.withNullable(createdEvent, assignedEvent);
        }

        private static AggTaskCreated taskCreated(AggTaskId id) {
            return AggTaskCreated.newBuilder()
                                 .setTaskId(id)
                                 .build();
        }

        /**
         * Creates a new {@link AggTaskAssigned} event message with provided values. If the
         * {@link UserId assignee} is a default empty instance returns {@code null}.
         */
        @Nullable
        private static AggTaskAssigned taskAssignedOrNull(AggTaskId id, UserId assignee) {
            final UserId emptyUserId = UserId.getDefaultInstance();
            if (assignee.equals(emptyUserId)) {
                return null;
            }
            final AggTaskAssigned event =
                    AggTaskAssigned.newBuilder()
                                   .setTaskId(id)
                                   .setNewAssignee(assignee)
                                   .build();
            return event;
        }

        @Assign
        AggTaskAssigned handle(AggAssignTask command) {
            final AggTaskId id = command.getTaskId();
            final UserId newAssignee = command.getAssignee();
            final UserId previousAssignee = getState().getAssignee();
            
            final AggTaskAssigned event = taskAssigned(id, previousAssignee, newAssignee);
            return event;
        }

        @Assign
        AggTaskAssigned handle(AggReassignTask command)
                throws AggCannotReassignUnassignedTask {
            final AggTaskId id = command.getTaskId();
            final UserId newAssignee = command.getAssignee();
            final UserId previousAssignee = getState().getAssignee();

            if (previousAssignee.equals(EMPTY_USER_ID)) {
                throw new AggCannotReassignUnassignedTask(id, previousAssignee);
            }

            final AggTaskAssigned event = taskAssigned(id, previousAssignee, newAssignee);
            return event;
        }

        private static AggTaskAssigned taskAssigned(AggTaskId id,
                                                    UserId previousAssignee,
                                                    UserId newAssignee) {
            return AggTaskAssigned.newBuilder()
                                  .setTaskId(id)
                                  .setPreviousAssignee(previousAssignee)
                                  .setNewAssignee(newAssignee)
                                  .build();
        }

        @Apply
        void event(AggTaskCreated event) {
            getBuilder().setId(event.getTaskId());
        }

        @Apply
        void event(AggTaskAssigned event) {
            getBuilder().setAssignee(event.getNewAssignee());
        }

        @React
        Pair<AggUserNotified, Optional<AggUserNotified>>
        on(AggTaskAssigned event) {
            final AggTaskId taskId = event.getTaskId();
            final UserId previousAssignee = event.getPreviousAssignee();
            final AggUserNotified previousAssigneeNotified =
                    userNotifiedOrNull(taskId, previousAssignee);
            final UserId newAssignee = event.getNewAssignee();
            final AggUserNotified newAssigneeNotified = userNotified(taskId, newAssignee);
            return Pair.withNullable(newAssigneeNotified, previousAssigneeNotified);
        }

        @Nullable
        private static AggUserNotified userNotifiedOrNull(AggTaskId taskId, UserId userId) {
            if (userId.equals(EMPTY_USER_ID)) {
                return null;
            }
            final AggUserNotified event = userNotified(taskId, userId);
            return event;
        }

        private static AggUserNotified userNotified(AggTaskId taskId, UserId userId) {
            final AggUserNotified event =
                    AggUserNotified.newBuilder()
                                   .setTaskId(taskId)
                                   .setUserId(userId)
                                   .build();
            return event;
        }

        @React
        Pair<AggUserNotified, Optional<AggUserNotified>>
        on(Rejections.AggCannotReassignUnassignedTask rejection) {
            final AggUserNotified event = userNotified(rejection.getTaskId(), 
                                                       rejection.getUserId());
            return Pair.withNullable(event, null);
        }
        
        @Apply
        void event(AggUserNotified event) {
            // Do nothing.
        }
    }
}
