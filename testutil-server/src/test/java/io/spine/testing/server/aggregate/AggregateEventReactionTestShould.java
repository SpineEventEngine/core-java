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

import io.spine.testing.server.TUProjectAggregate;
import io.spine.testing.server.TUProjectAssigned;
import io.spine.testing.server.expected.EventReactorExpected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.aggregate.given.AggregateEventReactionTestShouldEnv.EventReactingAggregate;
import static io.spine.testing.server.aggregate.given.AggregateEventReactionTestShouldEnv.EventReactingAggregateTest;
import static io.spine.testing.server.aggregate.given.AggregateEventReactionTestShouldEnv.EventReactingAggregateTest.TEST_EVENT;
import static io.spine.testing.server.aggregate.given.AggregateEventReactionTestShouldEnv.aggregate;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("ProcessManagerEventReactionTest should")
class AggregateEventReactionTestShould {

    private EventReactingAggregateTest aggregateEventTest;

    @BeforeEach
    void setUp() {
        aggregateEventTest = new EventReactingAggregateTest();
    }

    @Test
    @DisplayName("store tested event")
    void shouldStoreCommand() {
        aggregateEventTest.setUp();
        assertEquals(aggregateEventTest.storedMessage(), TEST_EVENT);
    }

    @Test
    @DisplayName("dispatch tested event and store results")
    @SuppressWarnings("CheckReturnValue")
    void shouldDispatchCommand() {
        aggregateEventTest.setUp();
        aggregateEventTest.init();
        EventReactingAggregate aggregate = aggregate();
        EventReactorExpected<TUProjectAggregate> expected = aggregateEventTest.expectThat(aggregate);

        expected.producesEvent(TUProjectAssigned.class, event -> {
            assertNotNull(event);
            assertTrue(isNotDefault(aggregate.getState().getTimestamp()));
        });
        expected.hasState(state -> assertTrue(isNotDefault(state.getTimestamp())));
    }
}
