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

import com.google.protobuf.Message;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.TenantId;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.Task;
import io.spine.test.entity.TaskId;
import io.spine.test.entity.command.EntAddTask;
import io.spine.test.entity.command.EntCreateProject;
import io.spine.test.entity.command.EntStartProject;
import io.spine.test.entity.event.EntTaskRenamed;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;

import java.util.Random;

import static io.spine.base.Identifier.newUuid;

/**
 * @author Mykhailo Drachuk
 */
public class IdempotencyGuardTestEnv {

    /** Prevents instantiation of this test environment. */
    private IdempotencyGuardTestEnv() {
    }

    public static ProjectId newProjectId() {
        return ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
    }

    public static TaskId newTaskId() {
        @SuppressWarnings("UnsecureRandomNumberGeneration") // OK for tests.
        int value = new Random().nextInt();
        return TaskId
                .newBuilder()
                .setId(value)
                .build();
    }

    public static TenantId newTenantId() {
        return TenantId
                .newBuilder()
                .setValue(newUuid())
                .build();
    }

    public static EntCreateProject createProject(ProjectId projectId) {
        return EntCreateProject
                .newBuilder()
                .setProjectId(projectId)
                .build();
    }

    public static EntStartProject startProject(ProjectId projectId) {
        return EntStartProject
                .newBuilder()
                .setProjectId(projectId)
                .build();
    }

    public static EntAddTask addTask(ProjectId projectId, TaskId taskId) {
        Task task = Task
                .newBuilder()
                .setTaskId(taskId)
                .build();
        return EntAddTask
                .newBuilder()
                .setProjectId(projectId)
                .setTask(task)
                .build();
    }

    public static EntTaskRenamed taskRenamed(TaskId id, String newName, ProjectId projectId) {
        return EntTaskRenamed
                .newBuilder()
                .setTaskId(id)
                .setNewName(newName)
                .setProjectId(projectId)
                .build();
    }

    public static Command command(Message commandMessage, TenantId tenantId) {
        return newRequestFactory(tenantId).command()
                                          .create(commandMessage);
    }

    private static TestActorRequestFactory newRequestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(IdempotencyGuardTestEnv.class, tenantId);
    }

    public static Event event(Message eventMessage, TenantId tenantId) {
        TestEventFactory factory = newEventFactory();
        Event event = factory.createEvent(eventMessage);
        Event result = setTenant(event, tenantId);
        return result;
    }

    private static TestEventFactory newEventFactory() {
        return TestEventFactory.newInstance(IdempotencyGuardTestEnv.class);
    }

    private static Event setTenant(Event origin, TenantId tenantId) {
        EventContext originContext = origin.getContext();
        CommandContext originCommandContext = originContext.getCommandContext();
        ActorContext actorContext = originCommandContext
                .getActorContext()
                .toBuilder()
                .setTenantId(tenantId)
                .build();
        CommandContext commandContext = originCommandContext
                .toBuilder()
                .setActorContext(actorContext)
                .build();
        EventContext eventContext = originContext
                .toBuilder()
                .setCommandContext(commandContext)
                .build();
        Event result = origin.toBuilder()
                             .setContext(eventContext)
                             .build();
        return result;
    }
}
