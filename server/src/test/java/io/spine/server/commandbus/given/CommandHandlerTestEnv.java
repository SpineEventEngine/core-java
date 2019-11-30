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

package io.spine.server.commandbus.given;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.base.EventMessage;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.logging.Logging;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHistory;
import io.spine.server.event.EventDispatcher;
import io.spine.server.tuple.Pair;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.test.commandbus.ProjectId;
import io.spine.test.commandbus.TaskId;
import io.spine.test.commandbus.command.CmdBusAddTask;
import io.spine.test.commandbus.command.CmdBusCreateProject;
import io.spine.test.commandbus.command.CmdBusCreateTask;
import io.spine.test.commandbus.command.CmdBusStartProject;
import io.spine.test.commandbus.event.CmdBusProjectCreated;
import io.spine.test.commandbus.event.CmdBusProjectStarted;
import io.spine.test.commandbus.event.CmdBusTaskAdded;
import io.spine.test.commandbus.event.CmdBusTaskAssigned;
import io.spine.test.commandbus.event.CmdBusTaskStarted;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newLinkedList;

public class CommandHandlerTestEnv {

    /** Prevents instantiation of this utility class. */
    private CommandHandlerTestEnv() {
    }

    public static final class EventCatcher implements EventDispatcher {

        private final List<EventEnvelope> dispatched = newLinkedList();

        @Override
        public ImmutableSet<EventClass> messageClasses() {
            return EventClass.setOf(
                    CmdBusProjectStarted.class,
                    CmdBusTaskAssigned.class,
                    CmdBusTaskStarted.class
            );
        }

        @Override
        public ImmutableSet<EventClass> domesticEventClasses() {
            return eventClasses();
        }

        @Override
        public ImmutableSet<EventClass> externalEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public void dispatch(EventEnvelope event) {
            dispatched.add(event);
        }

        public List<EventEnvelope> dispatched() {
            return ImmutableList.copyOf(dispatched);
        }
    }

    public static class TestCommandHandler extends AbstractCommandHandler implements Logging {

        private final ImmutableList<EventMessage> eventsOnStartProjectCmd =
                createEventsOnStartProjectCmd();

        private final CommandHistory commandsHandled = new CommandHistory();

        public void assertHandled(Command expected) {
            commandsHandled.assertHandled(expected);
        }

        @SuppressWarnings("CheckReturnValue")
        // Can ignore the returned ID of the command handler in these tests.
        public void handle(Command cmd) {
            CommandEnvelope commandEnvelope = CommandEnvelope.of(cmd);
            dispatch(commandEnvelope);
        }

        public ImmutableList<EventMessage> getEventsOnStartProjectCmd() {
            return eventsOnStartProjectCmd;
        }

        @Assign
        CmdBusProjectCreated handle(CmdBusCreateProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return CmdBusProjectCreated.getDefaultInstance();
        }

        @Assign
        CmdBusTaskAdded handle(CmdBusAddTask msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return CmdBusTaskAdded.getDefaultInstance();
        }

        @Assign
        List<EventMessage> handle(CmdBusStartProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return eventsOnStartProjectCmd;
        }

        @Assign
        Pair<CmdBusTaskAssigned, Optional<CmdBusTaskStarted>>
        handle(CmdBusCreateTask msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return createEventsOnCreateTaskCmd(msg);
        }

        private ImmutableList<EventMessage> createEventsOnStartProjectCmd() {
            ProjectId id = ProjectId
                    .newBuilder()
                    .setId(id())
                    .build();
            CmdBusProjectStarted startedEvent = CmdBusProjectStarted
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            CmdBusProjectStarted defaultEvent = CmdBusProjectStarted.getDefaultInstance();
            return ImmutableList.of(startedEvent, defaultEvent);
        }

        private static Pair<CmdBusTaskAssigned, Optional<CmdBusTaskStarted>>
        createEventsOnCreateTaskCmd(CmdBusCreateTask msg) {
            TaskId taskId = msg.getTaskId();
            CmdBusTaskAssigned cmdTaskAssigned = CmdBusTaskAssigned
                    .newBuilder()
                    .setTaskId(taskId)
                    .build();
            CmdBusTaskStarted cmdTaskStarted = msg.getStart()
                                               ? CmdBusTaskStarted
                                                       .newBuilder()
                                                       .setTaskId(taskId)
                                                       .build()
                                               : null;
            return Pair.withNullable(cmdTaskAssigned, cmdTaskStarted);
        }
    }
}
