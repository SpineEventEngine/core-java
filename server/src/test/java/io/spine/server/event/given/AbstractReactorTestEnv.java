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

package io.spine.server.event.given;

import com.google.common.collect.ImmutableList;
import io.spine.core.UserId;
import io.spine.server.event.AbstractEventReactor;
import io.spine.server.event.EventBus;
import io.spine.server.event.React;
import io.spine.test.event.ProjectId;
import io.spine.test.event.Task;
import io.spine.test.event.TaskAdded;
import io.spine.test.event.TaskAssigned;
import io.spine.test.event.TaskBecameUnavailable;
import io.spine.test.event.TaskId;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.spine.base.Identifier.newUuid;
import static java.lang.String.format;

public class AbstractReactorTestEnv {

    /* Prevent instantiation. */
    private AbstractReactorTestEnv() {
    }

    public static DefaultUserAssigner defaultUserAssigner(EventBus eventBus, UserId defaultUser) {
        return new DefaultUserAssigner(eventBus, defaultUser);
    }

    public static TaskDisruptor taskDisruptor(EventBus eventBus) {
        return new TaskDisruptor(eventBus);
    }

    /**
     * Disrupts task by making them unavailable as soon as they are assigned, emitting a
     * respective event.
     *
     * <p>Does so for all tasks, even those beyond its bounded context. Very evil.
     */
    public static class TaskDisruptor extends AbstractEventReactor {

        protected TaskDisruptor(EventBus eventBus) {
            super(eventBus);
        }

        @React(external = true)
        TaskBecameUnavailable makeUnavailable(TaskAssigned assigned) {
            TaskId id = assigned.getTask()
                                .getTaskId();
            TaskBecameUnavailable result = TaskBecameUnavailable
                    .newBuilder()
                    .setId(id)
                    .build();
            return result;
        }
    }

    /**
     * When a task is added, this class automatically assigns it to the default user,
     * emitting a respective event.
     */
    public static class DefaultUserAssigner extends AbstractEventReactor {

        private final UserId defaultUser;
        private final List<Task> assignedByThisAssigner = new ArrayList<>();

        private DefaultUserAssigner(EventBus eventBus, UserId defaultUser) {
            super(eventBus);
            this.defaultUser = defaultUser;
        }

        @React
        TaskAssigned taskAssigned(TaskAdded added) {
            Task task = added.getTask();
            assignedByThisAssigner.add(task);
            TaskAssigned result = TaskAssigned
                    .newBuilder()
                    .setTask(task)
                    .setAssignee(defaultUser)
                    .build();
            return result;
        }

        /** Obtains a list of all tasks that we assigned by this assigner. */
        public ImmutableList<Task> assignedByThisAssigner() {
            return ImmutableList.copyOf(assignedByThisAssigner);
        }
    }

    /** Returns a random unique user identifier. */
    public static UserId someUserId() {
        UserId result = UserId
                .newBuilder()
                .setValue(newUuid())
                .build();
        return result;
    }

    public static ProjectId someProjectId() {
        ProjectId result = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        return result;
    }

    public static Task someTask() {
        @SuppressWarnings("UnsecureRandomNumberGeneration")/* does not matter for testing purposes.*/
        int randomInt = new Random().nextInt();
        String description = format("Some irrelevant description of task %s", randomInt);
        TaskId taskId = TaskId
                .newBuilder()
                .setId(randomInt)
                .build();
        Task result = Task
                .newBuilder()
                .setTaskId(taskId)
                .setDescription(description)
                .setDone(false)
                .build();
        return result;
    }

    public static TaskAdded addSomeTask() {
        Task taskToAdd = someTask();
        TaskAdded result = TaskAdded
                .newBuilder()
                .setProjectId(someProjectId())
                .setTask(taskToAdd)
                .build();
        return result;
    }
}
