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

package io.spine.testing.server.procman;

import io.spine.testing.server.TUAssignTask;
import io.spine.testing.server.TUTaskAssigned;
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.expected.EventHandlerExpected;
import io.spine.testing.server.procman.given.ProcessManagerEventReactionTestShouldEnv.EventReactingProcessManager;
import io.spine.testing.server.procman.given.ProcessManagerEventReactionTestShouldEnv.EventReactingProcessManagerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.procman.given.ProcessManagerEventReactionTestShouldEnv.EventReactingProcessManager.NESTED_COMMAND;
import static io.spine.testing.server.procman.given.ProcessManagerEventReactionTestShouldEnv.EventReactingProcessManager.RESULT_EVENT;
import static io.spine.testing.server.procman.given.ProcessManagerEventReactionTestShouldEnv.EventReactingProcessManagerTest.TEST_EVENT;
import static io.spine.testing.server.procman.given.ProcessManagerEventReactionTestShouldEnv.processManager;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("ProcessManagerEventReactionTest should")
class ProcessManagerEventReactionTestShould {

    private EventReactingProcessManagerTest pmEventTest;

    @BeforeEach
    void setUp() {
        pmEventTest = new EventReactingProcessManagerTest();
    }

    @Test
    @DisplayName("store tested event")
    void shouldStoreCommand() {
        pmEventTest.setUp();
        assertEquals(pmEventTest.storedMessage(), TEST_EVENT);
    }

    @Test
    @DisplayName("dispatch tested event and store results")
    @SuppressWarnings("CheckReturnValue")
    void shouldDispatchCommand() {
        pmEventTest.setUp();
        pmEventTest.init();
        EventReactingProcessManager testPm = processManager();
        EventHandlerExpected<TUTaskCreationPm> expected = pmEventTest.expectThat(testPm);
        expected.routesCommand(TUAssignTask.class, command -> {
            assertEquals(command, NESTED_COMMAND);
        });
        expected.producesEvent(TUTaskAssigned.class, event -> {
            assertEquals(event, RESULT_EVENT);
        });
    }
}
