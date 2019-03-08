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

package io.spine.server.model.contexts.tasks;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.model.contexts.tasks.Task;
import io.spine.test.model.contexts.tasks.TaskId;
import io.spine.test.model.contexts.tasks.TaskVBuilder;
import io.spine.test.model.contexts.tasks.commands.CreateTask;
import io.spine.test.model.contexts.tasks.commands.RenameTask;
import io.spine.test.model.contexts.tasks.commands.TaskCreated;
import io.spine.test.model.contexts.tasks.commands.TaskRenamed;

final class TaskAggregate extends Aggregate<TaskId, Task, TaskVBuilder> {

    TaskAggregate(TaskId id) {
        super(id);
    }

    @Assign
    TaskCreated handle(CreateTask cmd) {
        return TaskCreated
                .newBuilder()
                .setId(cmd.getId())
                .setTask(Task.newBuilder()
                             .setId(cmd.getId())
                             .setName(cmd.getName())
                             .setDescription(cmd.getDescription()))
                .build();
    }

    @Apply
    private void event(TaskCreated event) {
        Task task = event.getTask();
        builder()
                .setId(event.getId())
                .setName(task.getName())
                .setDescription(task.getDescription());
    }

    @Assign
    TaskRenamed handle(RenameTask cmd) {
        return TaskRenamed
                .newBuilder()
                .setId(cmd.getId())
                .setNewName(cmd.getNewName())
                .build();
    }

    @Apply
    private void event(TaskRenamed event) {
        builder().setName(event.getNewName());
    }
}
