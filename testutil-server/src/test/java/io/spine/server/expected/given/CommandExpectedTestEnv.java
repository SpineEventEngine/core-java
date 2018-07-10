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

package io.spine.server.expected.given;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import io.spine.core.Rejection;
import io.spine.core.RejectionId;
import io.spine.server.expected.CommandExpected;

import java.util.List;

import static io.spine.server.expected.given.MessageProducingExpectedTestEnv.events;
import static io.spine.server.expected.given.MessageProducingExpectedTestEnv.newState;
import static io.spine.server.expected.given.MessageProducingExpectedTestEnv.oldState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author Vladyslav Lubenskyi
 */
public class CommandExpectedTestEnv {

    /**
     * Prevents direct instantiation.
     */
    private CommandExpectedTestEnv() {
    }

    public static Message rejection() {
        RejectionId id = RejectionId.newBuilder()
                                    .setValue("test rejection")
                                    .build();
        return Rejection.newBuilder()
                        .setId(id)
                        .build();
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

    public static CommandExpected<UInt64Value> commandExpectedWithRejection(Message rejection) {
        CommandExpected<UInt64Value> expected =
                new CommandExpected<>(events(),
                                      rejection,
                                      oldState(),
                                      newState(),
                                      interceptedCommands());
        return expected;
    }

    public static CommandExpected<UInt64Value> commandExpectedWithEvent(Message event) {
        CommandExpected<UInt64Value> expected =
                new CommandExpected<>(singletonList(event),
                                      null,
                                      oldState(),
                                      newState(),
                                      interceptedCommands());
        return expected;
    }

    public static CommandExpected<UInt64Value> commandExpected(

    ) {
        CommandExpected<UInt64Value> expected =
                new CommandExpected<>(events(),
                                      null,
                                      oldState(),
                                      newState(),
                                      interceptedCommands());
        return expected;
    }

    public static CommandExpected<UInt64Value> blankExpected() {
        CommandExpected<UInt64Value> expected =
                new CommandExpected<>(emptyList(),
                                      null,
                                      oldState(),
                                      oldState(),
                                      emptyList());
        return expected;
    }

    public static CommandExpected<UInt64Value> emptyExpected() {
        CommandExpected<UInt64Value> expected =
                new CommandExpected<>(singletonList(Empty.getDefaultInstance()),
                                      null,
                                      oldState(),
                                      oldState(),
                                      emptyList());
        return expected;
    }
}
