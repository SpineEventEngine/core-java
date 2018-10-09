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

package io.spine.testing.server.aggregate;

import io.spine.testing.server.aggregate.given.SampleEventImportTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregate;
import io.spine.testing.server.expected.EventReactorExpected;
import io.spine.testing.server.given.entity.TuProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.aggregate.given.SampleEventImportTest.TEST_EVENT;
import static io.spine.testing.server.aggregate.given.agg.TuAggregate.newInstance;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test for {@link io.spine.testing.server.aggregate.AggregateEventImportTest}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregateEventImportTest should")
class AggregateEventImportTestShould {

    private SampleEventImportTest aggregateImportEventTest;

    @BeforeEach
    void setUp() {
        aggregateImportEventTest = new SampleEventImportTest();
        aggregateImportEventTest.setUp();
    }

    @AfterEach
    void tearDown() {
        aggregateImportEventTest.tearDown();
    }

    @Test
    @DisplayName("store tested event")
    void shouldStoreEvent() {
        assertEquals(aggregateImportEventTest.storedMessage(), TEST_EVENT);
    }

    @Test
    @DisplayName("dispatch tested event and store results")
    @SuppressWarnings("CheckReturnValue")
    void shouldDispatchEvent() {
        TuAggregate aggregate = newInstance();
        EventReactorExpected<TuProject> expected = aggregateImportEventTest.expectThat(aggregate);
        expected.hasState(state -> assertTrue(isNotDefault(state.getTimestamp())));
    }
}
