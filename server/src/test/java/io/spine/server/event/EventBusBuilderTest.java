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

package io.spine.server.event;

import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings({"OptionalGetWithoutIsPresent",
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("EventBus Builder should")
class EventBusBuilderTest
        extends BusBuilderTest<EventBus.Builder, EventEnvelope, Event> {

    @Override
    protected EventBus.Builder builder() {
        return EventBus.newBuilder();
    }

    @Test
    @DisplayName("accept null Enricher")
    void acceptNullEnricher() {
        assertNull(builder().setEnricher(Tests.nullRef())
                            .enricher()
                            .orElse(null));
    }

    @Nested
    @DisplayName("return set")
    class ReturnSet {

        @Test
        @DisplayName("Enricher")
        void enricher() {
            EventEnricher enricher = EventEnricher
                    .newBuilder()
                    .build();
            assertSame(enricher, builder().setEnricher(enricher)
                                          .enricher()
                                          .get());
        }
    }

    @Nested
    @DisplayName("assign `StreamObserver`")
    class PostObserver {

        private EventBus.Builder builder;

        @BeforeEach
        void createBuilder() {
            builder = EventBus.newBuilder();
        }

        @Test
        @DisplayName("assigning `noOpObserver()` is not assigned")
        void assigningDefault() {
            assertThat(builder.build()
                              .observer())
                    .isInstanceOf(noOpObserver().getClass());
        }

        @Test
        @DisplayName("assign custom observer")
        void customValue() {
            StreamObserver<Ack> observer = new StreamObserver<Ack>() {
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

            assertThat(builder.setObserver(observer)
                              .build()
                              .observer())
                    .isEqualTo(observer);
        }

    }
}
