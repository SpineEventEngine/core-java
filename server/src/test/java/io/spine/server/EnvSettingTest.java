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

import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.server.EnvSetting.EnvType.PRODUCTION;
import static io.spine.server.EnvSetting.EnvType.TESTS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`EnvSetting` should")
@SuppressWarnings("DuplicateStringLiteralInspection")
class EnvSettingTest {

    enum UserDefinedEnv implements EnvSetting.EnvironmentType {
        STAGING
    }

    @Nested
    @DisplayName("not allow to configure a `null` value ")
    class NoNulls {

        @Test
        @DisplayName("for the `PRODUCTION` environment")
        void forProd() {
            testNoNullsForEnv(PRODUCTION);
        }

        @Test
        @DisplayName("for the `TESTS` environment")
        void forTests() {
            testNoNullsForEnv(TESTS);
        }

        @Test
        @DisplayName("for a user-defined environment")
        void forUserDefinedEnv() {
            testNoNullsForEnv(UserDefinedEnv.STAGING);

        }

        private void testNoNullsForEnv(EnvSetting.EnvironmentType tests) {
            EnvSetting<?> config = new EnvSetting();
            assertThrows(NullPointerException.class,
                         () -> config.configure(null, tests));
        }
    }

    @Nested
    @DisplayName("return a value")
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    class ReturnValue {

        @Test
        @DisplayName("for the `PRODUCTION` environment")
        void forProduction() {
            testReturnsForEnv(PRODUCTION);
        }

        @Test
        @DisplayName("for the `TESTS` environment")
        void forTests() {
            testReturnsForEnv(TESTS);
        }

        @Test
        @DisplayName("for a user-defined environment")
        void forUserDefinedEnv() {
            testReturnsForEnv(UserDefinedEnv.STAGING);

        }

        private void testReturnsForEnv(EnvSetting.EnvironmentType type) {
            InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            storageFactory.configure(factory, type);
            assertThat(storageFactory.value(type))
                    .isPresent();
            assertThat(storageFactory.value(type)
                                     .get()).isSameInstanceAs(factory);
        }
    }

    @Nested
    @DisplayName("when trying to assign a default value")
    class AssignDefault {

        @Test
        @DisplayName("assign for a missing `PRODUCTION` environment")
        void assignDefaultForProd() {
            testAssignsDefaultForEnv(PRODUCTION);
        }

        @Test
        @DisplayName("retain the original `PRODUCTION` env value")
        void retainForProd() {
            testRetainsDefaultForEnv(PRODUCTION);
        }

        @Test
        @DisplayName("assign for a missing `TESTS` env value")
        void assignDefaultForTests() {
            testRetainsDefaultForEnv(TESTS);
        }

        @Test
        @DisplayName("retain the original `TESTS` env value")
        void retainForTests() {
            testRetainsDefaultForEnv(TESTS);
        }

        @Test
        @DisplayName("assign for a missing user-defined env")
        void assignDefaultForUserDefined() {
            testRetainsDefaultForEnv(UserDefinedEnv.STAGING);
        }

        @Test
        @DisplayName("retain the original for a user-defined env")
        void retainForUserDefined() {
            testRetainsDefaultForEnv(UserDefinedEnv.STAGING);
        }

        void testAssignsDefaultForEnv(EnvSetting.EnvironmentType type) {
            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            MemoizingStorageFactory memoizingFactory = new MemoizingStorageFactory();
            assertThat(storageFactory.assignOrDefault(memoizingFactory, type))
                    .isSameInstanceAs(memoizingFactory);
        }

        void testRetainsDefaultForEnv(EnvSetting.EnvironmentType type) {
            EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
            MemoizingStorageFactory memoizingFactory = new MemoizingStorageFactory();
            storageFactory.configure(memoizingFactory, type);

            InMemoryStorageFactory inMemoryFactory = InMemoryStorageFactory.newInstance();
            assertThat(storageFactory.assignOrDefault(inMemoryFactory, type))
                    .isSameInstanceAs(memoizingFactory);

        }
    }

    @Test
    @DisplayName("reset the value for all environments")
    void resetTheValues() {
        InMemoryStorageFactory prodStorageFactory = InMemoryStorageFactory.newInstance();
        MemoizingStorageFactory testingStorageFactory = new MemoizingStorageFactory();
        InMemoryStorageFactory stagingStorageFactory = InMemoryStorageFactory.newInstance();

        EnvSetting<StorageFactory> storageFactory = new EnvSetting<>();
        storageFactory.configure(prodStorageFactory, PRODUCTION);
        storageFactory.configure(testingStorageFactory, TESTS);
        storageFactory.configure(stagingStorageFactory, UserDefinedEnv.STAGING);

        storageFactory.reset();

        Stream.of(PRODUCTION, TESTS, UserDefinedEnv.STAGING)
              .map(storageFactory::value)
              .forEach(s -> assertThat(s).isEmpty());
    }

    @Test
    @DisplayName("should run an operation against a value value if it's present")
    void runThrowableConsumer() throws Exception {
        MemoizingStorageFactory storageFactory = new MemoizingStorageFactory();

        EnvSetting<StorageFactory> storageSetting = new EnvSetting<>();

        storageSetting.configure(storageFactory, PRODUCTION);
        storageSetting.ifPresentForEnvironment(PRODUCTION, AutoCloseable::close);

        assertThat(storageFactory.isClosed()).isTrue();
    }
}
