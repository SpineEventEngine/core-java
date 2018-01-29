/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.bc;

import com.google.common.base.Supplier;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.event.EventBus;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.test.Tests;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
public class BoundedContextBuilderShould {

    private BoundedContext.Builder builder;

    @Before
    public void setUp() {
        final boolean multitenant = true;
        builder = BoundedContext.newBuilder()
                                .setMultitenant(multitenant);
    }

    @Test
    public void return_name_if_it_was_set() {
        final String nameString = getClass().getName();
        assertEquals(nameString, BoundedContext.newBuilder()
                                               .setName(nameString)
                                               .getName());
    }

    @Test
    public void return_storage_factory_supplier_if_it_was_set() {
        @SuppressWarnings("unchecked") // OK for this mock.
        Supplier<StorageFactory> mock = mock(Supplier.class);

        assertEquals(mock, builder.setStorageFactorySupplier(mock)
                                  .getStorageFactorySupplier()
                                  .get());
    }

    @Test
    public void allow_clearing_storage_factory_supplier() {
        assertFalse(builder.setStorageFactorySupplier(Tests.<Supplier<StorageFactory>>nullRef())
                           .getStorageFactorySupplier()
                           .isPresent());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_CommandDispatcher() {
        builder.setCommandBus(Tests.<CommandBus.Builder>nullRef());
    }

    @Test
    public void return_CommandBus_Builder() {
        final CommandBus.Builder expected = CommandBus.newBuilder();
        builder = BoundedContext.newBuilder()
                                .setCommandBus(expected);
        assertEquals(expected, builder.getCommandBus()
                                      .get());
    }

    @Test
    public void return_EventBus_builder() {
        final EventBus.Builder expected = EventBus.newBuilder();
        builder.setEventBus(expected);
        assertEquals(expected, builder.getEventBus()
                                      .get());
    }

    @Test
    public void return_IntegrationBus_builder() {
        final IntegrationBus.Builder expected = IntegrationBus.newBuilder();
        builder.setIntegrationBus(expected);
        assertEquals(expected, builder.getIntegrationBus()
                                      .get());
    }

    @Test
    public void support_multitenantcy() {
        builder.setMultitenant(true);
        assertTrue(builder.isMultitenant());
    }

    @Test
    public void be_single_tenant_by_default() {
        assertFalse(BoundedContext.newBuilder()
                                  .isMultitenant());
    }

    @Test
    public void allow_TenantIndex_configuration() {
        final TenantIndex tenantIndex = mock(TenantIndex.class);
        assertEquals(tenantIndex, BoundedContext.newBuilder()
                                                .setTenantIndex(tenantIndex)
                                                .getTenantIndex()
                                                .get());
    }

    @Test
    public void create_default_TenantIndex_if_not_configured() {
        assertNotNull(BoundedContext.newBuilder()
                                    .setMultitenant(true)
                                    .build()
                                    .getTenantIndex());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventBus() {
        builder.setEventBus(Tests.<EventBus.Builder>nullRef());
    }

    @Test
    public void create_CommandBus_if_it_was_not_set() {
        // Pass EventBus to builder initialization, and do NOT pass CommandBus.
        final BoundedContext boundedContext = builder
                .setEventBus(EventBus.newBuilder())
                .build();
        assertNotNull(boundedContext.getCommandBus());
    }

    @Test
    public void create_EventBus_if_it_was_not_set() {
        // Pass CommandBus.Builder to builder initialization, and do NOT pass EventBus.
        final BoundedContext boundedContext = builder
                .setMultitenant(true)
                .setCommandBus(CommandBus.newBuilder())
                .build();
        assertNotNull(boundedContext.getEventBus());
    }

    @Test
    public void create_both_CommandBus_and_EventBus_if_not_set() {
        final BoundedContext boundedContext = builder.build();
        assertNotNull(boundedContext.getCommandBus());
        assertNotNull(boundedContext.getEventBus());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_IntegrationBus() {
        builder.setIntegrationBus(Tests.<IntegrationBus.Builder>nullRef());
    }

    @Test(expected = IllegalStateException.class)
    public void match_multitenance_state_of_BoundedContext_and_CommandBus_if_single_tenant() {
        final CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                .setMultitenant(true)
                                                .setCommandStore(mock(CommandStore.class));
        BoundedContext.newBuilder()
                       .setMultitenant(false)
                       .setCommandBus(commandBus)
                       .build();
    }
}
