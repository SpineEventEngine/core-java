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

package io.spine.testing.server.blackbox.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.testing.server.blackbox.BbProject;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.BbProjectVBuilder;
import io.spine.testing.server.blackbox.BbTask;
import io.spine.testing.server.blackbox.command.BbAddTask;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.command.BbStartProject;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbProjectStarted;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.rejection.BbProjectAlreadyStarted;
import io.spine.testing.server.blackbox.rejection.BbTaskCreatedInCompletedProject;

import static io.spine.testing.server.blackbox.BbProject.Status.COMPLETED;
import static io.spine.testing.server.blackbox.BbProject.Status.CREATED;
import static io.spine.testing.server.blackbox.BbProject.Status.STARTED;

public class BbProjectAggregate extends Aggregate<BbProjectId, BbProject, BbProjectVBuilder> {

    protected BbProjectAggregate(BbProjectId id) {
        super(id);
    }

    @Assign
    BbProjectCreated handle(BbCreateProject command) {
        return BbProjectCreated
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    BbProjectStarted handle(BbStartProject command)
            throws BbProjectAlreadyStarted {
        if (getState().getStatus() != CREATED) {
            throw BbProjectAlreadyStarted
                    .newBuilder()
                    .setProjectId(command.getProjectId())
                    .build();
        }
        return BbProjectStarted
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    BbTaskAdded handle(BbAddTask command) throws BbTaskCreatedInCompletedProject {
        BbProjectId projectId = command.getProjectId();
        BbTask task = command.getTask();
        if (getState().getStatus() == COMPLETED) {
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

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(BbProjectCreated event) {
        getBuilder().setId(event.getProjectId());
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(BbProjectStarted event) {
        getBuilder().setStatus(STARTED);
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(BbTaskAdded event) {
        getBuilder().addTask(event.getTask());
    }
}
