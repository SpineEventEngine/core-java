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
import io.spine.testing.server.TUTaskCreationPm;
import io.spine.testing.server.expected.CommandHandlerExpected;
import io.spine.testing.server.procman.given.SamplePmCommandTest;
import io.spine.testing.server.procman.given.pm.CommandHandlingPm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.procman.given.SamplePmCommandTest.TEST_COMMAND;
import static io.spine.testing.server.procman.given.pm.CommandHandlingPm.NESTED_COMMAND;
import static io.spine.testing.server.procman.given.pm.CommandHandlingPm.newInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("ProcessManagerCommandTest should")
class PmCommandTestShould {

    private SamplePmCommandTest pmCommandTest;

    @BeforeEach
    void setUp() {
        pmCommandTest = new SamplePmCommandTest();
    }

    @Test
    @DisplayName("store tested command")
    void shouldStoreCommand() {
        pmCommandTest.setUp();
        assertEquals(pmCommandTest.storedMessage(), TEST_COMMAND);
    }

    @Test
    @DisplayName("dispatch tested command and store results")
    @SuppressWarnings("CheckReturnValue")
    void shouldDispatchCommand() {
        pmCommandTest.setUp();
        pmCommandTest.init();
        CommandHandlingPm testPm = newInstance();
        CommandHandlerExpected<TUTaskCreationPm> expected =
                pmCommandTest.expectThat(testPm);

        expected.producesCommand(TUAssignTask.class, command -> {
            assertEquals(NESTED_COMMAND, command);
        });
    }
}
