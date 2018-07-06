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

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import io.spine.core.CommandContext;
import io.spine.core.React;
import io.spine.core.UserId;
import io.spine.server.command.Assign;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerCommandTest;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.validate.StringValueVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProcessManagerCommandTestTestEnv {

    /**
     * Prevents direct instantiation.
     */
    private ProcessManagerCommandTestTestEnv() {
    }

    public static CommandHandlingProcessManager processManager() {
        StringValue state = StringValue.newBuilder()
                                       .setValue("state")
                                       .build();
        return Given.processManagerOfClass(CommandHandlingProcessManager.class)
                    .withId(ProcessManagerCommandTestTestEnv.class.getName())
                    .withState(state)
                    .build();
    }

    public static class CommandHandlingProcessManager extends ProcessManager<String, StringValue, StringValueVBuilder> {

        public static final UserId NESTED_COMMAND = UserId.newBuilder()
                                                          .setValue("test nested command")
                                                          .build();

        protected CommandHandlingProcessManager(String id) {
            super(id);
        }

        @Assign
        UInt64Value handle(UInt64Value command, CommandContext context) {
            newRouterFor(command, context).add(NESTED_COMMAND)
                                          .routeAll();
            return command;
        }

        @React
        void on(UInt64Value event) {
            getBuilder().setValue(Long.toString(event.getValue()));
        }

        @Assign
        Empty handle(UserId command) {
            getBuilder().setValue(command.getValue());
            return Empty.getDefaultInstance();
        }
    }

    public static class CommandHandlingProcessManagerRepository
            extends ProcessManagerRepository<String, CommandHandlingProcessManager, StringValue> {

    }

    public static class TimestampProcessManagerTest
            extends ProcessManagerCommandTest<UInt64Value, String, StringValue, CommandHandlingProcessManager> {

        public static final UInt64Value TEST_COMMAND = UInt64Value.newBuilder()
                                                                  .setValue(541L)
                                                                  .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected String newId() {
            return ProcessManagerCommandTestTestEnv.class.getName();
        }

        @Override
        protected UInt64Value createMessage() {
            return TEST_COMMAND;
        }

        @Override
        protected Repository<String, CommandHandlingProcessManager> createEntityRepository() {
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
