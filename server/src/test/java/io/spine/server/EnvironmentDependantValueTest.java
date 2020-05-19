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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EnvironmentDependantValue should")
class EnvironmentDependantValueTest {

    private static final String CONFIG_STRING = "config_string";

    @Test
    @DisplayName("not allow to set a null production value")
    void nullsForProductionForbidden() {
        EnvironmentDependantValue<String> configString = EnvironmentDependantValue
                .<String>newBuilder()
                .build();
        assertThrows(NullPointerException.class, () -> configString.production(null));
    }

    @Test
    @DisplayName("not allow to set a null testing value")
    void nullsForTestsForbidden() {
        EnvironmentDependantValue<String> configString = EnvironmentDependantValue
                .<String>newBuilder()
                .build();
        assertThrows(NullPointerException.class, () -> configString.tests(null));
    }
    @Test
    @DisplayName("return a wrapped production value")
    void wrapProduction() {
        String productionPrefix = "Production_";
        EnvironmentDependantValue<String> configString = EnvironmentDependantValue
                .<String>newBuilder()
                .wrappingProduction(config -> productionPrefix + config)
                .build();
        configString.production(CONFIG_STRING);
        assertThat(configString.production()).isEqualTo(productionPrefix + CONFIG_STRING);
    }

    @Test
    @DisplayName("return a wrapped test value")
    void wrapTests() {
        String testPrefix = "Test_";
        EnvironmentDependantValue<String> configString = EnvironmentDependantValue
                .<String>newBuilder()
                .wrappingTests(config -> testPrefix + config)
                .build();
        configString.tests(CONFIG_STRING);
        assertThat(configString.tests()).isEqualTo(testPrefix + CONFIG_STRING);

    }

    @Test
    @DisplayName("return an unwrapped production value if no wrapping function was specified")
    void returnWhenNotWrappedProduction() {
        EnvironmentDependantValue<String> configString = EnvironmentDependantValue
                .<String>newBuilder()
                .build();
        configString.production(CONFIG_STRING);
        assertThat(configString.production()).isEqualTo(CONFIG_STRING);
    }

    @Test
    @DisplayName("return an unwrapped test value if no wrapping function was specified")
    void returnWhenNotWrappedTests() {
        EnvironmentDependantValue<String> configString = EnvironmentDependantValue
                .<String>newBuilder()
                .build();
        configString.tests(CONFIG_STRING);
        assertThat(configString.tests()).isEqualTo(CONFIG_STRING);
    }
}
