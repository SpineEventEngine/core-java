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
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import io.spine.core.Enrichment;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerEventReactionTest;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.validate.StringValueVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProcessManagerEventReactionTestShouldEnv {

    /**
     * Prevents direct instantiation.
     */
    private ProcessManagerEventReactionTestShouldEnv() {
    }

    public static EventReactingProcessManager processManager() {
        StringValue state = StringValue.newBuilder()
                                       .setValue("state")
                                       .build();
        return Given.processManagerOfClass(EventReactingProcessManager.class)
                    .withId(ProcessManagerEventReactionTestShouldEnv.class.getName())
                    .withState(state)
                    .build();
    }

    /**
     * The dummy process manager that reacts on {@code UInt32Value} event, routes a nested command.
     */
    public static class EventReactingProcessManager
            extends ProcessManager<String, StringValue, StringValueVBuilder> {

        public static final UInt32Value RESULT_EVENT = UInt32Value.newBuilder()
                                                                  .setValue(123)
                                                                  .build();
        public static final StringValue NESTED_COMMAND = StringValue.newBuilder()
                                                                     .setValue("command")
                                                                     .build();

        protected EventReactingProcessManager(String id) {
            super(id);
        }

        @React
        @SuppressWarnings("CheckReturnValue")
        UInt32Value on(UInt64Value event, EventContext context) {
            newRouterFor(event, context.getCommandContext()).add(NESTED_COMMAND)
                                                            .routeAll();
            return RESULT_EVENT;
        }
    }

    private static class EventReactingProcessManagerRepository
            extends ProcessManagerRepository<String, EventReactingProcessManager, StringValue> {
    }

    public static class EventReactingProcessManagerTest
            extends ProcessManagerEventReactionTest<UInt64Value,
                                                    String,
                                                    StringValue,
                                                    EventReactingProcessManager> {

        public static final UInt64Value TEST_EVENT = UInt64Value.newBuilder()
                                                                .setValue(125)
                                                                .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected String newId() {
            return ProcessManagerEventReactionTestShouldEnv.class.getName();
        }

        @Override
        protected UInt64Value createMessage() {
            return TEST_EVENT;
        }

        @Override
        protected Repository<String, EventReactingProcessManager> createEntityRepository() {
            return new EventReactingProcessManagerRepository();
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

        @Override
        protected Enrichment enrichment() {
            return Enrichment.newBuilder()
                             .build();
        }
    }
}
