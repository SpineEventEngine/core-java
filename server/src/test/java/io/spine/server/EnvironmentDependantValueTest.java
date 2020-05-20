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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`EnvironmentDependantValue` should")
class EnvironmentDependantValueTest {

    private static final UnaryOperator<StorageFactory> WRAPPER_FUNCTION = SystemAwareStorageFactory::wrap;

    @Test
    @DisplayName("not allows to configure a `null` value")
    void nullsForProductionForbidden() {
        EnvironmentDependantValue<?> config = EnvironmentDependantValue
                .<String>newBuilder()
                .build();
        assertThrows(NullPointerException.class, () -> config.configure(null));
    }

    @Test
    @DisplayName("return a wrapped production value")
    void wrapProduction() {
        EnvironmentDependantValue<StorageFactory> storageFactory = EnvironmentDependantValue
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
        EnvironmentDependantValue<StorageFactory> storageFactory = EnvironmentDependantValue
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
        EnvironmentDependantValue<StorageFactory> storageFactory = EnvironmentDependantValue
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
        EnvironmentDependantValue<StorageFactory> storageFactory = EnvironmentDependantValue
                .<StorageFactory>newBuilder()
                .build();
        storageFactory.configure(factory)
                      .forTests();
        assertTestsMatches(storageFactory, s -> s == factory);
    }

    private static <P> void assertProductionMatches(EnvironmentDependantValue<P> value,
                                                    Predicate<P> assertion) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        P prodValue = value.production()
                           .get();
        assertThat(assertion.test(prodValue)).isTrue();

    }

    private static <P> void assertTestsMatches(EnvironmentDependantValue<P> value,
                                               Predicate<P> assertion) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        P prodValue = value.tests()
                           .get();
        assertThat(assertion.test(prodValue)).isTrue();
    }
}
