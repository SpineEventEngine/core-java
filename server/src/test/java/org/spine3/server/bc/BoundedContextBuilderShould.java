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

package org.spine3.server.bc;

import com.google.common.base.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandStore;
import org.spine3.server.event.EventBus;
import org.spine3.server.stand.StandUpdateDelivery;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.test.Tests;
import org.spine3.testdata.TestCommandBusFactory;
import org.spine3.testdata.TestEventBusFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("OptionalGetWithoutIsPresent" /* OK as we set right before get(). */)
public class BoundedContextBuilderShould {

    private StorageFactory storageFactory;
    private BoundedContext.Builder builder;

    @Before
    public void setUp() {
        storageFactory = InMemoryStorageFactory.getInstance();
        builder = BoundedContext.newBuilder();
    }

    @After
    public void tearDown() throws Exception {
        storageFactory.close();
    }

    @Test
    public void return_name_if_it_was_set() {
        final String name = getClass().getName();
        assertEquals(name, BoundedContext.newBuilder()
                                         .setName(name)
                                         .getName());
    }

    @Test
    public void return_storage_factory_supplier_if_it_was_set() {
        @SuppressWarnings("unchecked") // OK for mocks.
        Supplier<StorageFactory> mock = mock(Supplier.class);

        builder.setStorageFactorySupplier(mock);

        assertEquals(mock, builder.storageFactorySupplier().get());
    }

    @Test
    public void allow_clearing_storage_factory_supplier() {
        builder.setStorageFactorySupplier(Tests.<Supplier<StorageFactory>>nullRef());

        assertFalse(builder.storageFactorySupplier().isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandDispatcher() {
        builder.setCommandBus(Tests.<CommandBus>nullRef());
    }

    @Test
    public void return_CommandBus() {
        final CommandBus expected = TestCommandBusFactory.create(storageFactory);
        builder = BoundedContext.newBuilder()
                                .setCommandBus(expected);
        assertEquals(expected, builder.commandBus().get());
    }

    @Test
    public void return_EventBus() {
        final EventBus expected = TestEventBusFactory.create(storageFactory);
        builder.setEventBus(expected);
        assertEquals(expected, builder.eventBus().get());
    }

    @Test
    public void support_multitenantcy() {
        builder.setMultitenant(true);
        assertTrue(builder.isMultitenant());
    }

    @Test
    public void be_not_multitenant_by_default() {
        assertFalse(builder.isMultitenant());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventBus() {
        builder.setEventBus(Tests.<EventBus>nullRef());
    }

    @Test
    public void create_CommandBus_if_it_was_not_set() {
        // Pass EventBus to builder initialization, and do NOT pass CommandBus.
        final BoundedContext boundedContext = builder
                .setEventBus(TestEventBusFactory.create(storageFactory))
                .build();
        assertNotNull(boundedContext.getCommandBus());
    }

    @Test
    public void create_EventBus_if_it_was_not_set() {
        // Pass CommandBus to builder initialization, and do NOT pass EventBus.
        final BoundedContext boundedContext = builder
                .setMultitenant(true)
                .setCommandBus(TestCommandBusFactory.create(storageFactory))
                .build();
        assertNotNull(boundedContext.getEventBus());
    }

    @Test
    public void create_both_CommandBus_and_EventBus_if_not_set() {
        final BoundedContext boundedContext = builder.build();
        assertNotNull(boundedContext.getCommandBus());
        assertNotNull(boundedContext.getEventBus());
    }

    @Test
    public void accept_CommandStore() {
        final CommandStore commandStore = mock(CommandStore.class);
        builder.setCommandStore(commandStore);
        assertEquals(commandStore, builder.commandStore().get());
    }

    @Test(expected = NullPointerException.class)
    public void reject_null_CommandStore() {
        builder.setCommandStore(Tests.<CommandStore>nullRef());
    }

    @Test
    public void return_StandUpdateDelivery_if_set() {
        final StandUpdateDelivery mock = mock(StandUpdateDelivery.class);
        assertEquals(mock, builder.setStandUpdateDelivery(mock)
                                  .standUpdateDelivery().get());
    }

    @Test(expected = IllegalStateException.class)
    public void match_multitenance_state_of_BoundedContext_and_CommandBus_if_single_tenant() {
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setMultitenant(true)
                                                .setCommandStore(mock(CommandStore.class))
                                                .build();
        BoundedContext.newBuilder()
                       .setMultitenant(false)
                       .setCommandBus(commandBus)
                       .build();
    }
}
