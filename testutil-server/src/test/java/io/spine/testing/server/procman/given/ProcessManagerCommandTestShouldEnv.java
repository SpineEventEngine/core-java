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

package io.spine.testing.server.procman.given;

import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.server.command.Assign;
import io.spine.server.command.Command;
import io.spine.server.entity.Repository;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.testing.server.TUAssignTask;
import io.spine.testing.server.TUCreateTask;
import io.spine.testing.server.TUProjectId;
import io.spine.testing.server.TUTaskAssigned;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.TUTaskCreationPmVBuilder;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.procman.ProcessManagerCommandTest;
import org.junit.jupiter.api.BeforeEach;

import static com.google.protobuf.util.Timestamps.fromNanos;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProcessManagerCommandTestShouldEnv {

    private static final TUProjectId ID = TUProjectId.newBuilder()
                                                     .setValue("test pm id")
                                                     .build();

    /**
     * Prevents direct instantiation.
     */
    private ProcessManagerCommandTestShouldEnv() {
    }

    public static CommandHandlingProcessManager processManager() {
        return Given.processManagerOfClass(CommandHandlingProcessManager.class)
                    .withId(ID)
                    .build();
    }

    /**
     * A dummy process manager that handles a {@code TUCreateTask} command and routes a nested
     * command.
     */
    public static class CommandHandlingProcessManager
            extends ProcessManager<TUProjectId,
                                   TUTaskCreationPm,
                                   TUTaskCreationPmVBuilder> {

        public static final TUAssignTask NESTED_COMMAND =
                TUAssignTask.newBuilder()
                            .setId(ID)
                            .build();

        protected CommandHandlingProcessManager(TUProjectId id) {
            super(id);
        }

        @Command
        @SuppressWarnings("CheckReturnValue")
        TUAssignTask handle(TUCreateTask command, CommandContext context) {
            return TUAssignTask.newBuilder()
                               .setId(command.getId())
                               .build();
        }

        @Assign
        TUTaskAssigned handle(TUAssignTask command) {
            getBuilder().setTimestamp(fromNanos(123456));
            return TUTaskAssigned.newBuilder()
                                 .setId(command.getId())
                                 .build();
        }
    }

    private static class CommandHandlingProcessManagerRepository
            extends ProcessManagerRepository<TUProjectId,
                                             CommandHandlingProcessManager,
            TUTaskCreationPm> {
    }

    /**
     * The test class for the {@code TUCreateTask} command handler in
     * {@code CommandHandlingProcessManager}.
     */
    public static class TaskCreationProcessManagerTest
            extends ProcessManagerCommandTest<TUProjectId,
                                              TUCreateTask,
                                              TUTaskCreationPm,
                                              CommandHandlingProcessManager> {

        public static final TUCreateTask TEST_COMMAND =
                TUCreateTask.newBuilder()
                            .setId(ID)
                            .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected TUProjectId newId() {
            return ID;
        }

        @Override
        protected TUCreateTask createMessage() {
            return TEST_COMMAND;
        }

        @Override
        protected Repository<TUProjectId, CommandHandlingProcessManager>
        createEntityRepository() {
            return new CommandHandlingProcessManagerRepository();
        }

        public Message storedMessage() {
            return message();
        }

        /**
         * Exposes internal configuration method.
         */
        public void init() {
            configureBoundedContext();
        }
    }
}
