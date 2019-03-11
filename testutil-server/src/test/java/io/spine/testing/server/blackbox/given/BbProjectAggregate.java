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

package io.spine.testing.server.blackbox.given;

import io.spine.core.UserId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.testing.server.blackbox.BbProject;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BbProjectVBuilder;
import io.spine.testing.server.blackbox.BbTask;
import io.spine.testing.server.blackbox.command.BbAddTask;
import io.spine.testing.server.blackbox.command.BbAssignProject;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.command.BbStartProject;
import io.spine.testing.server.blackbox.event.BbAssigneeAdded;
import io.spine.testing.server.blackbox.event.BbAssigneeAddedVBuilder;
import io.spine.testing.server.blackbox.event.BbAssigneeRemoved;
import io.spine.testing.server.blackbox.event.BbAssigneeRemovedVBuilder;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbProjectStarted;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.event.BbUserDeleted;
import io.spine.testing.server.blackbox.rejection.BbProjectAlreadyStarted;
import io.spine.testing.server.blackbox.rejection.BbTaskCreatedInCompletedProject;

import java.util.List;
import java.util.Optional;

import static io.spine.testing.server.blackbox.BbProject.Status.COMPLETED;
import static io.spine.testing.server.blackbox.BbProject.Status.CREATED;
import static io.spine.testing.server.blackbox.BbProject.Status.STARTED;
import static java.util.Optional.empty;

class BbProjectAggregate extends Aggregate<BbProjectId, BbProject, BbProjectVBuilder> {

    @Assign
    BbProjectCreated handle(BbCreateProject command) {
        return BbProjectCreated
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    BbProjectStarted handle(BbStartProject command) throws BbProjectAlreadyStarted {
        BbProjectId projectId = command.getProjectId();
        if (state().getStatus() != CREATED) {
            throw BbProjectAlreadyStarted
                    .newBuilder()
                    .setProjectId(projectId)
                    .build();
        }
        return BbProjectStarted
                .newBuilder()
                .setProjectId(projectId)
                .build();
    }

    @Assign
    BbTaskAdded handle(BbAddTask command) throws BbTaskCreatedInCompletedProject {
        BbProjectId projectId = command.getProjectId();
        BbTask task = command.getTask();
        if (state().getStatus() == COMPLETED) {
            throw BbTaskCreatedInCompletedProject
                    .newBuilder()
                    .setProjectId(projectId)
                    .setTask(task)
                    .build();
        }
        return BbTaskAdded
                .newBuilder()
                .setProjectId(projectId)
                .setTask(task)
                .build();
    }

    @Assign
    BbAssigneeAdded handle(BbAssignProject command) {
        return BbAssigneeAddedVBuilder
                .newBuilder()
                .setId(id())
                .setUserId(command.getUserId())
                .build();
    }

    @React(external = true)
    Optional<BbAssigneeRemoved> on(BbUserDeleted event) {
        List<UserId> assignees = state().getAssigneeList();
        UserId user = event.getId();
        if (!assignees.contains(user)) {
            return empty();
        }
        return Optional.of(userUnassigned(user));
    }

    private BbAssigneeRemoved userUnassigned(UserId user) {
        return BbAssigneeRemovedVBuilder
                .newBuilder()
                .setId(id())
                .setUserId(user)
                .build();
    }

    @Apply
    private void on(BbProjectCreated event) {
        builder().setId(event.getProjectId())
                 .setStatus(CREATED);
    }

    @Apply
    private void on(BbProjectStarted event) {
        builder().setStatus(STARTED);
    }

    @Apply
    private void on(BbTaskAdded event) {
        builder().addTask(event.getTask());
    }

    @Apply
    private void on(BbAssigneeAdded event) {
        builder().addAssignee(event.getUserId());
    }

    @Apply
    private void on(BbAssigneeRemoved event) {
        BbProjectVBuilder builder = builder();
        List<UserId> assignees = builder.getAssignee();
        int index = assignees.indexOf(event.getUserId());
        builder.removeAssignee(index);
    }
}
