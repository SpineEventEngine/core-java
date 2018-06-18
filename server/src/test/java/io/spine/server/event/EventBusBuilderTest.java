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

package io.spine.server.event;

import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.grpc.LoggingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.test.Tests;
import io.spine.validate.MessageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.spine.server.BoundedContext.newName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
@DisplayName("EventBus Builder should")
class EventBusBuilderTest extends BusBuilderTest<EventBus.Builder,
                                                 EventEnvelope,
                                                 Event> {

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
        this.storageFactory = bc.getStorageFactory();
    }

    @Test
    @DisplayName("not accept null EventStore")
    void notAcceptNullEventStore() {
        assertThrows(NullPointerException.class, () -> builder().setEventStore(Tests.nullRef()));
    }

    @Test
    @DisplayName("return set StorageFactory")
    void returnSetStorageFactory() {
        assertEquals(storageFactory, builder().setStorageFactory(storageFactory)
                                              .getStorageFactory()
                                              .get());
    }

    @Test
    @DisplayName("return EventStore if set")
    void returnEventStoreIfSet() {
        EventStore mock = mock(EventStore.class);
        assertEquals(mock, builder().setEventStore(mock)
                                    .getEventStore()
                                    .get());
    }

    @Test
    @DisplayName("return stream Executor for EventStore if set")
    void returnStreamExecutorForEventStoreIfSet() {
        Executor mock = mock(Executor.class);
        assertEquals(mock, builder().setEventStoreStreamExecutor(mock)
                                    .getEventStoreStreamExecutor()
                                    .get());
    }

    @Test
    @DisplayName("not accept null EventValidator")
    void notAcceptNullEventValidator() {
        assertThrows(NullPointerException.class,
                     () -> builder().setEventValidator(Tests.nullRef()));
    }

    @Test
    @DisplayName("return set EventValidator")
    void returnSetEventValidator() {
        final MessageValidator validator = MessageValidator.newInstance();
        assertEquals(validator, builder().setEventValidator(validator)
                                         .getEventValidator()
                                         .get());
    }

    @Test
    @DisplayName("require set EventStore or StorageFactory")
    void requireSetEventStoreOrStorageFactory() {
        assertThrows(IllegalStateException.class, () -> EventBus.newBuilder()
                                                                .build());
    }

    @Test
    @DisplayName("set event validator if not set explicitly")
    void setEventValidatorIfNotSetExplicitly() {
        assertNotNull(builder().setStorageFactory(storageFactory)
                               .build()
                               .getMessageValidator());
    }

    @Test
    @DisplayName("accept null Enricher")
    void acceptNullEnricher() {
        assertNull(builder().setEnricher(Tests.nullRef())
                            .getEnricher()
                            .orNull());
    }

    @Test
    @DisplayName("return set Enricher")
    void returnSetEnricher() {
        EventEnricher enricher = mock(EventEnricher.class);

        assertEquals(enricher, builder().setStorageFactory(storageFactory)
                                        .setEnricher(enricher)
                                        .getEnricher()
                                        .get());
    }

    @Test
    @DisplayName("not accept StorageFactory if EventStore already specified")
    void notAcceptStorageFactoryIfEventStoreAlreadySpecified() {
        EventBus.Builder builder = builder().setEventStore(mock(EventStore.class));
        assertThrows(IllegalStateException.class, () -> builder.setStorageFactory(storageFactory));
    }

    @Test
    @DisplayName("not accept EventStore if StorageFactory already specified")
    void notAcceptEventStoreIfStorageFactoryAlreadySpecified() {
        EventBus.Builder builder = builder().setStorageFactory(mock(StorageFactory.class));
        assertThrows(IllegalStateException.class,
                     () -> builder.setEventStore(mock(EventStore.class)));
    }

    @Test
    @DisplayName("not accept EventStore if EventStoreStreamExecutor already specified")
    void notAcceptEventStoreIfEventStoreStreamExecutorAlreadySpecified() {
        EventBus.Builder builder = builder().setEventStoreStreamExecutor(mock(Executor.class));
        assertThrows(IllegalStateException.class,
                     () -> builder.setEventStore(mock(EventStore.class)));
    }

    @Test
    @DisplayName("not accept EventStoreStreamExecutor if EventStore already specified")
    void notAcceptEventStoreStreamExecutorIfEventStoreAlreadySpecified() {
        EventBus.Builder builder = builder().setEventStore(mock(EventStore.class));
        assertThrows(IllegalStateException.class,
                     () -> builder.setEventStoreStreamExecutor(mock(Executor.class)));
    }

    @Test
    @DisplayName("use directExecutor if EventStoreStreamExecutor not set")
    void useDirectExecutorIfEventStoreStreamExecutorNotSet() {
        EventBus build = builder()
                .setStorageFactory(storageFactory)
                .build();
        Executor streamExecutor = build.getEventStore()
                                       .getStreamExecutor();
        ensureExecutorDirect(streamExecutor);
    }

    @Test
    @DisplayName("use passed executor")
    void usePassedExecutor() {
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

    @Test
    @DisplayName("allow custom message validators")
    void allowCustomMessageValidators() {
        StorageFactory storageFactory =
                StorageFactorySwitch.newInstance(newName("test"), false)
                                    .get();
        MessageValidator validator = mock(MessageValidator.class);
        EventBus eventBus = builder()
                .setEventValidator(validator)
                .setStorageFactory(storageFactory)
                .build();
        assertEquals(validator, eventBus.getMessageValidator());
    }

    @Test
    @DisplayName("allow configuring logging level for post operations")
    void configureLogLevelForPost() {
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
