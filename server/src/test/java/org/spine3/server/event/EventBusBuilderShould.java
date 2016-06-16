/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.validate.MessageValidator;
import org.spine3.test.Tests;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventBusBuilderShould {

    private EventStore eventStore;

    @Before
    public void setUp() {
        final StorageFactory storageFactory = InMemoryStorageFactory.getInstance();
        this.eventStore = EventStore.newBuilder()
                                    .setStreamExecutor(MoreExecutors.directExecutor())
                                    .setStorage(storageFactory.createEventStorage())
                                    .setLogger(EventStore.log())
                                    .build();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventStore() {
        EventBus.newBuilder()
                .setEventStore(Tests.<EventStore>nullRef());
    }

    @Test
    public void return_set_EventStore() {
        assertEquals(eventStore, EventBus.newBuilder()
                                         .setEventStore(eventStore)
                                         .getEventStore());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_Executor() {
        EventBus.newBuilder().setExecutor(Tests.<Executor>nullRef());
    }

    @Test
    public void return_set_Executor() {
        final Executor executor = MoreExecutors.directExecutor();
        assertEquals(executor, EventBus.newBuilder()
                                       .setExecutor(executor)
                                       .getExecutor());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventValidator() {
        EventBus.newBuilder()
                .setEventValidator(Tests.<MessageValidator>nullRef());
    }

    @Test
    public void return_set_EventValidator() {
        final MessageValidator validator = new MessageValidator();
        assertEquals(validator, EventBus.newBuilder()
                                        .setEventValidator(validator)
                                        .getEventValidator());
    }

    @Test(expected = NullPointerException.class)
    public void require_set_EventStore() {
        EventBus.newBuilder().build();
    }

    @Test
    public void sets_directExector_if_not_set_explicitly() {
        assertEquals(MoreExecutors.directExecutor(), EventBus.newBuilder()
                                                             .setEventStore(eventStore)
                                                             .build()
                                                             .getExecutor());
    }

    @Test
    public void set_event_validator_if_not_set_explicitly() {
        assertNotNull(EventBus.newBuilder()
                              .setEventStore(eventStore)
                              .build()
                              .getEventValidator());
    }
}
