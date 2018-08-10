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

import io.spine.testing.server.aggregate.given.SamplePartCommandTest;
import io.spine.testing.server.aggregate.given.agg.TuAggregatePart;
import io.spine.testing.server.aggregate.given.agg.TuAggregateRoot;
import io.spine.testing.server.given.entity.TuTaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.server.aggregate.given.SamplePartCommandTest.TEST_COMMAND;
import static io.spine.testing.server.aggregate.given.agg.TuAggregatePart.ID;
import static io.spine.testing.server.aggregate.given.agg.TuAggregatePart.newInstance;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregatePartCommandTest should")
class AggregatePartCommandTestShould {

    private SamplePartCommandTest aggregatePartCommandTest;

    @BeforeEach
    void setUp() {
        aggregatePartCommandTest = new SamplePartCommandTest();
    }

    @Test
    @DisplayName("store tested command")
    void shouldStoreCommand() {
        aggregatePartCommandTest.setUp();
        assertEquals(aggregatePartCommandTest.storedMessage(), TEST_COMMAND);
    }

    @Test
    @DisplayName("dispatch tested command")
    void shouldDispatchCommand() {
        aggregatePartCommandTest.setUp();
        TuAggregatePart testPart = newInstance(
                TuAggregateRoot.newInstance(ID));
        aggregatePartCommandTest.expectThat(testPart)
                                .hasState(state -> {
                                    assertTrue(isNotDefault(state.getTimestamp()));
                                });
    }

    @Test
    @DisplayName("create new part")
    void shouldCreatePart() {
        TuTaskId id = TuTaskId.newBuilder()
                              .setValue(newUuid())
                              .build();
        aggregatePartCommandTest.setUp();
        TuAggregatePart part = aggregatePartCommandTest.createPart(id);
        assertNotNull(part);
        assertEquals(id, part.getId());
    }
}
