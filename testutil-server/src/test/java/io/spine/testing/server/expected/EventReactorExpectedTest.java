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

package io.spine.testing.server.expected;

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static io.spine.testing.server.expected.given.EventHandlerExpectedTestEnv.blankExpected;
import static io.spine.testing.server.expected.given.EventHandlerExpectedTestEnv.emptyExpected;
import static io.spine.testing.server.expected.given.EventHandlerExpectedTestEnv.expected;
import static io.spine.testing.server.expected.given.EventHandlerExpectedTestEnv.expectedWithCommand;
import static io.spine.testing.server.expected.given.EventHandlerExpectedTestEnv.expectedWithEvent;
import static io.spine.testing.server.expected.given.EventHandlerExpectedTestEnv.newState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EventHandlerExpected should")
class EventReactorExpectedTest {

    @Test
    @DisplayName("validate state")
    void validateState() {
        EventReactorExpected<UInt64Value> expected = expected();
        expected.hasState(state -> assertEquals(newState(), state));
    }

    @Test
    @DisplayName("track produced events")
    void trackEvents() {
        EventReactorExpected<UInt64Value> expected = expected();
        expected.producesMessages(StringValue.class, StringValue.class);
        assertThrows(AssertionFailedError.class, () -> expected.producesMessages(StringValue.class));
    }

    @Test
    @DisplayName("validate the single produced events")
    void trackSingleEvent() {
        StringValue expectedEvent = StringValue.newBuilder()
                                               .setValue("single produced event")
                                               .build();
        EventReactorExpected<UInt64Value> expected = expectedWithEvent(expectedEvent);
        expected.producesMessage(StringValue.class, event -> assertEquals(expectedEvent, event));
    }

    @Test
    @DisplayName("track routed commands")
    void trackCommands() {
        EventReactorExpected<UInt64Value> expected = expected();
        expected.producesCommands(StringValue.class, StringValue.class);
        assertThrows(AssertionFailedError.class, () ->
                expected.producesCommands(StringValue.class)
        );
    }

    @Test
    @DisplayName("validate the single routed command")
    void trackSingleCommand() {
        StringValue expectedCommand = StringValue.newBuilder()
                                                 .setValue("single routed command")
                                                 .build();
        EventReactorExpected<UInt64Value> expected = expectedWithCommand(expectedCommand);
        expected.producesCommand(StringValue.class, command ->
                assertEquals(expectedCommand, command));
    }

    @Test
    @DisplayName("ignore message if no events were generated")
    void ignoreNoEvents() {
        EventReactorExpected<UInt64Value> expected = blankExpected();
        expected.ignoresMessage();
    }

    @Test
    @DisplayName("ignore message if the single Empty was generated")
    void ignoreEmptyEvent() {
        EventReactorExpected<UInt64Value> expected = emptyExpected();
        expected.ignoresMessage();
    }
}
