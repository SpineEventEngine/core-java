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

import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.test.commandbus.command.CmdBusAddTask;
import io.spine.test.commandbus.command.CmdBusRemoveTask;
import io.spine.test.commandbus.command.FirstCmdBusCreateProject;
import io.spine.test.commandbus.command.SecondCmdBusStartProject;
import io.spine.test.commandbus.event.CmdBusProjectCreated;
import io.spine.test.commandbus.event.CmdBusProjectStarted;
import io.spine.test.commandbus.event.CmdBusTaskAdded;
import io.spine.test.reflect.InvalidProjectName;
import io.spine.test.reflect.ProjectId;

import java.util.ArrayList;
import java.util.List;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.Collections.unmodifiableList;

public class SingleTenantCommandBusTestEnv {

    /** Prevents instantiation of this utility class. */
    private SingleTenantCommandBusTestEnv() {
    }

    /**
     * A {@code CommandHandler}, which throws a rejection upon a command.
     */
    public static class FaultyHandler extends AbstractCommandHandler {

        private final InvalidProjectName rejection = InvalidProjectName
                .newBuilder()
                .setProjectId(ProjectId.getDefaultInstance())
                .build();

        public FaultyHandler(EventBus eventBus) {
            super(eventBus);
        }

        @SuppressWarnings("unused")     // does nothing, but throws a rejection.
        @Assign
        CmdBusTaskAdded handle(CmdBusAddTask msg, CommandContext context) throws
                                                                          InvalidProjectName {
            throw rejection;
        }

        @SuppressWarnings("unused")     // does nothing, but throws a rejection.
        @Assign
        CmdBusTaskAdded handle(CmdBusRemoveTask msg, CommandContext context) {
            throw newIllegalStateException("Command handling failed with unexpected exception");
        }

        public InvalidProjectName getThrowable() {
            return rejection;
        }
    }

    /**
     * A command handler that posts a nested command.
     */
    public static class CommandPostingHandler extends AbstractCommandHandler {

        private final CommandBus commandBus;
        private final List<Message> handledCommands = new ArrayList<>();
        private final Command commandToPost;

        public CommandPostingHandler(EventBus eventBus, CommandBus commandBus,
                                     Command commandToPost) {
            super(eventBus);
            this.commandBus = commandBus;
            this.commandToPost = commandToPost;
        }

        @Assign
        CmdBusProjectCreated handle(FirstCmdBusCreateProject command) {
            commandBus.post(commandToPost, noOpObserver());
            handledCommands.add(command);
            return CmdBusProjectCreated
                    .vBuilder()
                    .setProjectId(command.getId())
                    .build();
        }

        @Assign
        CmdBusProjectStarted handle(SecondCmdBusStartProject command) {
            handledCommands.add(command);
            return CmdBusProjectStarted
                    .vBuilder()
                    .setProjectId(command.getId())
                    .build();
        }

        public List<Message> handledCommands() {
            return unmodifiableList(handledCommands);
        }
    }
}
