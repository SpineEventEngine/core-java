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

package io.spine.server.procman.given;

import com.google.protobuf.Message;
import com.google.protobuf.util.Timestamps;
import io.spine.core.CommandContext;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerCommandTest;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.test.testutil.TestUtilAssignTask;
import io.spine.test.testutil.TestUtilCreateTask;
import io.spine.test.testutil.TestUtilProjectId;
import io.spine.test.testutil.TestUtilTaskAssigned;
import io.spine.test.testutil.TestUtilTaskCreated;
import io.spine.test.testutil.TestUtilTaskCreationPm;
import io.spine.test.testutil.TestUtilTaskCreationPmVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProcessManagerCommandTestTestEnv {

    private static final TestUtilProjectId ID = TestUtilProjectId.newBuilder()
                                                                 .setValue("test pm id")
                                                                 .build();

    /**
     * Prevents direct instantiation.
     */
    private ProcessManagerCommandTestTestEnv() {
    }

    public static CommandHandlingProcessManager processManager() {
        return Given.processManagerOfClass(CommandHandlingProcessManager.class)
                    .withId(ID)
                    .build();
    }

    /**
     * A dummy process manager that handles a {@code TestUtilCreateTask} command and routes a nested
     * command.
     */
    public static class CommandHandlingProcessManager
            extends ProcessManager<TestUtilProjectId,
                                   TestUtilTaskCreationPm,
                                   TestUtilTaskCreationPmVBuilder> {

        public static final TestUtilAssignTask NESTED_COMMAND =
                TestUtilAssignTask.newBuilder()
                                  .setId(ID)
                                  .build();

        protected CommandHandlingProcessManager(TestUtilProjectId id) {
            super(id);
        }

        @Assign
        @SuppressWarnings("CheckReturnValue")
        TestUtilTaskCreated handle(TestUtilCreateTask command, CommandContext context) {
            newRouterFor(command, context).add(NESTED_COMMAND)
                                          .routeAll();
            return TestUtilTaskCreated.newBuilder()
                                      .setId(command.getId())
                                      .build();
        }

        @Assign
        TestUtilTaskAssigned handle(TestUtilAssignTask command) {
            getBuilder().setTimestamp(Timestamps.fromNanos(123456));
            return TestUtilTaskAssigned.newBuilder()
                                       .setId(command.getId())
                                       .build();
        }
    }

    private static class CommandHandlingProcessManagerRepository
            extends ProcessManagerRepository<TestUtilProjectId,
                                             CommandHandlingProcessManager,
                                             TestUtilTaskCreationPm> {
    }

    /**
     * The test class for the {@code TestUtilCreateTask} command handler in
     * {@code CommandHandlingProcessManager}.
     */
    public static class TimestampProcessManagerTest
            extends ProcessManagerCommandTest<TestUtilProjectId,
                                              TestUtilCreateTask,
                                              TestUtilTaskCreationPm,
                                              CommandHandlingProcessManager> {

        public static final TestUtilCreateTask TEST_COMMAND =
                TestUtilCreateTask.newBuilder()
                                  .setId(ID)
                                  .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected TestUtilProjectId newId() {
            return ID;
        }

        @Override
        protected TestUtilCreateTask createMessage() {
            return TEST_COMMAND;
        }

        @Override
        protected Repository<TestUtilProjectId, CommandHandlingProcessManager>
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
