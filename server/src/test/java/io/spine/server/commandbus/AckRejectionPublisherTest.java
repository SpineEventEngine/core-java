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

package io.spine.server.commandbus;

import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.bus.Acks;
import io.spine.server.bus.Listener;
import io.spine.server.event.EventBus;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventEnvelope;
import io.spine.test.commandbus.CmdBusCaffetteriaId;
import io.spine.test.commandbus.command.CmdBusEntryDenied;
import io.spine.testing.client.TestActorRequestFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`AckRejectionPublisher` should")
class AckRejectionPublisherTest {

    private MemoizingListener listener;
    private AckRejectionPublisher publisher;

    @BeforeEach
    void createPublisher() {
        listener = new MemoizingListener();
        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .addEventListener(listener)
                .build();
        EventBus eventBus = context.eventBus();
        publisher = new AckRejectionPublisher(eventBus);
    }

    @Test
    @DisplayName("publish the passed rejection to event bus")
    void publishRejection() {
        TestActorRequestFactory requestFactory =
                new TestActorRequestFactory(AckRejectionPublisherTest.class);
        Command command = requestFactory.generateCommand();
        CommandEnvelope origin = CommandEnvelope.of(command);

        CmdBusEntryDenied throwable = CmdBusEntryDenied
                .newBuilder()
                .setId(CmdBusCaffetteriaId.generate())
                .setVisitorCount(15)
                .setReason("Test command bus rejection.")
                .build();
        RejectionEnvelope rejection = RejectionEnvelope.from(origin, throwable);
        Ack ack = Acks.reject(CommandId.generate(), rejection);
        publisher.onNext(ack);
        assertThat(listener.lastReceived)
                .isNotNull();
        assertThat(listener.lastReceived.message())
                .isEqualTo(throwable.messageThrown());
    }

    @Test
    @DisplayName("ignore non-rejection `Ack`s")
    void ignoreNonRejection() {
        Ack ack = Acks.acknowledge(CommandId.generate());
        publisher.onNext(ack);
        assertThat(listener.lastReceived)
                .isNull();
    }

    @Test
    @DisplayName("re-throw an error passed to `onError` as `IllegalStateException`")
    void rethrowError() {
        RuntimeException exception = new RuntimeException("Test Ack publisher exception.");
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                                                    () -> publisher.onError(exception));
        assertThat(thrown.getCause())
                .isEqualTo(exception);
    }

    private static class MemoizingListener implements Listener<EventEnvelope> {

        private @Nullable EventEnvelope lastReceived;

        @Override
        public void accept(EventEnvelope envelope) {
            lastReceived = envelope;
        }
    }
}
