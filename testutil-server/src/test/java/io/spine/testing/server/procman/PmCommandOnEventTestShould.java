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

package io.spine.testing.server.procman;

import io.spine.testing.server.expected.CommanderExpected;
import io.spine.testing.server.given.entity.TuPmState;
import io.spine.testing.server.given.entity.command.TuAssignProject;
import io.spine.testing.server.procman.given.SamplePmCommandOnEventTest;
import io.spine.testing.server.procman.given.pm.CommandingPm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.procman.given.SamplePmCommandOnEventTest.TEST_EVENT;
import static io.spine.testing.server.procman.given.pm.CommandingPm.newInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vladyslav Lubenskyi
 */
@DisplayName("PmCommandOnCommandTest should")
class PmCommandOnEventTestShould {

    private SamplePmCommandOnEventTest pmCommandingTest;

    @BeforeEach
    void setUp() {
        pmCommandingTest = new SamplePmCommandOnEventTest();
        pmCommandingTest.setUp();
    }

    @Test
    @DisplayName("store incoming command")
    void storeGeneratedCommand() {
        assertEquals(pmCommandingTest.storedMessage(), TEST_EVENT);
    }

    @Test
    @DisplayName("dispatch tested event and store results")
    void shouldDispatchCommand() {
        CommandingPm testPm = newInstance();
        CommanderExpected<TuPmState> expected = pmCommandingTest.expectThat(testPm);
        expected.producesCommand(
                TuAssignProject.class,
                c -> assertEquals(c.getId(), testPm.id())
        );
    }
}
