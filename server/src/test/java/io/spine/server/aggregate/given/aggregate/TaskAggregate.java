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

package io.spine.server.aggregate.given.aggregate;

import com.google.common.base.Optional;
import io.spine.core.React;
import io.spine.core.UserId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.tuple.Pair;
import io.spine.test.aggregate.command.AggAssignTask;
import io.spine.test.aggregate.command.AggCreateTask;
import io.spine.test.aggregate.command.AggReassignTask;
import io.spine.test.aggregate.event.AggTaskAssigned;
import io.spine.test.aggregate.event.AggTaskCreated;
import io.spine.test.aggregate.event.AggUserNotified;
import io.spine.test.aggregate.rejection.AggCannotReassignUnassignedTask;
import io.spine.test.aggregate.rejection.Rejections;
import io.spine.test.aggregate.task.AggTask;
import io.spine.test.aggregate.task.AggTaskId;
import io.spine.test.aggregate.task.AggTaskVBuilder;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An aggregate that fires a {@linkplain Pair pair} with an optional upon handling a command,
 * an event or a rejection.
 *
 * @author Alexander Yevsyukkov
 * @see io.spine.server.aggregate.AggregateTest#createSingleEventForPairFromCommandDispatch
 * @see io.spine.server.aggregate.AggregateTest#createSingleEventForPairFromEventReact
 * @see io.spine.server.aggregate.AggregateTest#createSingleEventForPairFromRejectionReact
 */
public class TaskAggregate extends Aggregate<AggTaskId, AggTask, AggTaskVBuilder> {

    private static final UserId EMPTY_USER_ID = UserId.getDefaultInstance();

    protected TaskAggregate(AggTaskId id) {
        super(id);
    }

    /**
     * A command handler that returns a pair with an optional second element.
     *
     * <p>{@link AggTaskAssigned} is present when the command contains an
     * {@linkplain AggCreateTask#getAssignee() assignee}.
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
     * {@linkplain UserId assignee} is a default empty instance returns {@code null}.
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
