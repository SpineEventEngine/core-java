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

package io.spine.testing.server.expected.given;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import io.spine.testing.server.expected.EventReactorExpected;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author Vladyslav Lubenskyi
 */
public class EventHandlerExpectedTestEnv {

    /**
     * Prevents direct instantiation.
     */
    private EventHandlerExpectedTestEnv() {
    }

    public static List<Message> events() {
        StringValue firstEvent = StringValue.newBuilder()
                                            .setValue("event 1")
                                            .build();
        StringValue secondEvent = StringValue.newBuilder()
                                             .setValue("event 2")
                                             .build();
        return asList(firstEvent, secondEvent);
    }

    public static List<Message> interceptedCommands() {
        StringValue firstCommand = StringValue.newBuilder()
                                              .setValue("command 1")
                                              .build();
        StringValue secondCommand = StringValue.newBuilder()
                                               .setValue("command 2")
                                               .build();
        return asList(firstCommand, secondCommand);
    }

    public static EventReactorExpected<UInt64Value> expected() {
        EventReactorExpected<UInt64Value> expected =
                new EventReactorExpected<>(events(),
                                           oldState(),
                                           newState(),
                                           interceptedCommands());
        return expected;
    }

    public static EventReactorExpected<UInt64Value> expectedWithEvent(Message event) {
        EventReactorExpected<UInt64Value> expected =
                new EventReactorExpected<>(singletonList(event),
                                           oldState(),
                                           newState(),
                                           interceptedCommands());
        return expected;
    }

    public static EventReactorExpected<UInt64Value> expectedWithCommand(Message command) {
        EventReactorExpected<UInt64Value> expected =
                new EventReactorExpected<>(events(),
                                           oldState(),
                                           newState(),
                                           singletonList(command));
        return expected;
    }

    public static EventReactorExpected<UInt64Value> blankExpected() {
        EventReactorExpected<UInt64Value> expected =
                new EventReactorExpected<>(emptyList(),
                                           oldState(),
                                           oldState(),
                                           emptyList());
        return expected;
    }

    public static EventReactorExpected<UInt64Value> emptyExpected() {
        EventReactorExpected<UInt64Value> expected =
                new EventReactorExpected<>(singletonList(Empty.getDefaultInstance()),
                                           oldState(),
                                           oldState(),
                                           emptyList());
        return expected;
    }

    public static UInt64Value newState() {
        return UInt64Value.newBuilder()
                          .setValue(2L)
                          .build();
    }

    public static UInt64Value oldState() {
        return UInt64Value.newBuilder()
                          .setValue(1L)
                          .build();
    }
}
