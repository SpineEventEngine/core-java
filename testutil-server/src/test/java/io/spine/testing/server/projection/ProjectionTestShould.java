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

package io.spine.testing.server.projection;

import io.spine.testing.server.expected.EventSubscriberExpected;
import io.spine.testing.server.given.entity.TuString;
import io.spine.testing.server.projection.given.SampleProjectionTest;
import io.spine.testing.server.projection.given.prj.TuProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.projection.given.SampleProjectionTest.TEST_EVENT;
import static io.spine.testing.server.projection.given.prj.TuProjection.newInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ProjectionTest should")
class ProjectionTestShould {

    private SampleProjectionTest projectionTest;

    @BeforeEach
    void setUp() {
        projectionTest = new SampleProjectionTest();
        projectionTest.setUp();
    }

    @AfterEach
    void tearDown() {
        projectionTest.tearDown();
    }

    @Test
    @DisplayName("store tested event")
    void shouldStoreCommand() {
        assertEquals(projectionTest.storedMessage(), TEST_EVENT);
    }

    @Test
    @DisplayName("dispatch tested event and store results")
    @SuppressWarnings("CheckReturnValue")
    void shouldDispatchCommand() {
        TuProjection aggregate = newInstance();
        EventSubscriberExpected<TuString> expected = projectionTest.expectThat(aggregate);
        expected.hasState(state -> assertEquals(state.getValue(), TEST_EVENT.getId().getValue()));
    }
}
