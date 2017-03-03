/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventEnvelope;
import org.spine3.server.event.enrich.EventEnricher;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.validate.MessageValidator;
import org.spine3.test.Tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class EventBusBuilderShould {

    private StorageFactory storageFactory;

    @Before
    public void setUp() {
        this.storageFactory = InMemoryStorageFactory.getInstance();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventStore() {
        EventBus.newBuilder()
                .setEventStore(Tests.<EventStore>nullRef());
    }

    @Test
    public void return_set_StorageFactory() {
        assertEquals(storageFactory, EventBus.newBuilder()
                                             .setStorageFactory(storageFactory)
                                             .getStorageFactory()
                                             .get());
    }

    @Test
    public void return_EventStore_if_set() {
        final EventStore mock = mock(EventStore.class);
        assertEquals(mock, EventBus.newBuilder()
                                   .setEventStore(mock)
                                   .getEventStore()
                                   .get());
    }

    @Test
    public void return_stream_Executor_for_EventStore_if_set() {
        final Executor mock = mock(Executor.class);
        assertEquals(mock, EventBus.newBuilder()
                                   .setEventStoreStreamExecutor(mock)
                                   .getEventStoreStreamExecutor()
                                   .get());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventValidator() {
        EventBus.newBuilder()
                .setEventValidator(Tests.<MessageValidator>nullRef());
    }

    @Test
    public void return_set_EventValidator() {
        final MessageValidator validator = MessageValidator.newInstance();
        assertEquals(validator, EventBus.newBuilder()
                                        .setEventValidator(validator)
                                        .getEventValidator()
                                        .get());
    }

    @Test(expected = IllegalStateException.class)
    public void require_set_EventStore_or_StorageFactory() {
        EventBus.newBuilder()
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_DispatcherEventDelivery() {
        EventBus.newBuilder()
                .setDispatcherEventDelivery(Tests.<DispatcherEventDelivery>nullRef());
    }

    @Test
    public void return_set_DispatcherEventDelivery() {
        // Create a custom event executor to differ from the default one.
        final DispatcherEventDelivery delivery = new DispatcherEventDelivery() {
            @Override
            public boolean shouldPostponeDelivery(EventEnvelope event, EventDispatcher dispatcher) {
                return true;
            }
        };
        assertEquals(delivery, EventBus.newBuilder()
                                       .setDispatcherEventDelivery(delivery)
                                       .getDispatcherEventDelivery()
                                       .get());
    }

    @Test
    public void set_direct_DispatcherEventDelivery_if_not_set_explicitly() {
        final DispatcherEventDelivery actualValue = EventBus.newBuilder()
                                                            .setStorageFactory(storageFactory)
                                                            .build()
                                                            .delivery();
        assertEquals(DispatcherEventDelivery.directDelivery(), actualValue);
    }

    @Test
    public void set_event_validator_if_not_set_explicitly() {
        assertNotNull(EventBus.newBuilder()
                              .setStorageFactory(storageFactory)
                              .build()
                              .getEventValidator());
    }

    @Test
    public void accept_null_Enricher() {
        assertNull(EventBus.newBuilder()
                           .setEnricher(Tests.<EventEnricher>nullRef())
                           .getEnricher()
                           .orNull());
    }

    @Test
    public void return_set_Enricher() {
        final EventEnricher enricher = mock(EventEnricher.class);

        assertEquals(enricher, EventBus.newBuilder()
                                       .setStorageFactory(storageFactory)
                                       .setEnricher(enricher)
                                       .getEnricher()
                                       .get());
    }

    @Test(expected = IllegalStateException.class)
    public void not_accept_StorageFactory_if_EventStore_already_specified() {
        final EventBus.Builder builder = EventBus.newBuilder()
                                                 .setEventStore(mock(EventStore.class));
        builder.setStorageFactory(storageFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void not_accept_EventStore_if_StorageFactory_already_specified() {
        final EventBus.Builder builder = EventBus.newBuilder()
                                                 .setStorageFactory(mock(StorageFactory.class));
        builder.setEventStore(mock(EventStore.class));
    }

    @Test(expected = IllegalStateException.class)
    public void not_accept_EventStore_if_EventStoreStreamExecutor_already_specified() {
        final EventBus.Builder builder = EventBus.newBuilder()
                                                 .setEventStoreStreamExecutor(mock(Executor.class));
        builder.setEventStore(mock(EventStore.class));
    }

    @Test(expected = IllegalStateException.class)
    public void not_accept_EventStoreStreamExecutor_if_EventStore_already_specified() {
        final EventBus.Builder builder = EventBus.newBuilder()
                                                 .setEventStore(mock(EventStore.class));
        builder.setEventStoreStreamExecutor(mock(Executor.class));
    }

    @Test
    public void use_directExecutor_if_EventStoreStreamExecutor_not_set() {
        final EventBus.Builder builder = EventBus.newBuilder()
                                                 .setStorageFactory(storageFactory);
        final EventBus build = builder.build();
        final Executor streamExecutor = build.getEventStore()
                                             .getStreamExecutor();
        ensureExecutorDirect(streamExecutor);
    }

    @Test
    public void use_passed_executor() {
        final CountDownLatch executorUsageLatch = new CountDownLatch(1);
        final Executor simpleExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {

                // Decrease the counter to ensure this method has been called.
                executorUsageLatch.countDown();
            }
        };
        final EventBus.Builder builder = EventBus.newBuilder()
                                                 .setStorageFactory(storageFactory)
                                                 .setEventStoreStreamExecutor(simpleExecutor);
        final EventBus build = builder.build();
        final Executor streamExecutor = build.getEventStore()
                                             .getStreamExecutor();
        streamExecutor.execute(mock(Runnable.class));
        try {
            /**
             * The executor configured to operate synchronously,
             * so the latch should already be {@code zero} at this point.
             **/
            executorUsageLatch.await(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("The specified executor was not used.");
        }
    }

    private static void ensureExecutorDirect(Executor streamExecutor) {
        final long mainThreadId = Thread.currentThread()
                                        .getId();
        streamExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final long runnableThreadId = Thread.currentThread()
                                                    .getId();
                assertEquals(mainThreadId, runnableThreadId);
            }
        });
    }
}
