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

import io.spine.base.EnvironmentType;
import io.spine.base.Production;
import io.spine.base.Tests;
import io.spine.server.given.environment.Local;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`EnvSetting` should")
@SuppressWarnings("DuplicateStringLiteralInspection")
class EnvSettingTest {

    @Nested
    @DisplayName("not allow to configure a `null` value ")
    class NoNulls {

        @Test
        @DisplayName("for the `Production` environment")
        void forProd() {
            testNoNullsForEnv(Production.class);
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
        void forEnv() {
            EnvSetting<StorageFactory> setting = new EnvSetting<>();
            assertThrows(NullPointerException.class,
                         () -> setting.use(InMemoryStorageFactory.newInstance(), null));
        }

        private void testNoNullsForEnv(Class<? extends EnvironmentType> envType) {
            EnvSetting<?> config = new EnvSetting();
            assertThrows(NullPointerException.class,
                         () -> config.use(null, envType));
        }
    }

    @Nested
    @DisplayName("return a value")
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    class ReturnValue {

        @Test
        @DisplayName("for the `Production` environment")
        void forProduction() {
            testReturnsForEnv(Production.class);
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

        private void testReturnsForEnv(Class<? extends EnvironmentType> type) {
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
            @DisplayName("assign for a missing `Production` environment")
            void assignDefaultForProd() {
                testAssignsDefaultForEnv(Production.class);
            }

            @Test
            @DisplayName("retain the original `Production` env value")
            void retainForProd() {
                testRetainsDefaultForEnv(Production.class);
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

            void testAssignsDefaultForEnv(Class<? extends EnvironmentType> type) {
                MemoizingStorageFactory memoizingFactory = new MemoizingStorageFactory();
                EnvSetting<StorageFactory> storageFactory =
                        new EnvSetting<>(type, () -> memoizingFactory);
                assertThat(storageFactory.value(type))
                        .isSameInstanceAs(memoizingFactory);
            }

            void testRetainsDefaultForEnv(Class<? extends EnvironmentType> type) {
                MemoizingStorageFactory defaultFactory = new MemoizingStorageFactory();
                EnvSetting<StorageFactory> storageFactory =
                        new EnvSetting<>(type, () -> defaultFactory);

                MemoizingStorageFactory actualFactory = new MemoizingStorageFactory();
                storageFactory.use(actualFactory, type);

                assertThat(storageFactory.value(type)).isSameInstanceAs(actualFactory);
            }
        }

        @Test
        @DisplayName("reset the value for all environments")
        void resetTheValues() {
            InMemoryStorageFactory prodStorageFactory = InMemoryStorageFactory.newInstance();
            MemoizingStorageFactory testingStorageFactory = new MemoizingStorageFactory();
            InMemoryStorageFactory localstorageFactory = InMemoryStorageFactory.newInstance();

            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            storageFactory.use(prodStorageFactory, Production.class);
            storageFactory.use(testingStorageFactory, Tests.class);
            storageFactory.use(localstorageFactory, Local.class);

            storageFactory.reset();

            Stream.of(Production.class, Tests.class, Local.class)
                  .map(storageFactory::optionalValue)
                  .forEach(s -> assertThat(s).isEmpty());
        }

        @Test
        @DisplayName("should run an operation against a value value if it's present")
        void runThrowableConsumer() throws Exception {
            MemoizingStorageFactory storageFactory = new MemoizingStorageFactory();

            EnvSetting<StorageFactory> storageSetting = new EnvSetting<>();

            storageSetting.use(storageFactory, Production.class);
            storageSetting.ifPresentForEnvironment(Production.class, AutoCloseable::close);

            assertThat(storageFactory.isClosed()).isTrue();
        }
    }
}
