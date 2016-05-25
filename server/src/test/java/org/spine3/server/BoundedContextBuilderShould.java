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

package org.spine3.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.command.CommandBus;
import org.spine3.server.event.EventBus;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import static org.junit.Assert.*;
import static org.spine3.testdata.TestCommands.newCommandBus;
import static org.spine3.testdata.TestEventFactory.newEventBus;

@SuppressWarnings("InstanceMethodNamingConvention")
public class BoundedContextBuilderShould {

    private StorageFactory storageFactory;

    @Before
    public void setUp() {
        storageFactory = InMemoryStorageFactory.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        storageFactory.close();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_StorageFactory() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setStorageFactory(null);
    }

    @Test
    public void return_StorageFactory_from_builder() {
        final StorageFactory sf = InMemoryStorageFactory.getInstance();
        final BoundedContext.Builder builder = BoundedContext.newBuilder().setStorageFactory(sf);
        assertEquals(sf, builder.getStorageFactory());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandDispatcher() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setCommandBus(null);
    }

    @Test
    public void return_CommandBus_from_builder() {
        final CommandBus expected = newCommandBus(storageFactory);
        final BoundedContext.Builder builder = BoundedContext.newBuilder().setCommandBus(expected);
        assertEquals(expected, builder.getCommandBus());
    }

    @Test
    public void return_EventBus_from_builder() {
        final EventBus expected = newEventBus(storageFactory);
        final BoundedContext.Builder builder = BoundedContext.newBuilder().setEventBus(expected);
        assertEquals(expected, builder.getEventBus());
    }

    @Test
    public void return_if_multitenant_from_builder() {
        final BoundedContext.Builder builder = BoundedContext.newBuilder()
                .setMultitenant(true);
        assertTrue(builder.isMultitenant());
    }

    @Test
    public void be_not_multitenant_by_default() {
        assertFalse(BoundedContext.newBuilder()
                                  .isMultitenant());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventBus() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setEventBus(null);
    }

    @Test
    public void create_CommandBus_using_set_StorageFactory_if_CommandBus_was_not_set() {
        // Pass EventBus to builder initialization, and do NOT pass CommandBus.
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                   .setStorageFactory(storageFactory)
                                                   .setEventBus(newEventBus(storageFactory))
                                                   .build();
        assertNotNull(boundedContext.getCommandBus());
    }

    @Test
    public void create_EventBus_using_set_StorageFactory_if_EventBus_was_not_set() {
        // Pass CommandBus to builder initialization, and do NOT pass EventBus.
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                   .setStorageFactory(storageFactory)
                                                   .setCommandBus(newCommandBus(storageFactory))
                                                   .build();
        assertNotNull(boundedContext.getEventBus());
    }
}
