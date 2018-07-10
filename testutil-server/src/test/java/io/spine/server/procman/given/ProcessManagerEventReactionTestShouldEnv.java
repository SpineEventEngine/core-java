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
import io.spine.core.Enrichment;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerEventReactionTest;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.testutil.server.aggregate.TestUtilAssignTask;
import io.spine.testutil.server.aggregate.TestUtilProjectId;
import io.spine.testutil.server.aggregate.TestUtilTaskAssigned;
import io.spine.testutil.server.aggregate.TestUtilTaskCreated;
import io.spine.testutil.server.aggregate.TestUtilTaskCreationPm;
import io.spine.testutil.server.aggregate.TestUtilTaskCreationPmVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProcessManagerEventReactionTestShouldEnv {

    private static final TestUtilProjectId ID = TestUtilProjectId.newBuilder()
                                                                 .setValue("test pm id")
                                                                 .build();

    /**
     * Prevents direct instantiation.
     */
    private ProcessManagerEventReactionTestShouldEnv() {
    }

    public static EventReactingProcessManager processManager() {
        return Given.processManagerOfClass(EventReactingProcessManager.class)
                    .withId(ID)
                    .build();
    }

    /**
     * The dummy process manager that reacts on {@code TestUtilTaskCreated} event and
     * routes a nested command.
     */
    public static class EventReactingProcessManager
            extends ProcessManager<TestUtilProjectId,
                                   TestUtilTaskCreationPm,
                                   TestUtilTaskCreationPmVBuilder> {

        public static final TestUtilTaskAssigned RESULT_EVENT =
                TestUtilTaskAssigned.newBuilder()
                                    .setId(ID)
                                    .build();
        public static final TestUtilAssignTask NESTED_COMMAND =
                TestUtilAssignTask.newBuilder()
                                  .setId(ID)
                                  .build();

        protected EventReactingProcessManager(TestUtilProjectId id) {
            super(id);
        }

        @React
        @SuppressWarnings("CheckReturnValue")
        TestUtilTaskAssigned on(TestUtilTaskCreated event, EventContext context) {
            newRouterFor(event, context.getCommandContext()).add(NESTED_COMMAND)
                                                            .routeAll();
            return RESULT_EVENT;
        }
    }

    private static class EventReactingProcessManagerRepository
            extends ProcessManagerRepository<TestUtilProjectId,
                                             EventReactingProcessManager,
                                             TestUtilTaskCreationPm> {
    }

    /**
     * The test class for the {@code TestUtilTaskCreated} event handler in
     * {@code EventReactingProcessManager}.
     */
    public static class EventReactingProcessManagerTest
            extends ProcessManagerEventReactionTest<TestUtilProjectId,
                                                    TestUtilTaskCreated,
                                                    TestUtilTaskCreationPm,
                                                    EventReactingProcessManager> {

        public static final TestUtilTaskCreated TEST_EVENT =
                TestUtilTaskCreated.newBuilder()
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
        protected TestUtilTaskCreated createMessage() {
            return TEST_EVENT;
        }

        @Override
        protected Repository<TestUtilProjectId, EventReactingProcessManager>
        createEntityRepository() {
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
