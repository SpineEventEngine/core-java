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

import io.spine.server.ServerEnvironment.AppEngineEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_ENVIRONMENT;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ServerEnvironment utility should")
class ServerEnvironmentTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ServerEnvironment.class);
    }

    @Test
    @DisplayName("tell when not running under AppEngine")
    void tellIfNotInAppEngine() {
        // Tests are not run by AppEngine by default.
        assertFalse(ServerEnvironment.getInstance().isAppEngine());
    }

    @Test
    @DisplayName("tell when not running on AppEngine cloud infrastructure")
    void tellIfNotInProductionAppEngine() {
        // Tests are not run by AppEngine by default.
        assertFalse(ServerEnvironment.getInstance()
                                     .isProductionAppEngine());
    }

    @Test
    @DisplayName("obtain AppEngine version as optional string")
    void getAppEngineVersion() {
        // By default we're not running under AppEngine.
        assertFalse(ServerEnvironment.getInstance()
                                     .appEngineVersion()
                                     .isPresent());
    }

    @Test
    @DisplayName("obtain AppEngine environment as optional string")
    void getAppEngineEnvironment() {
        // By default we're not running under AppEngine.
        assertFalse(ServerEnvironment.getInstance()
                                     .appEngineEnvironment()
                                     .isPresent());
    }

    @Nested
    @DisplayName("when running on App Engine cloud infrastructure")
    class OnProdAppEngine extends WithAppEngineEnvironment {

        OnProdAppEngine() {
            super(AppEngineEnvironment.PRODUCTION);
        }

        @Test
        @DisplayName("tell that running on production GAE")
        void tellIfInProductionAppEngine() {
            assertTrue(ServerEnvironment.getInstance()
                                        .isProductionAppEngine());
        }
    }

    @Nested
    @DisplayName("when running on App Engine local server")
    class OnDevAppEngine extends WithAppEngineEnvironment {

        OnDevAppEngine() {
            super(AppEngineEnvironment.DEVELOPMENT);
        }

        @Test
        @DisplayName("tell that not running on production GAE")
        void tellIfInProductionAppEngine() {
            assertFalse(ServerEnvironment.getInstance()
                                         .isProductionAppEngine());
        }
    }

    @SuppressWarnings({
            "AccessOfSystemProperties" /* Testing the configuration loaded from System properties. */,
            "AbstractClassWithoutAbstractMethods" /* A test base with setUp and tearDown. */
    })
    abstract class WithAppEngineEnvironment {

        private final AppEngineEnvironment targetEnvironment;

        private String initialValue;

        WithAppEngineEnvironment(AppEngineEnvironment targetEnvironment) {
            this.targetEnvironment = targetEnvironment;
        }

        @BeforeEach
        void setUp() {
            initialValue = System.getProperty(APP_ENGINE_ENVIRONMENT.path());
            System.setProperty(APP_ENGINE_ENVIRONMENT.path(), targetEnvironment.propertyValue());
        }

        @AfterEach
        void tearDown() {
            if (initialValue == null) {
                System.clearProperty(APP_ENGINE_ENVIRONMENT.path());
            } else {
                System.setProperty(APP_ENGINE_ENVIRONMENT.path(), initialValue);
            }
        }

        @SuppressWarnings("unused" /* Executed by JUnit. */)
        @Test
        @DisplayName("obtain AppEngine environment")
        void getAppEngineEnvironment() {
            ServerEnvironment serverEnvironment = ServerEnvironment.getInstance();
            Optional<AppEngineEnvironment> environment = serverEnvironment.appEngineEnvironment();
            assertTrue(environment.isPresent());
            assertEquals(targetEnvironment, environment.get());
        }
    }
}
