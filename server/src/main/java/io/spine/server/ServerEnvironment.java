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

import com.google.common.annotations.VisibleForTesting;

import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;
import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_ENVIRONMENT;
import static io.spine.server.DeploymentType.APPENGINE_CLOUD;
import static io.spine.server.DeploymentType.APPENGINE_EMULATOR;
import static io.spine.server.DeploymentType.STANDALONE;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

/**
 * The server conditions and configuration under which the application operates.
 */
public final class ServerEnvironment {

    private static final ServerEnvironment INSTANCE = new ServerEnvironment(); 

    /** Prevents instantiation of this utility class. */
    private ServerEnvironment() {
    }

    /**
     * Returns a singleton instance.
     */
    public static ServerEnvironment getInstance() {
        return INSTANCE;
    }

    /**
     * The kind of server environment application is running on.
     */
    public DeploymentType getDeploymentType() {
        Optional<DeploymentType> gaeKind = AppEngineEnvironment.get();
        return gaeKind.orElse(STANDALONE);
    }

    /**
     * Type of an App Engine Environment if applicable.
     *
     * <p>Either {@linkplain #PRODUCTION Production} when running on cloud infrastructure or
     * {@linkplain #DEVELOPMENT Development} when running local development server.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    // Duplicates of GAE environment names in tests.
    private enum AppEngineEnvironment {
        PRODUCTION("Production", APPENGINE_CLOUD),
        DEVELOPMENT("Development", APPENGINE_EMULATOR);

        private final String propertyValue;
        private final DeploymentType serverEnvironment;

        AppEngineEnvironment(String propertyValue, DeploymentType serverEnvironment) {
            this.propertyValue = propertyValue;
            this.serverEnvironment = serverEnvironment;
        }

        /**
         * Checks the {@linkplain SystemProperty#APP_ENGINE_ENVIRONMENT App Engine environment} 
         * system property value to match any of existing {@linkplain DeploymentType
         * server environment kinds}.
         */
        private static Optional<DeploymentType> get() {
            return APP_ENGINE_ENVIRONMENT
                    .value()
                    .flatMap(AppEngineEnvironment::match);
        }

        /**
         * Matches the provided value to the corresponding App Engine {@link DeploymentType}.
         */
        private static Optional<DeploymentType> match(String value) {
            return stream(values())
                    .filter(environment -> environment.matches(value))
                    .map(environment -> environment.serverEnvironment)
                    .findFirst();
        }

        private boolean matches(String value) {
            return propertyValue.equals(value);
        }
    }

    /**
     * The {@linkplain System#getProperties() System Properties} defining the Server Environment.
     */
    @VisibleForTesting
    @SuppressWarnings("AccessOfSystemProperties")// OK as we need system properties for this class.
    enum SystemProperty {
        APP_ENGINE_ENVIRONMENT("com.google.appengine.runtime.serverEnvironment");

        private final String path;

        SystemProperty(String path) {
            this.path = path;
        }

        /**
         * An optional value of {@linkplain System#getProperty(String) the property}.
         *
         * @return optional with string if property exists and is not an empty string,
         *         {@code empty} otherwise
         */
        private Optional<String> value() {
            String systemValue = System.getProperty(path());
            String nonEmptyValue = emptyToNull(systemValue);
            return ofNullable(nonEmptyValue);
        }

        String path() {
            return path;
        }
    }
}
