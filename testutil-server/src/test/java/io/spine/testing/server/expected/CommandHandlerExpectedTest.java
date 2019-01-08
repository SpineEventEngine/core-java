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
import io.spine.testing.server.given.entity.rejection.Rejections.TuFailedToAssignProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.blankExpected;
import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.commandExpected;
import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.commandExpectedWithCommand;
import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.commandExpectedWithEvent;
import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.commandExpectedWithRejection;
import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.emptyExpected;
import static io.spine.testing.server.expected.given.CommandExpectedTestEnv.rejectionMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("CommandHandlerExpected should")
class CommandHandlerExpectedTest {

    @Test
    @DisplayName("validate the rejection")
    void trackRejection() {
        CommandHandlerExpected<UInt64Value> expected =
                commandExpectedWithRejection(rejectionMessage());
        expected.throwsRejection(TuFailedToAssignProject.class);
    }

    @Test
    @DisplayName("ignore message if no events were generated")
    void ignoreNoEvents() {
        CommandHandlerExpected<UInt64Value> expected = blankExpected();
        expected.ignoresMessage();
    }

    @Test
    @DisplayName("ignore message if the single Empty was generated")
    void ignoreEmptyEvent() {
        CommandHandlerExpected<UInt64Value> expected = emptyExpected();
        expected.ignoresMessage();
    }

    @Test
    @DisplayName("not ignore message if it was rejected")
    void notIgnoreRejectedCommand() {
        CommandHandlerExpected<UInt64Value> expected =
                commandExpectedWithRejection(rejectionMessage());
        assertThrows(AssertionFailedError.class, expected::ignoresMessage);
    }

    @Test
    @DisplayName("not track events if rejected")
    void notTrackEventsIfRejected() {
        CommandHandlerExpected<UInt64Value> expected =
                commandExpectedWithRejection(rejectionMessage());
        assertThrows(
                AssertionFailedError.class,
                () -> expected.producesEvents(StringValue.class)
        );
    }

    @Test
    @DisplayName("track produced events")
    void trackEvents() {
        CommandHandlerExpected<UInt64Value> expected = commandExpected();
        expected.producesEvents(StringValue.class, StringValue.class);
    }

    @Test
    @DisplayName("validate the single produced events")
    void trackSingleEvent() {
        StringValue expectedEvent = StringValue
                .newBuilder()
                .setValue("single produced event")
                .build();
        CommandHandlerExpected<UInt64Value> expected = commandExpectedWithEvent(expectedEvent);
        expected.producesEvent(
                StringValue.class,
                event -> assertEquals(expectedEvent, event)
        );
    }

    @Test
    @DisplayName("track routed commands")
    void trackCommands() {
        CommandHandlerExpected<UInt64Value> expected = commandExpected();
        expected.producesCommands(StringValue.class, StringValue.class);
        assertThrows(
                AssertionFailedError.class,
                () -> expected.producesCommands(StringValue.class)
        );
    }

    @Test
    @DisplayName("validate the single routed command")
    void trackSingleCommand() {
        StringValue expectedCommand = StringValue
                .newBuilder()
                .setValue("single routed command")
                .build();
        CommandHandlerExpected<UInt64Value> expected = commandExpectedWithCommand(expectedCommand);
        expected.producesCommand(
                StringValue.class,
                command -> assertEquals(expectedCommand, command)
        );
    }
}
