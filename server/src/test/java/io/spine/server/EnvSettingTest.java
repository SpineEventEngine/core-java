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
import io.spine.server.storage.system.SystemAwareStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`EnvSetting` should")
class EnvSettingTest {

    private static final UnaryOperator<StorageFactory> WRAPPER_FUNCTION = SystemAwareStorageFactory::wrap;

    @Test
    @DisplayName("not allows to configure a `null` value")
    void nullsForProductionForbidden() {
        EnvSetting<?> config = EnvSetting
                .<String>newBuilder()
                .build();
        assertThrows(NullPointerException.class, () -> config.configure(null));
    }

    @Test
    @DisplayName("return a wrapped production value")
    void wrapProduction() {
        EnvSetting<StorageFactory> storageFactory = EnvSetting
                .<StorageFactory>newBuilder()
                .wrapProductionValue(WRAPPER_FUNCTION)
                .build();
        storageFactory.configure(InMemoryStorageFactory.newInstance())
                      .forProduction();
        assertProductionMatches(storageFactory, s -> s instanceof SystemAwareStorageFactory);
    }

    @Test
    @DisplayName("return a wrapped test value")
    void wrapTests() {
        EnvSetting<StorageFactory> storageFactory = EnvSetting
                .<StorageFactory>newBuilder()
                .wrapTestValue(WRAPPER_FUNCTION)
                .build();
        storageFactory.configure(InMemoryStorageFactory.newInstance())
                      .forTests();
        assertTestsMatches(storageFactory, s -> s instanceof SystemAwareStorageFactory);
    }

    @Test
    @DisplayName("return an unwrapped production value if no wrapping function was specified")
    void returnWhenNotWrappedProduction() {
        InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
        EnvSetting<StorageFactory> storageFactory = EnvSetting
                .<StorageFactory>newBuilder()
                .build();
        storageFactory.configure(factory)
                      .forProduction();
        assertProductionMatches(storageFactory, s -> s == factory);
    }

    @Test
    @DisplayName("return an unwrapped test value if no wrapping function was specified")
    void returnWhenNotWrappedTests() {
        InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
        EnvSetting<StorageFactory> storageFactory = EnvSetting
                .<StorageFactory>newBuilder()
                .build();
        storageFactory.configure(factory)
                      .forTests();
        assertTestsMatches(storageFactory, s -> s == factory);
    }

    @Test
    @DisplayName("reset the production and testing values")
    @SuppressWarnings("ObjectEquality" /* a check for sameness is correct here.*/)
    void resetTheValues() {
        InMemoryStorageFactory factoryForProduction = InMemoryStorageFactory.newInstance();
        MemoizingStorageFactory factoryForTests = new MemoizingStorageFactory();

        EnvSetting<StorageFactory> storageFactory = EnvSetting
                .<StorageFactory>newBuilder()
                .build();

        storageFactory.configure(factoryForProduction)
                      .forProduction();
        storageFactory.configure(factoryForTests)
                      .forTests();

        assertProductionMatches(storageFactory, s -> s == factoryForProduction);
        assertTestsMatches(storageFactory, s -> s == factoryForTests);

        storageFactory.reset();

        assertThat(storageFactory.production()).isEmpty();
        assertThat(storageFactory.tests()).isEmpty();
    }

    @Test
    @DisplayName("should wrap the production value when assigning with `productionOrAssignDefault`")
    void wrapProdIfUsingAssignOrDefault() {
        InMemoryStorageFactory storageFactory = InMemoryStorageFactory.newInstance();

        EnvSetting<StorageFactory> storageSetting = EnvSetting
                .<StorageFactory>newBuilder()
                .wrapProductionValue(SystemAwareStorageFactory::wrap)
                .build();

        StorageFactory factory = storageSetting.productionOrAssignDefault(storageFactory);
        assertThat(factory).isNotSameInstanceAs(storageFactory);
        assertThat(factory).isInstanceOf(SystemAwareStorageFactory.class);
    }

    @Test
    @DisplayName("should wrap the testing value when assigning with `productionOrAssignDefault`")
    void wrapTestsIfUsingAssignOrDefault() {
        MemoizingStorageFactory storageFactory = new MemoizingStorageFactory();

        EnvSetting<StorageFactory> storageSetting = EnvSetting
                .<StorageFactory>newBuilder()
                .wrapTestValue(SystemAwareStorageFactory::wrap)
                .build();

        StorageFactory factory = storageSetting.testsOrAssignDefault(storageFactory);
        assertThat(factory).isNotSameInstanceAs(storageFactory);
        assertThat(factory).isInstanceOf(SystemAwareStorageFactory.class);
    }

    private static <P> void assertProductionMatches(EnvSetting<P> value,
                                                    Predicate<P> assertion) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        P prodValue = value.production()
                           .get();
        assertThat(assertion.test(prodValue)).isTrue();

    }

    private static <P> void assertTestsMatches(EnvSetting<P> value,
                                               Predicate<P> assertion) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        P prodValue = value.tests()
                           .get();
        assertThat(assertion.test(prodValue)).isTrue();
    }
}
