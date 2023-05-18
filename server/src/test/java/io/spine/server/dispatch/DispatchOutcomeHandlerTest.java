/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.dispatch;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.Truth;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.server.dispatch.given.Given;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

@DisplayName("DispatchOutcomeHandler should")
public final class DispatchOutcomeHandlerTest {

    @Test
    @DisplayName("allow default doNothing handlers")
    void allowDefaultHandlers() {
        DispatchOutcome outcome = DispatchOutcome.getDefaultInstance();
        DispatchOutcomeHandler handler = DispatchOutcomeHandler.from(outcome);
        assertThat(handler.handle()).isEqualTo(outcome);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        DispatchOutcome outcome = DispatchOutcome.getDefaultInstance();
        DispatchOutcomeHandler handler = DispatchOutcomeHandler.from(outcome);
        new NullPointerTester().testAllPublicInstanceMethods(handler);
    }

    @Nested
    @DisplayName("handle")
    final class Handle {

        @Test
        @DisplayName("error outcome")
        void error() {
            Error error = Error
                    .newBuilder()
                    .setCode(1)
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setError(error)
                    .build();
            List<Error> errors = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onError(errors::add)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            assertThat(errors).containsExactly(error);
        }

        @Test
        @DisplayName("success outcome")
        void success() {
            Success success = Success
                    .newBuilder()
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setSuccess(success)
                    .build();
            List<Success> successes = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onSuccess(successes::add)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            assertThat(successes).containsExactly(success);
        }

        @Test
        @DisplayName("produced events outcome")
        void events() {
            Event event = Given.event();
            Success success = Success
                    .newBuilder()
                    .setProducedEvents(ProducedEvents
                                               .newBuilder()
                                               .addEvent(event)
                                               .build())
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setSuccess(success)
                    .build();
            List<Event> events = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onEvents(events::addAll)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            assertThat(events).containsExactly(event);
        }

        @Test
        @DisplayName("produced commands outcome")
        void commands() {
            Command command = Given.command();
            Success success = Success
                    .newBuilder()
                    .setProducedCommands(ProducedCommands
                                                 .newBuilder()
                                                 .addCommand(command)
                                                 .build())
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setSuccess(success)
                    .build();
            List<Command> commands = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onCommands(commands::addAll)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            assertThat(commands).containsExactly(command);
        }

        @Test
        @DisplayName("rejection outcome")
        void rejection() {
            Event rejection = Given.rejectionEvent();
            Success error = Success
                    .newBuilder()
                    .setRejection(rejection)
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setSuccess(error)
                    .build();
            List<Event> rejections = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onRejection(rejections::add)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            Truth.assertThat(rejections)
                 .containsExactly(rejection);
        }

        @Test
        @DisplayName("interruption outcome")
        void interruption() {
            MessageId messageId = MessageId
                    .newBuilder()
                    .setId(Identifier.pack(TestValues.randomString()))
                    .build();
            Interruption interruption = Interruption
                    .newBuilder()
                    .setStoppedAt(messageId)
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setInterrupted(interruption)
                    .build();
            List<Interruption> interruptions = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onInterruption(interruptions::add)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            assertThat(interruptions).containsExactly(interruption);
        }

        @Test
        @DisplayName("ignore outcome")
        void ignore() {
            Ignore ignore = Ignore
                    .newBuilder()
                    .setReason(TestValues.randomString())
                    .build();
            DispatchOutcome outcome = DispatchOutcome
                    .newBuilder()
                    .setIgnored(ignore)
                    .build();
            List<Ignore> ignored = new ArrayList<>();
            DispatchOutcome result = DispatchOutcomeHandler
                    .from(outcome)
                    .onIgnored(ignored::add)
                    .handle();
            assertThat(result).isEqualTo(outcome);
            assertThat(ignored).containsExactly(ignore);
        }
    }
}
