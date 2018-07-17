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
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.commandstore.CommandStore;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.testing.Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.BoundedContext.newName;
import static io.spine.server.tenant.TenantAwareTest.createTenantIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("CommandBus Builder should")
class CommandBusBuilderTest
        extends BusBuilderTest<CommandBus.Builder, CommandEnvelope, Command> {

    private CommandStore commandStore;

    @Override
    protected CommandBus.Builder builder() {
        return CommandBus.newBuilder();
    }

    @BeforeEach
    void setUp() {
        final boolean multitenant = true;
        InMemoryStorageFactory storageFactory =
                InMemoryStorageFactory.newInstance(newName(getClass().getSimpleName()),
                                                   multitenant);
        TenantIndex tenantIndex = createTenantIndex(multitenant, storageFactory);
        commandStore = new CommandStore(storageFactory, tenantIndex);
    }

    @Test
    @DisplayName("create new CommandBus instance")
    void createNewInstance() {
        final CommandBus commandBus = builder().setCommandStore(commandStore)
                                               .build();
        assertNotNull(commandBus);
    }

    @Test
    @DisplayName("not accept null CommandStore")
    void notAcceptNullCommandStore() {
        assertThrows(NullPointerException.class, () -> builder().setCommandStore(Tests.nullRef()));
    }

    @Test
    @DisplayName("not allow to omit setting CommandStore")
    void neverOmitCommandStore() {
        assertThrows(IllegalStateException.class, () -> CommandBus.newBuilder()
                                                                  .build());
    }

    @Nested
    @DisplayName("allow to specify")
    class AllowToSpecify {

        @Test
        @DisplayName("CommandScheduler")
        void commandScheduler() {
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
        @DisplayName("RejectionBus")
        void rejectionBus() {
            final RejectionBus expectedRejectionBus = mock(RejectionBus.class);

            final CommandBus.Builder builder = builder().setCommandStore(commandStore)
                                                        .setRejectionBus(expectedRejectionBus);
            assertTrue(builder.getRejectionBus()
                              .isPresent());
            assertEquals(expectedRejectionBus, builder.getRejectionBus()
                                                      .get());
        }

        @Test
        @DisplayName("if thread spawn is allowed")
        void ifThreadSpawnAllowed() {
            assertTrue(builder().setThreadSpawnAllowed(true)
                                .isThreadSpawnAllowed());

            assertFalse(CommandBus.newBuilder()
                                  .setThreadSpawnAllowed(false)
                                  .isThreadSpawnAllowed());
        }

        @Test
        @DisplayName("if CommandBus is multitenant")
        void ifIsMultitenant() {
            assertTrue(builder().setMultitenant(true)
                                .isMultitenant());
            assertFalse(builder().setMultitenant(false)
                                 .isMultitenant());
        }

        @Test
        @DisplayName("CommandStore")
        void commandStore() {
            final CommandStore commandStore = mock(CommandStore.class);

            assertEquals(commandStore, builder().setCommandStore(commandStore)
                                                .getCommandStore());
        }
    }
}
