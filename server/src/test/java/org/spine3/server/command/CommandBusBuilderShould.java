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

package org.spine3.server.command;

import org.junit.Before;
import org.junit.Test;
import org.spine3.server.failure.FailureBus;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.server.tenant.TenantAwareTest;
import org.spine3.server.tenant.TenantIndex;
import org.spine3.test.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ConstantConditions")
public class CommandBusBuilderShould {

    private CommandStore commandStore;

    @Before
    public void setUp() {
        final boolean multitenant = true;
        final InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.getInstance(multitenant);
        final TenantIndex tenantIndex =
                TenantAwareTest.createTenantIndex(multitenant, storageFactory);
        commandStore = new CommandStore(storageFactory, tenantIndex);
    }

    @Test(expected = NullPointerException.class)
    public void not_accept_null_CommandStore() {
        CommandBus.newBuilder()
                  .setCommandStore(Tests.<CommandStore>nullRef());
    }

    @Test(expected = IllegalStateException.class)
    public void not_allow_to_omit_setting_CommandStore() {
        CommandBus.newBuilder()
                  .build();
    }

    @Test
    public void create_new_instance() {
        final CommandBus commandBus = CommandBus.newBuilder()
                                                .setCommandStore(commandStore)
                                                .build();
        assertNotNull(commandBus);
    }

    @Test
    public void allow_to_specify_command_scheduler() {
        final CommandScheduler expectedScheduler = mock(CommandScheduler.class);

        final CommandBus.Builder builder = CommandBus.newBuilder()
                                                     .setCommandStore(commandStore)
                                                     .setCommandScheduler(expectedScheduler);

        assertEquals(expectedScheduler, builder.getCommandScheduler()
                                               .get());

        final CommandBus commandBus = builder.build();
        assertNotNull(commandBus);

        final CommandScheduler actualScheduler = commandBus.scheduler();
        assertEquals(expectedScheduler, actualScheduler);
    }

    @Test
    public void allow_to_specify_failure_bus() {
        final FailureBus expectedFailureBus = mock(FailureBus.class);

        final CommandBus.Builder builder = CommandBus.newBuilder()
                                                     .setCommandStore(commandStore)
                                                     .setFailureBus(expectedFailureBus);
        assertEquals(expectedFailureBus, builder.getFailureBus()
                                                .get());
    }

    @Test
    public void specify_if_thread_spawn_allowed() {
        assertTrue(CommandBus.newBuilder()
                             .setThreadSpawnAllowed(true)
                             .isThreadSpawnAllowed());

        assertFalse(CommandBus.newBuilder()
                              .setThreadSpawnAllowed(false)
                              .isThreadSpawnAllowed());
    }

    @Test
    public void verify_if_multitenant() {
        assertTrue(CommandBus.newBuilder()
                             .setMultitenant(true)
                             .isMultitenant());
        assertFalse(CommandBus.newBuilder()
                              .setMultitenant(false)
                              .isMultitenant());
    }

    @Test
    public void set_command_store() {
        final CommandStore commandStore = mock(CommandStore.class);

        assertEquals(commandStore, CommandBus.newBuilder()
                                             .setCommandStore(commandStore)
                                             .getCommandStore());
    }

    @Test
    public void allow_adding_filter() {
        final CommandBusFilter filter = mock(CommandBusFilter.class);

        assertTrue(CommandBus.newBuilder()
                             .addFilter(filter)
                             .getFilters()
                             .contains(filter));
    }

    @Test
    public void allow_removing_filter() {
        final CommandBusFilter filter = mock(CommandBusFilter.class);

        assertFalse(CommandBus.newBuilder()
                              .addFilter(filter)
                              .removeFilter(filter)
                              .getFilters()
                              .contains(filter));
    }
}
