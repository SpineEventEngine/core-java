/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.bus;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.Status.StatusCase;
import io.spine.server.bus.given.BusFilters;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.test.bus.ShareId;
import io.spine.test.bus.ShareTraded;
import io.spine.test.bus.command.ShareCannotBeTraded;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.core.Status.StatusCase.OK;
import static io.spine.core.Status.StatusCase.REJECTION;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`BusFilter` should")
class BusFilterTest {

    private CommandEnvelope commandEnvelope;

    @BeforeEach
    void createCommandEnvelope() {
        commandEnvelope = commandEnvelope();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicInstanceMethods(new BusFilters.RejectingWithOk());
    }

    @Test
    @DisplayName("let the message pass the filter")
    void letPass() {
        BusFilter<CommandEnvelope> filter = new BusFilters.Accepting();
        Optional<Ack> ack = filter.filter(commandEnvelope);
        assertThat(ack).isEmpty();
    }

    @Test
    @DisplayName("reject the message with the `OK` status")
    void rejectWithOk() {
        BusFilter<CommandEnvelope> filter = new BusFilters.RejectingWithOk();
        Optional<Ack> ack = filter.filter(commandEnvelope);
        assertThat(ack).isPresent();

        Ack theAck = ack.get();
        assertIdEquals(theAck);
        assertStatusEquals(theAck, OK);
    }

    @Test
    @DisplayName("reject the message with an error")
    void rejectWithError() {
        Error error = Error
                .newBuilder()
                .setType(BusFilterTest.class.getCanonicalName())
                .setMessage("Ignore this error.")
                .build();
        BusFilter<CommandEnvelope> filter = new BusFilters.RejectingWithError(error);
        Optional<Ack> ack = filter.filter(commandEnvelope);
        assertThat(ack).isPresent();

        Ack theAck = ack.get();
        assertIdEquals(theAck);
        assertStatusEquals(theAck, ERROR);
        assertThat(theAck.getStatus()
                         .getError()).isEqualTo(error);
    }

    @Test
    @DisplayName("reject the message with a rejection")
    void rejectWithThrowableMessage() {
        ShareCannotBeTraded rejection = ShareCannotBeTraded
                .newBuilder()
                .setShare(ShareId.newBuilder().setValue(newUuid()).build())
                .setReason("The test rejection.")
                .build();
        BusFilter<CommandEnvelope> filter =
                new BusFilters.RejectingWithThrowableMessage(rejection);
        Optional<Ack> ack = filter.filter(commandEnvelope);
        assertThat(ack).isPresent();

        Ack theAck = ack.get();
        assertIdEquals(theAck);
        assertStatusEquals(theAck, REJECTION);
        Message rejectionMessage = unpack(theAck.getStatus()
                                                .getRejection()
                                                .getMessage());
        assertThat(rejectionMessage).isEqualTo(rejection.messageThrown());
    }

    @Test
    @DisplayName("throw `IAE` when throwing business rejection from non-command-handling filter")
    void failOnThrowableMessageForNonCommand() {
        BusFilters.Throwing throwingFilter = new BusFilters.Throwing();
        assertThrows(IllegalArgumentException.class,
                     () -> throwingFilter.filter(eventEnvelope()));
    }

    private void assertIdEquals(Ack ack) {
        Message id = unpack(ack.getMessageId());
        assertThat(id).isEqualTo(commandEnvelope.id());
    }

    private static void assertStatusEquals(Ack ack, StatusCase status) {
        assertThat(ack.getStatus().getStatusCase()).isEqualTo(status);
    }

    private static CommandEnvelope commandEnvelope() {
        TestActorRequestFactory requestFactory = new TestActorRequestFactory(BusFilterTest.class);
        Command command = requestFactory.generateCommand();
        return CommandEnvelope.of(command);
    }

    private static EventEnvelope eventEnvelope() {
        TestEventFactory eventFactory = TestEventFactory.newInstance(BusFilterTest.class);
        ShareId id = ShareId
                .newBuilder()
                .setValue(newUuid())
                .build();
        ShareTraded eventMessage = ShareTraded
                .newBuilder()
                .setShare(id)
                .setAmount(42)
                .build();
        Event event = eventFactory.createEvent(eventMessage);
        return EventEnvelope.of(event);
    }
}
