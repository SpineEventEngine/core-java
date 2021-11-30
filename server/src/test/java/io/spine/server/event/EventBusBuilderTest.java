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

package io.spine.server.event;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.type.EventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.testing.Assertions.assertNpe;
import static io.spine.testing.TestValues.nullRef;

@DisplayName("`EventBus.Builder` should")
@SuppressWarnings("ThrowableNotThrown") // in custom assertions
class EventBusBuilderTest
        extends BusBuilderTest<EventBus.Builder, EventEnvelope, Event> {

    @Override
    protected EventBus.Builder builder() {
        return EventBus.newBuilder();
    }

    @Test
    @DisplayName("reject null `EventEnricher`")
    void rejectNullEnricher() {
        assertNpe(() -> builder().injectEnricher(nullRef()));
    }

    @Nested
    @DisplayName("assign `StreamObserver`")
    class PostObserver {

        @Test
        @DisplayName("assigning `noOpObserver()` if not assigned")
        void assigningDefault() {
            var bus = builder().build();
            var observer = bus.observer();
            assertThat(observer).isInstanceOf(noOpObserver().getClass());
        }

        @Test
        @DisplayName("assign custom observer")
        void customValue() {
            var observer = new StreamObserver<Ack>() {
                @Override
                public void onNext(Ack value) {
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                }
            };
            var bus = builder().setObserver(observer).build();
            assertThat(bus.observer()).isEqualTo(observer);
        }
    }
}
