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
import static io.spine.server.ServerEnvironment.AppEngineEnvironment.PRODUCTION;
import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_ENVIRONMENT;
import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_VERSION;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

/**
 * The server conditions and configuration under which the application operates.
 */
public class ServerEnvironment {

    /** Prevents instantiation of this utility class. */
    private ServerEnvironment() {
    }

    /**
     * Returns a singleton instance.
     */
    public static ServerEnvironment getInstance() {
        return Singleton.INSTANCE;
    }

    /**
     * Returns {@code true} if the code is running on the Google AppEngine,
     * {@code false} otherwise.
     */
    public boolean isAppEngine() {
        Optional<String> version = APP_ENGINE_VERSION.value();
        return version.isPresent();
    }

    /**
     * Returns {@code true} if the code is running on the Google AppEngine cloud infrastructure,
     * {@code false} otherwise.
     */
    public boolean isProductionAppEngine() {
        Optional<String> environment = APP_ENGINE_ENVIRONMENT.value();
        return environment.map(PRODUCTION::matches)
                          .orElse(false);
    }

    /**
     * Returns an optional with current Google AppEngine version
     * or {@linkplain Optional#empty() empty} if running outside of AppEngine.
     */
    public Optional<String> appEngineVersion() {
        return APP_ENGINE_VERSION.value();
    }

    public Optional<AppEngineEnvironment> appEngineEnvironment() {
        return APP_ENGINE_ENVIRONMENT
                .value()
                .flatMap(AppEngineEnvironment::match);
    }

    /**
     * A singleton holder.
     *
     * <p>Server environment is a singleton in scope of JVM.
     */
    private static final class Singleton {
        private static final ServerEnvironment INSTANCE = new ServerEnvironment();
    }

    /**
     * Type of an App Engine Environment if applicable.
     *
     * <p>Either {@linkplain #PRODUCTION Production} when running on cloud infrastructure or
     * {@linkplain #DEVELOPMENT Development} when running local development server.
     */
    public enum AppEngineEnvironment {
        PRODUCTION("Production"),
        DEVELOPMENT("Development");

        private final String propertyValue;

        AppEngineEnvironment(String propertyValue) {
            this.propertyValue = propertyValue;
        }

        static Optional<AppEngineEnvironment> match(String value) {
            return stream(values())
                    .filter(environment -> environment.matches(value))
                    .findFirst();
        }

        boolean matches(String value) {
            return propertyValue().equals(value);
        }

        @VisibleForTesting
        String propertyValue() {
            return propertyValue;
        }
    }

    /**
     * The {@linkplain System#getProperties() System Properties} defining the Server Environment.
     */
    @VisibleForTesting
    @SuppressWarnings("AccessOfSystemProperties")// OK as we need system properties for this class.
    enum SystemProperty {
        APP_ENGINE_VERSION("com.google.appengine.runtime.version"),
        APP_ENGINE_ENVIRONMENT("com.google.appengine.runtime.environment");

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
        Optional<String> value() {
            String systemValue = System.getProperty(path());
            String nonEmptyValue = emptyToNull(systemValue);
            return ofNullable(nonEmptyValue);
        }

        @VisibleForTesting
        String path() {
            return path;
        }
    }
}
