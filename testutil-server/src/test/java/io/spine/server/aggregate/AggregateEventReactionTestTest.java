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

package io.spine.server.aggregate;

import com.google.protobuf.StringValue;
import com.google.protobuf.util.Timestamps;
import io.spine.server.MessageProducingExpected;
import io.spine.server.aggregate.given.AggregateEventReactionTestTestEnv.EventReactingAggregate;
import io.spine.server.aggregate.given.AggregateEventReactionTestTestEnv.EventReactingAggregateTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.given.AggregateEventReactionTestTestEnv.EventReactingAggregateTest.TEST_EVENT;
import static io.spine.server.aggregate.given.AggregateEventReactionTestTestEnv.aggregate;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("ProcessManagerEventReactionTest should")
class AggregateEventReactionTestTest {

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
        MessageProducingExpected<StringValue> expected = aggregateEventTest.expectThat(aggregate);

        expected.producesEvent(StringValue.class, event -> {
            assertEquals(event.getValue(), Timestamps.toString(TEST_EVENT));
        });
        expected.hasState(state -> {
            assertEquals(state.getValue(), Timestamps.toString(TEST_EVENT));
        });
    }
}
