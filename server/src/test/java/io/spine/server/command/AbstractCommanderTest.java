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

package io.spine.server.command;

import com.google.protobuf.Message;
import io.spine.client.CommandFactory;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.DelegatingEventDispatcher;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventFactory;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdSetTaskDescription;
import io.spine.test.command.FirstCmdCreateProject;
import io.spine.test.command.ProjectId;
import io.spine.test.command.Task;
import io.spine.test.command.TaskId;
import io.spine.test.command.event.CmdTaskAdded;
import io.spine.testing.TestValues;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AbstractCommander should")
class AbstractCommanderTest {

    private final CommandFactory commandFactory =
            TestActorRequestFactory.newInstance(getClass())
                                   .command();
    private final EventFactory eventFactory =
            TestEventFactory.newInstance(getClass());

    private final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                                .build();
    private CommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        CommandBus commandBus = boundedContext.getCommandBus();
        EventBus eventBus = boundedContext.getEventBus();
        AbstractCommander commander = new Commendatore(commandBus, boundedContext.getEventBus());
        interceptor = new CommandInterceptor(boundedContext,
                                             FirstCmdCreateProject.class,
                                             CmdSetTaskDescription.class);
        commandBus.register(commander);
        eventBus.register(DelegatingEventDispatcher.of(commander));
    }

    @Test
    @DisplayName("create a command in response to a command")
    void commandOnCommand() {
        CmdCreateProject commandMessage = CmdCreateProject
                .newBuilder()
                .setProjectId(newProjectId())
                .build();
        createCommandAndPost(commandMessage);

        assertTrue(interceptor.contains(FirstCmdCreateProject.class));
    }

    @Test
    @DisplayName("create a command on an event")
    void commandOnEvent() {
        CmdTaskAdded eventMessage = CmdTaskAdded
                .newBuilder()
                .setProjectId(newProjectId())
                .setTask(Task.newBuilder().setTaskId(newTaskId()))
                .build();

        createEventAndPost(eventMessage);

        assertTrue(interceptor.contains(CmdSetTaskDescription.class));
    }

    /*
     * Test Environment
     *******************************/

    private static ProjectId newProjectId() {
        return ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
    }

    private static TaskId newTaskId() {
        return TaskId
                .newBuilder()
                .setId(TestValues.random(100))
                .build();

    }

    private void createCommandAndPost(Message commandMessage) {
        io.spine.core.Command command = commandFactory.create(commandMessage);
        boundedContext.getCommandBus()
                      .post(command, StreamObservers.noOpObserver());
    }

    private void createEventAndPost(Message eventMessage) {
        io.spine.core.Event event = eventFactory.createEvent(eventMessage, null);
        boundedContext.getEventBus()
                      .post(event);
    }

    /**
     * Test environment class that generates new commands in response to incoming messages.
     */
    private static final class Commendatore extends AbstractCommander {

        private Commendatore(CommandBus commandBus, EventBus eventBus) {
            super(commandBus, eventBus);
        }

        @Command
        FirstCmdCreateProject on(CmdCreateProject command) {
            return FirstCmdCreateProject
                    .newBuilder()
                    .setId(command.getProjectId())
                    .build();
        }

        @Command
        CmdSetTaskDescription on(CmdTaskAdded event) {
            return CmdSetTaskDescription
                    .newBuilder()
                    .setTaskId(event.getTask()
                                    .getTaskId())
                    .setDescription("Testing command creation on event")
                    .build();
        }
    }
}
