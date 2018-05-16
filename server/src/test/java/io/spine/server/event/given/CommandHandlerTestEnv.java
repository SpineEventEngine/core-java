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

package io.spine.server.event.given;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.command.Assign;
import io.spine.server.command.CommandHandler;
import io.spine.server.command.CommandHistory;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.test.command.CmdAddTask;
import io.spine.test.command.CmdCreateProject;
import io.spine.test.command.CmdStartProject;
import io.spine.test.command.event.CmdProjectCreated;
import io.spine.test.command.event.CmdProjectStarted;
import io.spine.test.command.event.CmdTaskAdded;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Alexander Yevsyukov
 */
public class CommandHandlerTestEnv {

    /** Prevents instantiation on this utility class. */
    private CommandHandlerTestEnv() {
    }

    public static class EventCatcher implements EventDispatcher<String> {

        private final List<EventEnvelope> dispatched = new LinkedList<>();

        @Override
        public Set<EventClass> getMessageClasses() {
            return ImmutableSet.of(EventClass.of(CmdProjectStarted.class),
                                   EventClass.of(StringValue.class));
        }

        @Override
        public Set<String> dispatch(EventEnvelope envelope) {
            dispatched.add(envelope);
            return Identity.of(this);
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField") // OK for tests.
        public List<EventEnvelope> getDispatched() {
            return dispatched;
        }
    }

    @SuppressWarnings({"OverloadedMethodsWithSameNumberOfParameters",
                       "ReturnOfCollectionOrArrayField"})
    public static class TestCommandHandler extends CommandHandler {

        private final ImmutableList<Message> eventsOnStartProjectCmd =
                createEventsOnStartProjectCmd();

        private final CommandHistory commandsHandled = new CommandHistory();

        @Nullable
        private CommandEnvelope lastErrorEnvelope;
        @Nullable
        private RuntimeException lastException;

        public TestCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        public void assertHandled(Command expected) {
            commandsHandled.assertHandled(expected);
        }

        public void handle(Command cmd) {
            final CommandEnvelope commandEnvelope = CommandEnvelope.of(cmd);
            dispatch(commandEnvelope);
        }

        public ImmutableList<Message> getEventsOnStartProjectCmd() {
            return eventsOnStartProjectCmd;
        }

        @Override
        @VisibleForTesting
        public Logger log() {
            return super.log();
        }

        @Assign
        CmdProjectCreated handle(CmdCreateProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return CmdProjectCreated.getDefaultInstance();
        }

        @Assign
        CmdTaskAdded handle(CmdAddTask msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return CmdTaskAdded.getDefaultInstance();
        }

        @Assign
        List<Message> handle(CmdStartProject msg, CommandContext context) {
            commandsHandled.add(msg, context);
            return eventsOnStartProjectCmd;
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            super.onError(envelope, exception);
            lastErrorEnvelope = envelope;
            lastException = exception;
        }

        @Nullable
        public CommandEnvelope getLastErrorEnvelope() {
            return lastErrorEnvelope;
        }

        @Nullable
        public RuntimeException getLastException() {
            return lastException;
        }

        private static ImmutableList<Message> createEventsOnStartProjectCmd() {
            final ImmutableList.Builder<Message> builder = ImmutableList.builder();
            builder.add(CmdProjectStarted.getDefaultInstance(), StringValue.getDefaultInstance());
            return builder.build();
        }
    }
}
