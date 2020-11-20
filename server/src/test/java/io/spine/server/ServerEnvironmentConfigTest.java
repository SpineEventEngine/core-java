/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.truth.Truth8;
import io.spine.base.Environment;
import io.spine.base.EnvironmentType;
import io.spine.base.Production;
import io.spine.base.Tests;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.UniformAcrossAllShards;
import io.spine.server.given.environment.Local;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.SystemAwareStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import io.spine.server.trace.given.MemoizingTracerFactory;
import io.spine.server.transport.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.ServerEnvironment.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests configuration of “features” of {@link ServerEnvironment}.
 *
 * @see ServerEnvironmentTest
 */
@DisplayName("`ServerEnvironment` should allow configuration of")
class ServerEnvironmentConfigTest {

    private static final Environment environment = Environment.instance();
    private static final ServerEnvironment serverEnvironment = ServerEnvironment.instance();

    @AfterEach
    void resetAndBackToTests() {
        serverEnvironment.reset();
        environment.reset();
        environment.setTo(Tests.class);
    }

    @Nested
    @DisplayName("`TransportFactory` for")
    class OfTransportFactory {

        @Nested
        @DisplayName("`Production`")
        class ForProd {

            @BeforeEach
            void turnToProduction() {
                environment.setTo(Production.class);
            }

            @Test
            @DisplayName("throw an `IllegalStateException` if not configured")
            void throwsIfNotConfigured() {
                assertThrows(IllegalStateException.class, serverEnvironment::transportFactory);
            }

            @Test
            @DisplayName("return configured instance in Production")
            void productionValue() {
                TransportFactory factory = new StubTransportFactory();
                when(Production.class).use(factory);
                assertThat(serverEnvironment.transportFactory())
                        .isEqualTo(factory);
            }
        }

        @Nested
        @DisplayName("`Tests`")
        class ForTests {

            @Test
            @DisplayName("returning explicitly set value")
            void setExplicitly() {
                TransportFactory factory = new StubTransportFactory();
                when(Tests.class).use(factory);
                assertThat(serverEnvironment.transportFactory())
                        .isSameInstanceAs(factory);
            }

            @Test
            @DisplayName("returning an `InMemoryTransportFactory` when not set")
            void notSet() {
                environment.setTo(Tests.class);
                assertThat(serverEnvironment.transportFactory())
                        .isInstanceOf(InMemoryTransportFactory.class);
            }
        }

        @Nested
        @DisplayName("custom environment")
        class ForCustom {

            @BeforeEach
            void setCustom() {
                environment.setTo(Local.class);
                Local.enable();
            }

            @Test
            @DisplayName("throwing an `IllegalStateException` if not set")
            void illegalState() {
                assertThrows(IllegalStateException.class, serverEnvironment::transportFactory);
            }

            @Test
            @DisplayName("returning a configured instance")
            void ok() {
                InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
                when(Local.class)
                                 .use(transportFactory);
                assertThat(serverEnvironment.transportFactory())
                        .isSameInstanceAs(transportFactory);
            }
        }
    }

    @Nested
    @DisplayName("`Delivery`")
    class DeliveryTest {

        @Test
        @DisplayName("to default back to `Local` if no delivery is set")
        void backToLocal() {
            Delivery delivery = serverEnvironment.delivery();
            assertThat(delivery).isNotNull();
        }

        @Test
        @DisplayName("to a custom mechanism")
        void allowToCustomizeDeliveryStrategy() {
            Delivery newDelivery = Delivery.newBuilder()
                                           .setStrategy(UniformAcrossAllShards.forNumber(42))
                                           .build();
            Delivery defaultValue = serverEnvironment.delivery();
            Class<? extends EnvironmentType> currentType = environment.type();

            when(currentType).use(newDelivery);

            assertThat(serverEnvironment.delivery())
                    .isSameInstanceAs(newDelivery);

            // Restore the default value.
            when(currentType)
                             .use(defaultValue);
        }
    }

    @Nested
    @DisplayName("`TracerFactory`")
    class TracerFactory {

        @Test
        @DisplayName("returning an instance for the production environment")
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        void forProduction() {
            environment.setTo(Production.class);

            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            when(Production.class).use(memoizingTracer);

            assertThat(serverEnvironment.tracing()
                                        .get()).isSameInstanceAs(memoizingTracer);
        }

        @Test
        @DisplayName("for the testing environment")
        void forTesting() {
            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            when(Tests.class)
                             .use(memoizingTracer);

            Truth8.assertThat(serverEnvironment.tracing())
                  .hasValue(memoizingTracer);
        }

        @Test
        @DisplayName("for a custom environment")
        void forCustom() {
            environment.setTo(Local.class);

            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            when(Local.class).use(memoizingTracer);

            Truth8.assertThat(serverEnvironment.tracing())
                  .hasValue(memoizingTracer);
        }

        @Test
        @DisplayName("for a custom environment, returning empty if it's disabled")
        void forCustomEmpty() {
            Local.disable();

            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            when(Local.class).use(memoizingTracer);

            Truth8.assertThat(serverEnvironment.tracing())
                  .isEmpty();
        }
    }

    @Nested
    @DisplayName("`StorageFactory` for")
    class OfStorageFactory {

        @Nested
        @DisplayName("`Production`")
        class ForProduction {

            @BeforeEach
            void turnToProduction() {
                environment.setTo(Production.class);
            }

            @Test
            @DisplayName("throwing an `IllegalStateException` if not configured")
            void throwsIfNotConfigured() {
                assertThrows(IllegalStateException.class, serverEnvironment::storageFactory);
            }

            @Test
            @DisplayName("return configured `StorageFactory`")
            void productionFactory() {
                StorageFactory factory = InMemoryStorageFactory.newInstance();
                when(Production.class)
                                 .use(factory);
                StorageFactory delegate =
                        ((SystemAwareStorageFactory) serverEnvironment.storageFactory()).delegate();
                assertThat(delegate)
                        .isEqualTo(factory);
            }

            @Test
            @DisplayName("return `InMemoryStorageFactory` under Tests")
            void testsFactory() {
                environment.setTo(Tests.class);

                StorageFactory factory = serverEnvironment.storageFactory();
                assertThat(factory)
                        .isInstanceOf(SystemAwareStorageFactory.class);
                SystemAwareStorageFactory systemAware = (SystemAwareStorageFactory) factory;
                assertThat(systemAware.delegate()).isInstanceOf(InMemoryStorageFactory.class);
            }
        }

        @Nested
        @DisplayName("`Tests`")
        class ForTests {

            @Test
            @DisplayName("wrapping `SystemAwareStorageFactory` over the passed instance")
            void getSet() {
                StorageFactory factory = new MemoizingStorageFactory();
                when(Tests.class)
                                 .use(factory);

                StorageFactory delegate =
                        ((SystemAwareStorageFactory) serverEnvironment.storageFactory()).delegate();
                assertThat(delegate)
                        .isEqualTo(factory);
            }
        }

        @Nested
        @DisplayName("a custom environment")
        class ForCustom {

            @BeforeEach
            void reset() {
                environment.setTo(Local.class);
                Local.enable();
            }

            @Test
            @DisplayName("throwing an `IllegalStateException` if not set")
            void illegalState() {
                assertThrows(IllegalStateException.class, serverEnvironment::storageFactory);
            }

            @Test
            @DisplayName("returning a configured wrapped instance")
            void returnConfiguredStorageFactory() {
                InMemoryStorageFactory inMemory = InMemoryStorageFactory.newInstance();
                when(Local.class)
                                 .use(inMemory);

                StorageFactory delegate =
                        ((SystemAwareStorageFactory) serverEnvironment.storageFactory()).delegate();
                assertThat(delegate)
                        .isEqualTo(inMemory);
            }
        }
    }

    /**
     * Stub implementation of {@code TransportFactory} which delegates all the calls
     * to {@code InMemoryTransportFactory}.
     */
    static class StubTransportFactory implements TransportFactory {

        private final TransportFactory delegate = InMemoryTransportFactory.newInstance();

        @Override
        public Publisher createPublisher(ChannelId id) {
            return delegate.createPublisher(id);
        }

        @Override
        public Subscriber createSubscriber(ChannelId id) {
            return delegate.createSubscriber(id);
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }
}
