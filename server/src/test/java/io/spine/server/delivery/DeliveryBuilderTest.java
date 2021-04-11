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

package io.spine.server.delivery;

import com.google.protobuf.Duration;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.memory.InMemoryShardedWorkRegistry;
import io.spine.server.storage.StorageFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.protobuf.util.Durations.fromMinutes;
import static io.spine.testing.TestValues.nullRef;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`DeliveryBuilder` should")
class DeliveryBuilderTest {

    private static DeliveryBuilder builder() {
        return Delivery.newBuilder();
    }

    @Nested
    @DisplayName("not accept `null`")
    class NotAcceptNull {

        @Test
        @DisplayName("delivery strategy")
        void strategy() {
            assertThrows(NullPointerException.class,
                         () -> builder().setStrategy(nullRef()));
        }

        @Test
        @DisplayName("Inbox storage")
        void inboxStorage() {
            assertThrows(NullPointerException.class,
                         () -> builder().setInboxStorage(nullRef()));
        }

        @Test
        @DisplayName("Catch-up storage")
        void catchUpStorage() {
            assertThrows(NullPointerException.class,
                         () -> builder().setCatchUpStorage(nullRef()));
        }

        @Test
        @DisplayName("work registry")
        void workRegistry() {
            assertThrows(NullPointerException.class,
                         () -> builder().setWorkRegistry(nullRef()));
        }

        @Test
        @DisplayName("deduplication window")
        void deduplicationWindow() {
            assertThrows(NullPointerException.class,
                         () -> builder().setDeduplicationWindow(nullRef()));
        }

        @Test
        @DisplayName("delivery monitor")
        void deliveryMonitor() {
            assertThrows(NullPointerException.class,
                         () -> builder().setMonitor(nullRef()));
        }
    }

    @Test
    @DisplayName("accept only positive page size")
    void acceptOnlyPositivePageSize() {
        assertThrows(IllegalArgumentException.class,
                     () -> builder().setPageSize(0));
        assertThrows(IllegalArgumentException.class,
                     () -> builder().setPageSize(-3));
    }

    @Test
    @DisplayName("accept only positive catch-up page size")
    void acceptOnlyPositiveCatchUpPageSize() {
        assertThrows(IllegalArgumentException.class,
                     () -> builder().setCatchUpPageSize(0));
        assertThrows(IllegalArgumentException.class,
                     () -> builder().setCatchUpPageSize(-3));
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
            StorageFactory factory = ServerEnvironment.instance()
                                                      .storageFactory();
            InboxStorage storage = new InboxStorage(factory, false);
            assertEquals(storage, builder().setInboxStorage(storage)
                                           .inboxStorage()
                                           .get());
        }

        @Test
        @DisplayName("Catch-up storage")
        void catchUpStorage() {
            StorageFactory factory = ServerEnvironment.instance()
                                                      .storageFactory();
            CatchUpStorage storage = new CatchUpStorage(factory, false);
            assertEquals(storage, builder().setCatchUpStorage(storage)
                                           .catchUpStorage()
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
        @DisplayName("deduplication window")
        void deduplicationWindow() {
            Duration duration = fromMinutes(123);
            assertEquals(duration, builder().setDeduplicationWindow(duration)
                                            .deduplicationWindow()
                                            .get());
        }

        @Test
        @DisplayName("delivery monitor")
        void deliveryMonitor() {
            DeliveryMonitor monitor = DeliveryMonitor.alwaysContinue();
            assertEquals(monitor, builder().setMonitor(monitor)
                                           .deliveryMonitor()
                                           .get());
        }

        @Test
        @DisplayName("page size")
        void pageSize() {
            int pageSize = 42;
            assertEquals(pageSize, builder().setPageSize(pageSize)
                                            .pageSize()
                                            .get());
        }

        @Test
        @DisplayName("catch-up page size")
        void catchUpPageSize() {
            int catchUpPageSize = 499;
            assertEquals(catchUpPageSize, builder().setCatchUpPageSize(catchUpPageSize)
                                                   .catchUpPageSize()
                                                   .get());
        }
    }

    @Nested
    @DisplayName("throw `NullPointerException` if attempting to get the unset value of")
    class ThrowNpe {

        @Test
        @DisplayName("delivery strategy")
        void strategy() {
            assertThrows(NullPointerException.class, () -> builder().getStrategy());
        }

        @Test
        @DisplayName("Inbox storage")
        void inboxStorage() {
            assertThrows(NullPointerException.class, () -> builder().getInboxStorage());
        }

        @Test
        @DisplayName("Catch-up storage")
        void catchUpStorage() {
            assertThrows(NullPointerException.class, () -> builder().getCatchUpStorage());
        }

        @Test
        @DisplayName("work registry")
        void workRegistry() {
            assertThrows(NullPointerException.class, () -> builder().getWorkRegistry());
        }

        @Test
        @DisplayName("deduplication window")
        void deduplicationWindow() {
            assertThrows(NullPointerException.class, () -> builder().getDeduplicationWindow());
        }

        @Test
        @DisplayName("delivery monitor")
        void deliveryMonitor() {
            assertThrows(NullPointerException.class, () -> builder().getMonitor());
        }

        @Test
        @DisplayName("page size")
        void pageSize() {
            assertThrows(NullPointerException.class, () -> builder().getPageSize());
        }
    }
}
