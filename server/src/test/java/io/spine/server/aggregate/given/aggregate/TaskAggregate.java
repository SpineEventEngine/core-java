/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.core.UserId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * An aggregate that fires a {@linkplain Pair pair} with an optional upon handling a command,
 * an event or a rejection.
 *
 * @see io.spine.server.aggregate.AggregateTest.CreateSingleEventForPair
 */
public class TaskAggregate extends Aggregate<AggTaskId, AggTask, AggTask.Builder> {

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
        AggTaskId id = command.getTaskId();
        AggTaskCreated createdEvent = taskCreated(id);

        UserId assignee = command.getAssignee();
        AggTaskAssigned assignedEvent = taskAssignedOrNull(id, assignee);

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
    private static @Nullable AggTaskAssigned taskAssignedOrNull(AggTaskId id, UserId assignee) {
        UserId emptyUserId = UserId.getDefaultInstance();
        if (assignee.equals(emptyUserId)) {
            return null;
        }
        AggTaskAssigned event = AggTaskAssigned.newBuilder()
                                               .setTaskId(id)
                                               .setNewAssignee(assignee)
                                               .build();
        return event;
    }

    @Assign
    AggTaskAssigned handle(AggAssignTask command) {
        AggTaskId id = command.getTaskId();
        UserId newAssignee = command.getAssignee();
        UserId previousAssignee = state().getAssignee();

        AggTaskAssigned event = taskAssigned(id, previousAssignee, newAssignee);
        return event;
    }

    @Assign
    AggTaskAssigned handle(AggReassignTask command)
            throws AggCannotReassignUnassignedTask {
        AggTaskId id = command.getTaskId();
        UserId newAssignee = command.getAssignee();
        UserId previousAssignee = state().getAssignee();

        if (previousAssignee.equals(EMPTY_USER_ID)) {
            throw AggCannotReassignUnassignedTask
                    .newBuilder()
                    .setTaskId(id)
                    .setUserId(previousAssignee)
                    .build();
        }

        AggTaskAssigned event = taskAssigned(id, previousAssignee, newAssignee);
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
    private void event(AggTaskCreated event) {
        builder().setId(event.getTaskId());
    }

    @Apply
    private void event(AggTaskAssigned event) {
        builder().setAssignee(event.getNewAssignee());
    }

    @React
    Pair<AggUserNotified, Optional<AggUserNotified>>
    on(AggTaskAssigned event) {
        AggTaskId taskId = event.getTaskId();
        UserId previousAssignee = event.getPreviousAssignee();
        AggUserNotified previousAssigneeNotified = userNotifiedOrNull(taskId, previousAssignee);
        UserId newAssignee = event.getNewAssignee();
        AggUserNotified newAssigneeNotified = userNotified(taskId, newAssignee);
        return Pair.withNullable(newAssigneeNotified, previousAssigneeNotified);
    }

    private static @Nullable AggUserNotified userNotifiedOrNull(AggTaskId taskId, UserId userId) {
        if (userId.equals(EMPTY_USER_ID)) {
            return null;
        }
        AggUserNotified event = userNotified(taskId, userId);
        return event;
    }

    private static AggUserNotified userNotified(AggTaskId taskId, UserId userId) {
        AggUserNotified event = AggUserNotified.newBuilder()
                                               .setTaskId(taskId)
                                               .setUserId(userId)
                                               .build();
        return event;
    }

    @React
    Pair<AggUserNotified, Optional<AggUserNotified>>
    on(Rejections.AggCannotReassignUnassignedTask rejection) {
        AggUserNotified event = userNotified(rejection.getTaskId(),
                                             rejection.getUserId());
        return Pair.withNullable(event, null);
    }

    @Apply
    private void event(AggUserNotified event) {
        // Do nothing.
    }
}
