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
import io.spine.core.Rejection;
import io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.TimePrinter;
import io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.TimePrintingRejectionTest;
import io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.TimePrintingTest;
import io.spine.server.expected.CommandExpected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.TimePrintingTest.TEST_COMMAND;
import static io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.aggregate;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregateCommandTest should")
class AggregateCommandTestShould {

    private TimePrintingTest aggregateCommandTest;
    private TimePrintingRejectionTest aggregateRejectionCommandTest;

    @BeforeEach
    void setUp() {
        aggregateCommandTest = new TimePrintingTest();
        aggregateRejectionCommandTest = new TimePrintingRejectionTest();
    }

    @Test
    @DisplayName("store tested command")
    void shouldStoreCommand() {
        aggregateCommandTest.setUp();
        assertEquals(aggregateCommandTest.storedMessage(), TEST_COMMAND);
    }

    @Test
    @DisplayName("dispatch tested command")
    void shouldDispatchCommand() {
        aggregateCommandTest.setUp();
        TimePrinter testAggregate = aggregate();
        aggregateCommandTest.expectThat(testAggregate);
        String newState = testAggregate.getState()
                                       .getValue();
        assertEquals(newState, Timestamps.toString(TEST_COMMAND));
    }

    @Test
    @DisplayName("not fail when rejected")
    void shouldHandleRejection() {
        aggregateRejectionCommandTest.setUp();
        TimePrinter testAggregate = aggregate();
        CommandExpected<StringValue> expected = aggregateRejectionCommandTest.expectThat(
                testAggregate);
        expected.throwsRejection(Rejection.class);
    }
}
