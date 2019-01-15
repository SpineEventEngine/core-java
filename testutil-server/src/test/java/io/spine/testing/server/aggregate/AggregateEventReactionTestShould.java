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

package io.spine.testing.server.aggregate;

import io.spine.testing.server.aggregate.given.SampleEventReactionTest;
import io.spine.testing.server.aggregate.given.SamplePartEventReactionTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregateRoot;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregate;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregatePart;
import io.spine.testing.server.expected.EventReactorExpected;
import io.spine.testing.server.given.entity.TuComments;
import io.spine.testing.server.given.entity.TuProject;
import io.spine.testing.server.given.entity.event.TuCommentLimitReached;
import io.spine.testing.server.given.entity.event.TuProjectAssigned;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit tst fot {@link io.spine.testing.server.aggregate.AggregateEventReactionTest}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregateEventReactionTest should")
class AggregateEventReactionTestShould {

    @Nested
    @DisplayName("for aggregate")
    class ForAggregate {

        private SampleEventReactionTest aggregateEventTest;

        @BeforeEach
        void setUp() {
            aggregateEventTest = new SampleEventReactionTest();
            aggregateEventTest.setUp();
        }

        @AfterEach
        void tearDown() {
            aggregateEventTest.tearDown();
        }

        @Test
        @DisplayName("store tested event")
        void shouldStoreEvent() {
            assertEquals(aggregateEventTest.storedMessage(), SampleEventReactionTest.TEST_EVENT);
        }

        @Test
        @DisplayName("dispatch tested event and store results")
        @SuppressWarnings("CheckReturnValue")
        void shouldDispatchEvent() {
            TuReactingAggregate aggregate = TuReactingAggregate.newInstance();
            EventReactorExpected<TuProject> expected = aggregateEventTest.expectThat(aggregate);

            expected.producesEvent(TuProjectAssigned.class,
                                   event -> assertTrue(isNotDefault(event)));
            expected.hasState(state -> assertTrue(isNotDefault(state.getTimestamp())));
        }
    }

    @Nested
    @DisplayName("for aggregate part")
    class ForPart {

        private SamplePartEventReactionTest partEventTest;

        @BeforeEach
        void setUp() {
            partEventTest = new SamplePartEventReactionTest();
            partEventTest.setUp();
        }

        @AfterEach
        void tearDown() {
            partEventTest.tearDown();
        }

        @Test
        @DisplayName("store tested event")
        void shouldStoreEvent() {
            assertEquals(partEventTest.storedMessage(), SamplePartEventReactionTest.TEST_EVENT);
        }

        @Test
        @DisplayName("dispatch tested event and store results")
        @SuppressWarnings("CheckReturnValue")
        void shouldDispatchEvent() {
            TuAggregateRoot root = TuAggregateRoot.newInstance(TuReactingAggregatePart.ID);
            TuReactingAggregatePart part = TuReactingAggregatePart.newInstance(root);
            EventReactorExpected<TuComments> expected = partEventTest.expectThat(part);

            expected.producesEvent(TuCommentLimitReached.class,
                                   event -> assertTrue(isNotDefault(event)));
            expected.hasState(state -> assertTrue(state.getCommentLimitReached()));
        }
    }
}
