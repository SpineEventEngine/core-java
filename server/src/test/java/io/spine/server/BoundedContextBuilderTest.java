/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server;

import com.google.common.testing.NullPointerTester;
import io.spine.server.aggregate.AggregateRootDirectory;
import io.spine.server.bc.given.ProjectAggregate;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.testing.Tests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"OptionalGetWithoutIsPresent",
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("BoundedContext Builder should")
class BoundedContextBuilderTest {

    private BoundedContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = BoundedContext.newBuilder()
                                .setMultitenant(true);
    }

    @Test
    @DisplayName("not allow nulls")
    void notAcceptNulls() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(builder);
    }

    @Nested
    @DisplayName("return")
    class Return {

        @Test
        @DisplayName("CommandBus Builder")
        void commandBusBuilder() {
            CommandBus.Builder expected = CommandBus.newBuilder();
            builder = BoundedContext.newBuilder()
                                    .setCommandBus(expected);
            assertEquals(expected, builder.commandBus()
                                          .get());
        }

        @Test
        @DisplayName("EventBus Builder")
        void eventBusBuilder() {
            EventBus.Builder expected = EventBus.newBuilder();
            builder.setEventBus(expected);
            assertEquals(expected, builder.eventBus()
                                          .get());
        }

        @Test
        @DisplayName("name if it was set")
        void name() {
            String nameString = getClass().getName();
            assertEquals(nameString, BoundedContext.newBuilder()
                                                   .setName(nameString)
                                                   .name()
                                                   .getValue());
        }

        @Test
        @DisplayName("StorageFactory supplier if it was set")
        void storageFactorySupplier() {
            StorageFactory mock = mock(StorageFactory.class);
            assertEquals(mock, builder.setStorageFactorySupplier(() -> mock)
                                      .storageFactorySupplier()
                                      .get()
                                      .get());
        }

        @Test
        @DisplayName("TransportFactory if it was set")
        void transportFactory() {
            TransportFactory factory = InMemoryTransportFactory.newInstance();

            assertEquals(factory, builder.setTransportFactory(factory)
                                         .transportFactory()
                                         .get());
        }

        @Test
        @DisplayName("AggregateRootDirectory if it was set")
        void aggregateRootDirectory() {
            AggregateRootDirectory directory = mock(AggregateRootDirectory.class);
            builder.setAggregateRootDirectory(() -> directory);
            assertEquals(directory, builder.aggregateRootDirectory());
        }
    }

    @Test
    @DisplayName("allow clearing storage factory supplier")
    void clearStorageFactorySupplier() {
        assertFalse(builder.setStorageFactorySupplier(Tests.nullRef())
                           .storageFactorySupplier()
                           .isPresent());
    }

    @Nested
    @DisplayName("if not given custom, create default")
    class CreateDefault {

        @Test
        @DisplayName("TenantIndex")
        void tenantIndex() {
            assertNotNull(BoundedContext.newBuilder()
                                        .setMultitenant(true)
                                        .build()
                                        .tenantIndex());
        }

        @Test
        @DisplayName("CommandBus")
        void commandBus() {
            // Pass EventBus to builder initialization, and do NOT pass CommandBus.
            BoundedContext boundedContext = builder.setEventBus(EventBus.newBuilder())
                                                   .build();
            assertNotNull(boundedContext.commandBus());
        }

        @Test
        @DisplayName("EventBus")
        void eventBus() {
            // Pass CommandBus.Builder to builder initialization, and do NOT pass EventBus.
            BoundedContext boundedContext = builder.setMultitenant(true)
                                                   .setCommandBus(CommandBus.newBuilder())
                                                   .build();
            assertNotNull(boundedContext.eventBus());
        }

        @Test
        @DisplayName("CommandBus and EventBus simultaneously")
        void commandBusAndEventBus() {
            BoundedContext boundedContext = builder.build();
            assertNotNull(boundedContext.commandBus());
            assertNotNull(boundedContext.eventBus());
        }

        @Test
        @DisplayName("AggregateRootDirectory")
        void aggregateRootDirectory() {
            BoundedContext boundedContext = builder.build();
            assertNotNull(boundedContext.aggregateRootDirectory());
        }
    }

    @Test
    @DisplayName("be single tenant by default")
    void beSingleTenantByDefault() {
        assertFalse(BoundedContext.newBuilder()
                                  .isMultitenant());
    }

    @Test
    @DisplayName("support multitenancy")
    void supportMultitenancy() {
        builder.setMultitenant(true);
        assertTrue(builder.isMultitenant());
    }

    @Test
    @DisplayName("allow TenantIndex configuration")
    void setTenantIndex() {
        TenantIndex tenantIndex = mock(TenantIndex.class);
        assertEquals(tenantIndex, BoundedContext.newBuilder()
                                                .setTenantIndex(tenantIndex)
                                                .tenantIndex()
                                                .get());
    }

    @Test
    @DisplayName("not accept CommandBus with different multitenancy state")
    void matchCommandBusMultitenancy() {
        CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                  .setMultitenant(true);
        assertThrows(IllegalStateException.class, () -> BoundedContext.newBuilder()
                                                                      .setMultitenant(false)
                                                                      .setCommandBus(commandBus)
                                                                      .build());
    }

    @Nested
    class Repositories {

        private Repository<?, ?> repository;
        private BoundedContextBuilder builder;

        @BeforeEach
        void setUp() {
            this.builder = BoundedContext.newBuilder();
            repository = DefaultRepository.of(ProjectAggregate.class);
        }

        @Test
        @DisplayName("add repository")
        void addRepo() {
            assertFalse(builder.hasRepository(repository));

            builder.add(repository);

            assertTrue(builder.hasRepository(repository));
        }

        @Test
        @DisplayName("remove repository")
        void removeRepo() {
            builder.add(repository);
            assertTrue(builder.hasRepository(repository));

            builder.remove(repository);
            assertFalse(builder.hasRepository(repository));
        }
    }
}
