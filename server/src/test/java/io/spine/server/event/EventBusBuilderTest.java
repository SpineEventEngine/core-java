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

import io.spine.core.Event;
import io.spine.grpc.LoggingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.enrich.Enricher;
import io.spine.server.event.store.EventStore;
import io.spine.server.storage.StorageFactory;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.spine.server.event.given.EventStoreTestEnv.eventStore;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"OptionalGetWithoutIsPresent",
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("EventBus Builder should")
class EventBusBuilderTest
        extends BusBuilderTest<EventBus.Builder, EventEnvelope, Event> {

    private StorageFactory storageFactory;

    @Override
    protected EventBus.Builder builder() {
        return EventBus.newBuilder();
    }

    @BeforeEach
    void setUp() {
        BoundedContext bc = BoundedContext
                .newBuilder()
                .setMultitenant(true)
                .build();
        this.storageFactory = bc.storageFactory();
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("EventStore")
        void eventStore() {
            assertThrows(NullPointerException.class,
                         () -> builder().setEventStore(Tests.nullRef()));
        }
    }

    @Test
    @DisplayName("accept null Enricher")
    void acceptNullEnricher() {
        assertNull(builder().setEnricher(Tests.nullRef())
                            .getEnricher()
                            .orElse(null));
    }

    @Nested
    @DisplayName("return set")
    class ReturnSet {

        @Test
        @DisplayName("StorageFactory")
        void storageFactory() {
            assertEquals(storageFactory, builder().setStorageFactory(storageFactory)
                                                  .getStorageFactory()
                                                  .get());
        }

        @Test
        @DisplayName("EventStore")
        void obtainEventStore() {
            EventStore mock = eventStore();
            assertEquals(mock, builder().setEventStore(mock)
                                        .getEventStore()
                                        .get());
        }

        @Test
        @DisplayName("stream Executor for EventStore")
        void streamExecutor() {
            Executor mock = mock(Executor.class);
            assertEquals(mock, builder().setEventStoreStreamExecutor(mock)
                                        .getEventStoreStreamExecutor()
                                        .get());
        }

        @Test
        @DisplayName("Enricher")
        void enricher() {
            Enricher enricher = Enricher.newBuilder()
                                        .build();
            assertSame(enricher, builder().setStorageFactory(storageFactory)
                                          .setEnricher(enricher)
                                          .getEnricher()
                                          .get());
        }
    }

    @Test
    @DisplayName("throw ISE if neither EventStore nor StorageFactory is set")
    void requireEventStoreOrStorageFactory() {
        assertThrows(IllegalStateException.class, () -> EventBus.newBuilder()
                                                                .build());
    }

    @Nested
    @DisplayName("not allow to override")
    class NotOverride {

        private EventStore eventStore;

        @BeforeEach
        void setUp() {
            eventStore = eventStore();
        }

        @Test
        @DisplayName("EventStore by StorageFactory")
        void eventStoreByStorageFactory() {
            EventBus.Builder builder = builder().setEventStore(eventStore);
            assertThrows(IllegalStateException.class,
                         () -> builder.setStorageFactory(storageFactory));
        }

        @Test
        @DisplayName("StorageFactory by EventStore")
        void storageFactoryByEventStore() {
            EventBus.Builder builder = builder().setStorageFactory(mock(StorageFactory.class));
            assertThrows(IllegalStateException.class,
                         () -> builder.setEventStore(eventStore));
        }

        @Test
        @DisplayName("EventStoreStreamExecutor by EventStore")
        void eventExecutorByEventStore() {
            EventBus.Builder builder = builder().setEventStoreStreamExecutor(mock(Executor.class));
            assertThrows(IllegalStateException.class,
                         () -> builder.setEventStore(eventStore));
        }

        @Test
        @DisplayName("EventStore by EventStoreStreamExecutor")
        void eventStoreByEventExecutor() {
            EventBus.Builder builder = builder().setEventStore(eventStore);
            assertThrows(IllegalStateException.class,
                         () -> builder.setEventStoreStreamExecutor(mock(Executor.class)));
        }
    }

    @Nested
    @DisplayName("use executor")
    class UseExecutor {

        @Test
        @DisplayName("which is direct if no custom one was passed")
        void direct() {
            EventBus build = builder()
                    .setStorageFactory(storageFactory)
                    .build();
            Executor streamExecutor = build.getEventStore()
                                           .getStreamExecutor();
            ensureExecutorDirect(streamExecutor);
        }

        @Test
        @DisplayName("which was passed to Builder")
        void passed() {
            CountDownLatch executorUsageLatch = new CountDownLatch(1);

            // Decrease the counter to ensure this method has been called.
            Executor simpleExecutor = command -> executorUsageLatch.countDown();
            EventBus.Builder builder = builder().setStorageFactory(storageFactory)
                                                .setEventStoreStreamExecutor(simpleExecutor);
            EventBus build = builder.build();
            Executor streamExecutor = build.getEventStore()
                                           .getStreamExecutor();
            streamExecutor.execute(mock(Runnable.class));
            try {
            /* The executor configured to operate synchronously,
               so the latch should already be zero at this point.
             */
                executorUsageLatch.await(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                fail("The specified executor was not used.");
            }
        }
    }

    @Test
    @DisplayName("allow configuring logging level for post operations")
    void setLogLevelForPost() {
        // See that the default level is TRACE.
        assertEquals(LoggingObserver.Level.TRACE, builder().getLogLevelForPost());

        // Check setting new value.
        EventBus.Builder builder = builder();
        LoggingObserver.Level newLevel = LoggingObserver.Level.DEBUG;

        assertSame(builder, builder.setLogLevelForPost(newLevel));
        assertEquals(newLevel, builder.getLogLevelForPost());
    }

    private static void ensureExecutorDirect(Executor streamExecutor) {
        long mainThreadId = Thread.currentThread()
                                  .getId();
        streamExecutor.execute(() -> {
            long runnableThreadId = Thread.currentThread()
                                          .getId();
            assertEquals(mainThreadId, runnableThreadId);
        });
    }
}
