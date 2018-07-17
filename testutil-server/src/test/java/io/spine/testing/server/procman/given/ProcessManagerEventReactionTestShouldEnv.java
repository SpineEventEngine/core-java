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
import io.spine.core.Enrichment;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.entity.Repository;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerRepository;
import io.spine.testing.server.TUAssignTask;
import io.spine.testing.server.TUProjectId;
import io.spine.testing.server.TUTaskAssigned;
import io.spine.testing.server.TUTaskCreated;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.TUTaskCreationPmVBuilder;
import io.spine.testing.server.entity.given.Given;
import io.spine.testing.server.expected.EventHandlerExpected;
import io.spine.testing.server.procman.ProcessManagerEventReactionTest;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProcessManagerEventReactionTestShouldEnv {

    private static final TUProjectId ID = TUProjectId.newBuilder()
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
     * The dummy process manager that reacts on {@code TUTaskCreated} event and
     * routes a nested command.
     */
    public static class EventReactingProcessManager
            extends ProcessManager<TUProjectId,
            TUTaskCreationPm,
            TUTaskCreationPmVBuilder> {

        public static final TUTaskAssigned RESULT_EVENT =
                TUTaskAssigned.newBuilder()
                              .setId(ID)
                              .build();
        public static final TUAssignTask NESTED_COMMAND =
                TUAssignTask.newBuilder()
                            .setId(ID)
                            .build();

        protected EventReactingProcessManager(TUProjectId id) {
            super(id);
        }

        @React
        @SuppressWarnings("CheckReturnValue")
        TUTaskAssigned on(TUTaskCreated event, EventContext context) {
            newRouterFor(event, context.getCommandContext()).add(NESTED_COMMAND)
                                                            .routeAll();
            return RESULT_EVENT;
        }
    }

    private static class EventReactingProcessManagerRepository
            extends ProcessManagerRepository<TUProjectId,
                                             EventReactingProcessManager,
                                             TUTaskCreationPm> {
    }

    /**
     * The test class for the {@code TUTaskCreated} event handler in
     * {@code EventReactingProcessManager}.
     */
    public static class EventReactingProcessManagerTest
            extends ProcessManagerEventReactionTest<TUProjectId,
                                                    TUTaskCreated,
                                                    TUTaskCreationPm,
                                                    EventReactingProcessManager> {

        public static final TUTaskCreated TEST_EVENT =
                TUTaskCreated.newBuilder()
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
        protected TUTaskCreated createMessage() {
            return TEST_EVENT;
        }

        @Override
        protected Repository<TUProjectId, EventReactingProcessManager> createEntityRepository() {
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

        @Override
        public EventHandlerExpected<TUTaskCreationPm>
        expectThat(EventReactingProcessManager entity) {
            return super.expectThat(entity);
        }
    }
}
