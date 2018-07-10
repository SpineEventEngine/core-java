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

package io.spine.server.expected;

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import io.spine.core.Rejection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static io.spine.server.expected.given.CommandExpectedTestEnv.commandExpected;
import static io.spine.server.expected.given.CommandExpectedTestEnv.commandExpectedWithEvent;
import static io.spine.server.expected.given.CommandExpectedTestEnv.commandExpectedWithRejection;
import static io.spine.server.expected.given.CommandExpectedTestEnv.rejection;
import static io.spine.server.expected.given.MessageProducingExpectedTestEnv.blankExpected;
import static io.spine.server.expected.given.MessageProducingExpectedTestEnv.emptyExpected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("CommandExpected should")
class CommandExpectedShould {

    @Test
    @DisplayName("validate the rejection")
    void trackRejection() {
        CommandExpected<UInt64Value> expected =
                commandExpectedWithRejection(rejection());
        expected.throwsRejection(Rejection.class);
    }

    @Test
    @DisplayName("ignore message if no events were generated")
    void ignoreNoEvents() {
        MessageProducingExpected<UInt64Value> expected = blankExpected();
        expected.ignoresMessage();
    }

    @Test
    @DisplayName("ignore message if the single Empty was generated")
    void ignoreEmptyEvent() {
        MessageProducingExpected<UInt64Value> expected = emptyExpected();
        expected.ignoresMessage();
    }

    @Test
    @DisplayName("not ignore message if it was rejected")
    void notIgnoreRejectedCommand() {
        CommandExpected<UInt64Value> expected =
                commandExpectedWithRejection(rejection());
        assertThrows(AssertionFailedError.class, expected::ignoresMessage);
    }

    @Test
    @DisplayName("not track events if rejected")
    void notTrackEventsIfRejected() {
        CommandExpected<UInt64Value> expected =
                commandExpectedWithRejection(rejection());
        assertThrows(AssertionFailedError.class, () -> expected.producesEvents(StringValue.class));
    }

    @Test
    @DisplayName("track produced events")
    void trackEvents() {
        CommandExpected<UInt64Value> expected = commandExpected();
        expected.producesEvents(StringValue.class, StringValue.class);
    }

    @Test
    @DisplayName("validate the single produced events")
    void trackSingleEvent() {
        StringValue expectedEvent = StringValue.newBuilder()
                                               .setValue("single produced event")
                                               .build();
        CommandExpected<UInt64Value> expected = commandExpectedWithEvent(expectedEvent);
        expected.producesEvent(StringValue.class, event -> {
            assertEquals(expectedEvent, event);
        });
    }
}
