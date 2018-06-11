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
import io.spine.server.bus.BusBuilderShould;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.test.Tests;
import io.spine.validate.MessageValidator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.spine.server.BoundedContext.newName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
public class EventBusBuilderShould extends BusBuilderShould<EventBus.Builder,
                                                            EventEnvelope,
                                                            Event> {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    private StorageFactory storageFactory;

    @Override
    protected EventBus.Builder builder() {
        return EventBus.newBuilder();
    }

    @Before
    public void setUp() {
        BoundedContext bc = BoundedContext
                .newBuilder()
                .setMultitenant(true)
                .build();
        this.storageFactory = bc.getStorageFactory();
    }

    @Test
    public void do_not_accept_null_EventStore() {
        thrown.expect(NullPointerException.class);
        builder().setEventStore(Tests.nullRef());
    }

    @Test
    public void return_set_StorageFactory() {
        assertEquals(storageFactory, builder().setStorageFactory(storageFactory)
                                              .getStorageFactory()
                                              .get());
    }

    @Test
    public void return_EventStore_if_set() {
        EventStore mock = mock(EventStore.class);
        assertEquals(mock, builder().setEventStore(mock)
                                    .getEventStore()
                                    .get());
    }

    @Test
    public void return_stream_Executor_for_EventStore_if_set() {
        Executor mock = mock(Executor.class);
        assertEquals(mock, builder().setEventStoreStreamExecutor(mock)
                                    .getEventStoreStreamExecutor()
                                    .get());
    }

    @Test
    public void do_not_accept_null_EventValidator() {
        thrown.expect(NullPointerException.class);
        builder().setEventValidator(Tests.nullRef());
    }

    @Test
    public void return_set_EventValidator() {
        final MessageValidator validator = MessageValidator.newInstance();
        assertEquals(validator, builder().setEventValidator(validator)
                                         .getEventValidator()
                                         .get());
    }

    @Test
    public void require_set_EventStore_or_StorageFactory() {
        thrown.expect(IllegalStateException.class);
        EventBus.newBuilder()
                .build();
    }

    @Test
    public void set_event_validator_if_not_set_explicitly() {
        assertNotNull(builder().setStorageFactory(storageFactory)
                               .build()
                               .getMessageValidator());
    }

    @Test
    public void accept_null_Enricher() {
        assertNull(builder().setEnricher(Tests.nullRef())
                            .getEnricher()
                            .orNull());
    }

    @Test
    public void return_set_Enricher() {
        EventEnricher enricher = mock(EventEnricher.class);

        assertEquals(enricher, builder().setStorageFactory(storageFactory)
                                        .setEnricher(enricher)
                                        .getEnricher()
                                        .get());
    }

    @Test
    public void not_accept_StorageFactory_if_EventStore_already_specified() {
        EventBus.Builder builder = builder().setEventStore(mock(EventStore.class));
        thrown.expect(IllegalStateException.class);
        builder.setStorageFactory(storageFactory);
    }

    @Test
    public void not_accept_EventStore_if_StorageFactory_already_specified() {
        EventBus.Builder builder = builder().setStorageFactory(mock(StorageFactory.class));
        thrown.expect(IllegalStateException.class);
        builder.setEventStore(mock(EventStore.class));
    }

    @Test
    public void not_accept_EventStore_if_EventStoreStreamExecutor_already_specified() {
        EventBus.Builder builder = builder().setEventStoreStreamExecutor(mock(Executor.class));
        thrown.expect(IllegalStateException.class);
        builder.setEventStore(mock(EventStore.class));
    }

    @Test
    public void not_accept_EventStoreStreamExecutor_if_EventStore_already_specified() {
        EventBus.Builder builder = builder().setEventStore(mock(EventStore.class));
        thrown.expect(IllegalStateException.class);
        builder.setEventStoreStreamExecutor(mock(Executor.class));
    }

    @Test
    public void use_directExecutor_if_EventStoreStreamExecutor_not_set() {
        EventBus build = builder()
                .setStorageFactory(storageFactory)
                .build();
        Executor streamExecutor = build.getEventStore()
                                       .getStreamExecutor();
        ensureExecutorDirect(streamExecutor);
    }

    @Test
    public void use_passed_executor() {
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
    public void allow_custom_message_validators() {
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
    public void allow_configuring_logging_level_for_post_operations() {
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
