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

package io.spine.server.delivery;

import com.google.protobuf.Duration;
import io.spine.protobuf.Durations2;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import io.spine.server.storage.memory.InMemoryInboxStorage;
import io.spine.testing.Tests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Delivery Builder should")
public class DeliveryBuilderTest {

    private static Delivery.Builder builder() {
        return Delivery.newBuilder();
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("delivery strategy")
        void strategy() {
            assertThrows(NullPointerException.class,
                         () -> builder().setStrategy(Tests.nullRef()));
        }

        @Test
        @DisplayName("Inbox storage")
        void inboxStorage() {
            assertThrows(NullPointerException.class,
                         () -> builder().setInboxStorage(Tests.nullRef()));
        }

        @Test
        @DisplayName("work registry")
        void workRegistry() {
            assertThrows(NullPointerException.class,
                         () -> builder().setWorkRegistry(Tests.nullRef()));
        }

        @Test
        @DisplayName("idempotence window")
        void idempotenceWindow() {
            assertThrows(NullPointerException.class,
                         () -> builder().setIdempotenceWindow(Tests.nullRef()));
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")    // testing `Builder` getters.
    @Nested
    @DisplayName("return set")
    class ReturnSet {

        @Test
        @DisplayName("delivery strategy")
        void strategy() {
            DeliveryStrategy strategy = UniformAcrossAllShards.forNumber(42);
            assertEquals(strategy, builder().setStrategy(strategy)
                                            .strategy()
                                            .get());
        }

        @Test
        @DisplayName("Inbox storage")
        void inboxStorage() {
            InMemoryInboxStorage storage = new InMemoryInboxStorage(false);
            assertEquals(storage, builder().setInboxStorage(storage)
                                           .inboxStorage()
                                           .get());
        }

        @Test
        @DisplayName("work registry")
        void workRegistry() {
            InMemoryShardedWorkRegistry registry = new InMemoryShardedWorkRegistry();
            assertEquals(registry, builder().setWorkRegistry(registry)
                                            .workRegistry()
                                            .get());
        }

        @Test
        @DisplayName("idempotence window")
        void idempotenceWindow() {
            Duration duration = Durations2.fromMinutes(123);
            assertEquals(duration, builder().setIdempotenceWindow(duration)
                                            .idempotenceWindow()
                                            .get());
        }
    }
}
