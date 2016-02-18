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

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventStore;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;

import static org.junit.Assert.*;

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

    private static CommandBus newCommandDispatcher(StorageFactory storageFactory) {
        return CommandBus.create(new CommandStore(storageFactory.createCommandStorage()));
    }

    private static EventBus newEventBus(StorageFactory storageFactory) {
        return EventBus.newInstance(EventStore.newBuilder()
                                              .setStreamExecutor(MoreExecutors.directExecutor())
                                              .setStorage(storageFactory.createEventStorage())
                                              .build());
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
    public void return_CommandDispatcher_from_builder() {
        final CommandBus expected = newCommandDispatcher(storageFactory);
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
        assertFalse(BoundedContext.newBuilder().isMultitenant());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventBus() {
        //noinspection ConstantConditions
        BoundedContext.newBuilder().setEventBus(null);
    }
}
