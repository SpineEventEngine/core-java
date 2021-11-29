/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.command;

import com.google.protobuf.Empty;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.client.CommandFactory;
import io.spine.core.UserId;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.EventFactory;
import io.spine.server.tuple.Pair;
import io.spine.test.command.CmdAssignTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdCreateTask;
import io.spine.test.command.CmdSetTaskDescription;
import io.spine.test.command.CmdStartTask;
import io.spine.test.command.FirstCmdCreateProject;
import io.spine.test.command.ProjectId;
import io.spine.test.command.Task;
import io.spine.test.command.TaskId;
import io.spine.test.command.event.CmdTaskAdded;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.TestValues.random;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("OverlyCoupledClass")
@DisplayName("`AbstractCommander` should")
class AbstractCommanderTest {

    private final CommandFactory commandFactory = new TestActorRequestFactory(getClass()).command();
    private final EventFactory eventFactory = TestEventFactory.newInstance(getClass());

    private BoundedContext context;
    private CommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CommandInterceptor(FirstCmdCreateProject.class,
                                             CmdSetTaskDescription.class,
                                             CmdAssignTask.class,
                                             CmdStartTask.class);
        AbstractCommander commander = new Commendatore();
        context = BoundedContextBuilder
                .assumingTests()
                .addCommandDispatcher(commander)
                .addCommandDispatcher(interceptor)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    @DisplayName("create a command in response to a command")
    void commandOnCommand() {
        var commandMessage = CmdCreateProject.newBuilder()
                .setProjectId(newProjectId())
                .build();
        createCommandAndPost(commandMessage);

        assertTrue(interceptor.contains(FirstCmdCreateProject.class));
    }

    @Test
    @DisplayName("create a command on an event")
    void commandOnEvent() {
        var eventMessage = CmdTaskAdded.newBuilder()
                .setProjectId(newProjectId())
                .setTask(Task.newBuilder()
                             .setTaskId(newTaskId())
                             .build())
                .build();

        createEventAndPost(eventMessage);

        assertTrue(interceptor.contains(CmdSetTaskDescription.class));
    }

    @Test
    @DisplayName("create a pair of commands in response to a command")
    void createCommandPair() {
        postCreateTaskCommand(true);

        assertTrue(interceptor.contains(CmdAssignTask.class));
        assertTrue(interceptor.contains(CmdStartTask.class));
    }

    @Test
    @DisplayName("create a pair of commands with null second command in response to a command")
    void createCommandPairWithNull() {
        postCreateTaskCommand(false);

        assertTrue(interceptor.contains(CmdAssignTask.class));
        assertFalse(interceptor.contains(CmdStartTask.class));
        assertFalse(interceptor.contains(Empty.class));
    }

    /*
     * Test Environment
     *******************************/

    private static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                .setId(newUuid())
                .build();
    }

    private static TaskId newTaskId() {
        return TaskId.newBuilder()
                .setId(random(1, 100))
                .build();
    }

    private static UserId newUserId() {
        return UserId.newBuilder()
                .setValue(newUuid())
                .build();

    }

    private void createCommandAndPost(CommandMessage commandMessage) {
        var command = commandFactory.create(commandMessage);
        context.commandBus()
               .post(command, StreamObservers.noOpObserver());
    }

    private void createEventAndPost(EventMessage eventMessage) {
        var event = eventFactory.createEvent(eventMessage, null);
        context.eventBus()
               .post(event);
    }

    private void postCreateTaskCommand(boolean startTask) {
        var taskId = newTaskId();
        var userId = newUserId();
        var task = Task.newBuilder()
                .setTaskId(taskId)
                .setAssignee(userId)
                .build();
        var commandMessage = CmdCreateTask.newBuilder()
                .setTaskId(taskId)
                .setTask(task)
                .setStart(startTask)
                .build();
        createCommandAndPost(commandMessage);
    }

    /**
     * Test environment class that generates new commands in response to incoming messages.
     */
    private static final class Commendatore extends AbstractCommander {

        @Command
        FirstCmdCreateProject on(CmdCreateProject command) {
            return FirstCmdCreateProject.newBuilder()
                    .setId(command.getProjectId())
                    .build();
        }

        @Command
        CmdSetTaskDescription on(CmdTaskAdded event) {
            return CmdSetTaskDescription.newBuilder()
                    .setTaskId(event.getTask()
                                    .getTaskId())
                    .setDescription("Testing command creation on event")
                    .build();
        }

        @Command
        Pair<CmdAssignTask, Optional<CmdStartTask>> on(CmdCreateTask command) {
            var taskId = command.getTaskId();
            var assignee = command.getTask()
                                  .getAssignee();
            var cmdAssignTask = CmdAssignTask.newBuilder()
                    .setTaskId(taskId)
                    .setAssignee(assignee)
                    .build();
            var cmdStartTask = command.getStart()
                               ? CmdStartTask.newBuilder()
                                       .setTaskId(taskId)
                                       .build()
                               : null;
            return Pair.withNullable(cmdAssignTask, cmdStartTask);
        }
    }
}
