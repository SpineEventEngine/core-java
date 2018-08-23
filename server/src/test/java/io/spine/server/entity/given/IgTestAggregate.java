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

package io.spine.server.entity.given;

import com.google.common.collect.Lists;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.ProjectVBuilder;
import io.spine.test.entity.Task;
import io.spine.test.entity.TaskId;
import io.spine.test.entity.command.EntAddTask;
import io.spine.test.entity.command.EntCreateProject;
import io.spine.test.entity.command.EntStartProject;
import io.spine.test.entity.event.EntProjectCreated;
import io.spine.test.entity.event.EntProjectStarted;
import io.spine.test.entity.event.EntTaskAdded;
import io.spine.test.entity.event.EntTaskChanged;
import io.spine.test.entity.event.EntTaskRenamed;

import java.util.List;

import static io.spine.test.entity.Project.Status.CREATED;
import static io.spine.test.entity.Project.Status.STARTED;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * An aggregate class for {@linkplain io.spine.server.entity.IdempotencyGuardTest
 * IdempotencyGuard tests}.
 *
 * @author Mykhailo Drachuk
 */
public class IgTestAggregate
        extends Aggregate<ProjectId, Project, ProjectVBuilder> {

    public IgTestAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    EntProjectCreated handle(EntCreateProject cmd) {
        EntProjectCreated event = EntProjectCreated
                .newBuilder()
                .setProjectId(cmd.getProjectId())
                .build();
        return event;
    }

    @Assign
    EntProjectStarted handle(EntStartProject cmd) {
        EntProjectStarted message = EntProjectStarted
                .newBuilder()
                .setProjectId(cmd.getProjectId())
                .build();
        return message;
    }

    @Assign
    EntTaskAdded handle(EntAddTask cmd) {
        EntTaskAdded event = EntTaskAdded
                .newBuilder()
                .setProjectId(cmd.getProjectId())
                .setTask(cmd.getTask())
                .build();
        return event;
    }

    @React
    EntTaskChanged reactOn(EntTaskRenamed event) {
        TaskId targetTaskId = event.getTaskId();
        Task changedTask = getState().getTaskList()
                                     .stream()
                                     .filter(task -> task.getTaskId()
                                                         .equals(targetTaskId))
                                     .findAny()
                                     .orElseThrow(() -> unknownTask(targetTaskId));
        Task renamed = changedTask.toBuilder()
                                  .setTitle(event.getNewName())
                                  .build();
        return EntTaskChanged
                .newBuilder()
                .setId(targetTaskId)
                .setNewTask(renamed)
                .build();
    }

    @Apply
    private void event(EntProjectCreated event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(CREATED);
    }

    @Apply
    private void event(EntProjectStarted event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(STARTED);
    }

    @Apply
    private void event(EntTaskAdded event) {
        getBuilder()
                .addTask(event.getTask());
    }

    @Apply
    void event(EntTaskChanged event) {
        TaskId taskId = event.getId();
        List<Task> originalTasks = getBuilder().getTask();
        List<Task> tasks = Lists.newArrayListWithExpectedSize(originalTasks.size());
        tasks.addAll(originalTasks);
        boolean removed = tasks.removeIf(task -> task.getTaskId()
                                                     .equals(taskId));
        if (!removed) {
            throw unknownTask(taskId);
        }
        tasks.add(event.getNewTask());
        getBuilder().clearTask()
                    .addAllTask(tasks);
    }

    private static IllegalStateException unknownTask(TaskId taskId) {
        return newIllegalStateException("Task with ID %s is not present in the project.", taskId);
    }
}
