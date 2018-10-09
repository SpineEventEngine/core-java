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

import io.spine.testing.server.aggregate.given.SamplePartEventReactionTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregateRoot;
import io.spine.testing.server.aggregate.given.agg.TuReactingAggregatePart;
import io.spine.testing.server.given.entity.event.TuCommentLimitReached;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.server.aggregate.given.SamplePartEventReactionTest.TEST_EVENT;
import static io.spine.testing.server.aggregate.given.agg.TuReactingAggregatePart.ID;
import static io.spine.testing.server.aggregate.given.agg.TuReactingAggregatePart.newInstance;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test for {@link io.spine.testing.server.aggregate.AggregatePartEventReactionTest}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregatePartEventReactionTest should")
class AggregatePartEventReactionTestShould {

    private SamplePartEventReactionTest aggregatePartEventTest;

    @BeforeEach
    void setUp() {
        aggregatePartEventTest = new SamplePartEventReactionTest();
    }

    @Test
    @DisplayName("store tested event")
    void shouldStoreEvent() {
        aggregatePartEventTest.setUp();
        assertEquals(aggregatePartEventTest.storedMessage(), TEST_EVENT);
    }

    @Test
    @DisplayName("dispatch tested event")
    void shouldDispatchEvent() {
        aggregatePartEventTest.setUp();
        TuReactingAggregatePart testPart = newInstance(TuAggregateRoot.newInstance(ID));
        aggregatePartEventTest.expectThat(testPart)
                              .hasState(state -> {
                                  assertTrue(state.getCommentLimitReached());
                              })
                              .producesEvents(TuCommentLimitReached.class);
    }
}
