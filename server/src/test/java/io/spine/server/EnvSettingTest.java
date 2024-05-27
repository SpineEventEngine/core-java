/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.environment.DefaultMode;
import io.spine.environment.Environment;
import io.spine.environment.EnvironmentType;
import io.spine.environment.Tests;
import io.spine.server.given.environment.Local;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.spine.testing.Assertions.assertNpe;
import static io.spine.testing.Tests.nullRef;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@DisplayName("`EnvSetting` should")
@SuppressWarnings("DuplicateStringLiteralInspection")
class EnvSettingTest {

    @Nested
    @DisplayName("not allow to configure a `null` value ")
    class NoNulls {

        @Test
        @DisplayName("for the `DefaultMode` environment")
        void forDefault() {
            testNoNullsForEnv(DefaultMode.class);
        }

        @Test
        @DisplayName("for the `Tests` environment")
        void forTests() {
            testNoNullsForEnv(Tests.class);
        }

        @Test
        @DisplayName("for a user-defined environment")
        void forUserDefined() {
            testNoNullsForEnv(Local.class);
        }

        @Test
        @DisplayName("using the `null` as the env value")
        @SuppressWarnings("ThrowableNotThrown")
        void forEnv() {
            EnvSetting<StorageFactory> setting = new EnvSetting<>();
            assertNpe(() -> setting.use(InMemoryStorageFactory.newInstance(), nullRef()));
        }

        @SuppressWarnings("ThrowableNotThrown")
        private void testNoNullsForEnv(Class<? extends EnvironmentType<?>> envType) {
            EnvSetting<?> setting = new EnvSetting<Void>();
            assertNpe(() -> setting.use(nullRef(), envType));
            assertNpe(() -> setting.lazyUse(nullRef(), envType));
        }
    }

    @Nested
    @DisplayName("return a value")
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    class ReturnValue {

        @Test
        @DisplayName("for the `DefaultMode` environment")
        void forDefault() {
            testReturnsForEnv(DefaultMode.class);
        }

        @Test
        @DisplayName("for the `Tests` environment")
        void forTests() {
            testReturnsForEnv(Tests.class);
        }

        @Test
        @DisplayName("for a user-defined environment type")
        void forUserDefinedType() {
            testReturnsForEnv(Local.class);
        }

        @Test
        @DisplayName("for the current environment type")
        void forCurrentEnv() {
            Environment environment = Environment.instance();
            InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            storageFactory.use(factory, environment.type());
            assertThat(storageFactory.value()).isSameInstanceAs(factory);
        }

        private void testReturnsForEnv(Class<? extends EnvironmentType<?>> type) {
            InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            storageFactory.use(factory, type);
            assertThat(storageFactory.optionalValue(type))
                    .isPresent();
            assertThat(storageFactory.optionalValue(type)
                                     .get()).isSameInstanceAs(factory);
        }

        @Nested
        @DisplayName("when trying to assign a default value")
        class AssignDefault {

            @Test
            @DisplayName("assign for a missing `DefaultMode` environment")
            void assignDefaultForDefaultMode() {
                testAssignsDefaultForEnv(DefaultMode.class);
            }

            @Test
            @DisplayName("retain the original `DefaultMode` env value")
            void retainForDefaultMode() {
                testRetainsDefaultForEnv(DefaultMode.class);
            }

            @Test
            @DisplayName("assign for a missing custom environment")
            void assignDefaultForCustom() {
                testAssignsDefaultForEnv(Local.class);
            }

            @Test
            @DisplayName("retain the original value for a custom environment")
            void retainForCustom() {
                testRetainsDefaultForEnv(Local.class);
            }

            @Test
            @DisplayName("assign for a missing `Tests` env value")
            void assignDefaultForTests() {
                testRetainsDefaultForEnv(Tests.class);
            }

            @Test
            @DisplayName("retain the original `Tests` env value")
            void retainForTests() {
                testRetainsDefaultForEnv(Tests.class);
            }

            void testAssignsDefaultForEnv(Class<? extends EnvironmentType<?>> type) {
                StorageFactory memoizingFactory = new MemoizingStorageFactory();
                EnvSetting<StorageFactory> storageFactory =
                        new EnvSetting<>(type, () -> memoizingFactory);
                assertThat(storageFactory.value(type))
                        .isSameInstanceAs(memoizingFactory);
            }

            void testRetainsDefaultForEnv(Class<? extends EnvironmentType<?>> type) {
                StorageFactory defaultFactory = new MemoizingStorageFactory();
                EnvSetting<StorageFactory> storageFactory =
                        new EnvSetting<>(type, () -> defaultFactory);

                StorageFactory actualFactory = new MemoizingStorageFactory();
                storageFactory.use(actualFactory, type);

                assertThat(storageFactory.value(type)).isSameInstanceAs(actualFactory);
            }
        }
    }

    @Nested
    @DisplayName("allow to configure the values lazily")
    class Lazy {

        @Test
        @DisplayName("for the `DefaultMode` environment")
        void forDefault() {
            testLazyForEnv(DefaultMode.class);
        }

        @Test
        @DisplayName("for the `Tests` environment")
        void forTests() {
            testLazyForEnv(Tests.class);
        }

        @Test
        @DisplayName("for a user-defined environment type")
        void forUserDefinedType() {
            testLazyForEnv(Local.class);
        }

        private void testLazyForEnv(Class<? extends EnvironmentType<?>> type) {
            InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            AtomicBoolean resolved = new AtomicBoolean(false);
            storageFactory.lazyUse(() -> {
                resolved.set(true);
                return factory;
            }, type);
            assertThat(resolved.get()).isFalse();

            Optional<StorageFactory> actual = storageFactory.optionalValue(type);
            assertThat(actual)
                    .isPresent();
            assertThat(resolved.get()).isTrue();
            assertThat(actual.get()).isSameInstanceAs(factory);
        }
    }

    @Nested
    @DisplayName("be thread-safe")
    class ThreadSafety {

        private ExecutorService readWriteExecutors;
        private CountDownLatch latch;

        @BeforeEach
        void setUp() {
            readWriteExecutors = Executors.newFixedThreadPool(3);
            latch = new CountDownLatch(1);
        }

        @Test
        @DisplayName("allowing multiple threads to read simultaneously " +
                "without affecting the stored value")
        void testReadOperations() {
            EnvSetting<UUID> setting = new EnvSetting<>();
            UUID initialValue = randomUUID();
            setting.use(initialValue, Local.class);

            Future<?> readBlockingFuture = runBlockingReadOperation(setting, initialValue);
            sleepUninterruptibly(100, MILLISECONDS);

            UUID actualValue = setting.value(Local.class);
            assertThat(actualValue).isEqualTo(initialValue);

            latch.countDown();
            awaitFutureCompletion(readBlockingFuture);
        }

        @Test
        @DisplayName("allowing a write operation to holds exclusive access, " +
                "blocking concurrent reads and writes until complete")
        void testWriteOperations() {
            EnvSetting<UUID> setting = new EnvSetting<>();
            UUID initialValue = randomUUID();

            Future<?> writeBlockingFuture = runBlockingWriteOperation(setting, initialValue);
            sleepUninterruptibly(100, MILLISECONDS);

            UUID rewrittenValue = randomUUID();
            Future<?> writeFuture = runVerifyingWriteOperation(setting,
                                                               rewrittenValue,
                                                               initialValue);
            sleepUninterruptibly(100, MILLISECONDS);

            latch.countDown();
            awaitFutureCompletion(writeFuture);
            awaitFutureCompletion(writeBlockingFuture);

            assertThat(setting.value(Local.class)).isEqualTo(rewrittenValue);
        }

        @AfterEach
        void tearDown() {
            try {
                readWriteExecutors.shutdownNow();
                readWriteExecutors.awaitTermination(500, MILLISECONDS);
            } catch (InterruptedException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        /**
         * Submits a blocking read operation to the given setting and verifies the retrieved value.
         *
         * @param setting
         *         the environment setting to read from
         * @param expectedValue
         *         the expected value to verify the result of the read operation
         * @param <T>
         *         the type of value in the environment setting
         */
        private <T> Future<?> runBlockingReadOperation(EnvSetting<T> setting, T expectedValue) {
            return readWriteExecutors.submit(() -> {
                Optional<T> actualValue = setting.valueFor(() -> {
                    awaitUninterruptibly(latch);
                    return Local.class;
                });
                assertThat(actualValue).isPresent();
                assertThat(actualValue.get()).isEqualTo(expectedValue);
            });
        }

        /**
         * Submits a blocking write operation to the given setting.
         *
         * @param setting
         *         the environment setting to write to
         * @param valueToSet
         *         the value to set
         * @param <T>
         *         the type of value in the environment setting
         */
        private <T> Future<?> runBlockingWriteOperation(EnvSetting<T> setting, T valueToSet) {
            return readWriteExecutors.submit(() -> {
                setting.useViaInit(() -> {
                    awaitUninterruptibly(latch);
                    return valueToSet;
                }, Local.class);
            });
        }

        /**
         * Submits a non-blocking write operation to the provided setting and verifies
         * the current value in the given setting before its update.
         *
         * @param setting
         *         the environment setting to write to
         * @param valueToSet
         *         the new value to set
         * @param currentValue
         *         the expected current value to verify before updating
         * @param <T>
         *         the type of value in the environment setting
         */
        private <T> Future<?> runVerifyingWriteOperation(EnvSetting<T> setting,
                                                         T valueToSet,
                                                         T currentValue) {
            return readWriteExecutors.submit(() -> {
                assertThat(setting.value(Local.class)).isEqualTo(currentValue);
                setting.useViaInit(() -> valueToSet, Local.class);
            });
        }

        private void awaitFutureCompletion(Future<?> future) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw illegalStateWithCauseOf(e);
            }
        }
    }

    @Test
    @DisplayName("reset the value for all environments")
    void resetTheValues() {
        InMemoryStorageFactory defaultStorageFactory = InMemoryStorageFactory.newInstance();
        StorageFactory testingStorageFactory = new MemoizingStorageFactory();
        InMemoryStorageFactory localStorageFactory = InMemoryStorageFactory.newInstance();

        EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
        storageFactory.use(defaultStorageFactory, DefaultMode.class);
        storageFactory.use(testingStorageFactory, Tests.class);
        storageFactory.use(localStorageFactory, Local.class);

        storageFactory.reset();

        Stream.of(DefaultMode.class, Tests.class, Local.class)
              .map(storageFactory::optionalValue)
              .forEach(s -> assertThat(s).isEmpty());
    }

    @Test
    @DisplayName("run an operation against a value if it's present")
    void runThrowableConsumer() throws Exception {
        MemoizingStorageFactory storageFactory = new MemoizingStorageFactory();

        EnvSetting<StorageFactory> storageSetting = new EnvSetting<>();

        storageSetting.use(storageFactory, DefaultMode.class);
        storageSetting.ifPresentForEnvironment(DefaultMode.class, AutoCloseable::close);

        assertThat(storageFactory.isClosed()).isTrue();
    }

    @Test
    @DisplayName("run an operation for all present values")
    void runOperationForAll() throws Exception {
        MemoizingStorageFactory defaultStorageFactory = new MemoizingStorageFactory();
        MemoizingStorageFactory testingStorageFactory = new MemoizingStorageFactory();
        MemoizingStorageFactory localStorageFactory = new MemoizingStorageFactory();

        EnvSetting<StorageFactory> storageSetting = new EnvSetting<>();

        storageSetting.use(defaultStorageFactory, DefaultMode.class);
        storageSetting.use(testingStorageFactory, Tests.class);
        storageSetting.use(localStorageFactory, Local.class);

        storageSetting.apply(AutoCloseable::close);

        assertThat(defaultStorageFactory.isClosed()).isTrue();
        assertThat(testingStorageFactory.isClosed()).isTrue();
        assertThat(localStorageFactory.isClosed()).isTrue();
    }
}
