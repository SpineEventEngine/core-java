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

package io.spine.server.commandbus;

import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.server.bus.BusBuilderShould;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.tenant.TenantAwareTest;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.test.Tests;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.spine.server.BoundedContext.newName;
import static io.spine.server.tenant.TenantAwareTest.createTenantIndex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("ConstantConditions")
public class CommandBusBuilderShould
        extends BusBuilderShould<CommandBus.Builder, CommandEnvelope, Command> {

    private CommandStore commandStore;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Override
    protected CommandBus.Builder builder() {
        return CommandBus.newBuilder();
    }

    @Before
    public void setUp() {
        final boolean multitenant = true;
        InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.newInstance(newName(getClass().getSimpleName()),
                                                   multitenant);
        TenantIndex tenantIndex = createTenantIndex(multitenant, storageFactory);
        commandStore = new CommandStore(storageFactory, tenantIndex);
    }

    @Test
    public void not_accept_null_CommandStore() {
        thrown.expect(NullPointerException.class);
        builder().setCommandStore(Tests.nullRef());
    }

    @Test
    public void not_allow_to_omit_setting_CommandStore() {
        thrown.expect(IllegalStateException.class);
        CommandBus.newBuilder().build();
    }

    @Test
    public void create_new_instance() {
        final CommandBus commandBus = builder().setCommandStore(commandStore)
                                               .build();
        assertNotNull(commandBus);
    }

    @Test
    public void allow_to_specify_command_scheduler() {
        final CommandScheduler expectedScheduler = mock(CommandScheduler.class);

        final CommandBus.Builder builder = builder().setCommandStore(commandStore)
                                                    .setCommandScheduler(expectedScheduler);

        assertTrue(builder.getCommandScheduler()
                          .isPresent());
        assertEquals(expectedScheduler, builder.getCommandScheduler()
                                               .get());

        final CommandBus commandBus = builder.build();
        assertNotNull(commandBus);

        final CommandScheduler actualScheduler = commandBus.scheduler();
        assertEquals(expectedScheduler, actualScheduler);
    }

    @Test
    public void allow_to_specify_rejeciton_bus() {
        final RejectionBus expectedRejectionBus = mock(RejectionBus.class);

        final CommandBus.Builder builder = builder().setCommandStore(commandStore)
                                                    .setRejectionBus(expectedRejectionBus);
        assertTrue(builder.getRejectionBus()
                          .isPresent());
        assertEquals(expectedRejectionBus, builder.getRejectionBus()
                                                  .get());
    }

    @Test
    public void specify_if_thread_spawn_allowed() {
        assertTrue(builder().setThreadSpawnAllowed(true)
                            .isThreadSpawnAllowed());

        assertFalse(CommandBus.newBuilder()
                              .setThreadSpawnAllowed(false)
                              .isThreadSpawnAllowed());
    }

    @Test
    public void verify_if_multitenant() {
        assertTrue(builder().setMultitenant(true)
                            .isMultitenant());
        assertFalse(builder().setMultitenant(false)
                             .isMultitenant());
    }

    @Test
    public void set_command_store() {
        final CommandStore commandStore = mock(CommandStore.class);

        assertEquals(commandStore, builder().setCommandStore(commandStore)
                                            .getCommandStore());
    }
}
