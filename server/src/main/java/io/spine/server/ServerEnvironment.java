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
import static io.spine.server.DeploymentType.APPENGINE_CLOUD;
import static io.spine.server.DeploymentType.APPENGINE_EMULATOR;
import static io.spine.server.DeploymentType.STANDALONE;
import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_ENVIRONMENT;
import static io.spine.server.ServerEnvironment.SystemProperty.APP_ENGINE_VERSION;
import static java.util.Optional.ofNullable;

/**
 * The server conditions and configuration under which the application operates.
 */
public final class ServerEnvironment {

    private static final ServerEnvironment INSTANCE = new ServerEnvironment();

    @VisibleForTesting
    static final String APP_ENGINE_ENVIRONMENT_PRODUCTION_VALUE = "Production";
    @VisibleForTesting
    static final String APP_ENGINE_ENVIRONMENT_DEVELOPMENT_VALUE = "Development";

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
     * Returns {@code true} if the code is running on the Google App Engine,
     * {@code false} otherwise.
     *
     * @deprecated this method will be removed in 1.0, check {@linkplain #getDeploymentType()
     *         deployment type} to match any of
     *         {@link DeploymentType#APPENGINE_EMULATOR APPENGINE_EMULATOR} or
     *         {@link DeploymentType#APPENGINE_CLOUD APPENGINE_CLOUD} instead.
     */
    @Deprecated
    public boolean isAppEngine() {
        Optional<String> gaeVersion = APP_ENGINE_VERSION.value();
        boolean isVersionPresent = gaeVersion.isPresent();
        return isVersionPresent;
    }

    /**
     * Returns an optional with current Google App Engine version
     * or {@code empty} if the program is running not on the App Engine.
     *
     * @deprecated this method will be removed in 1.0.
     */
    @Deprecated
    public Optional<String> appEngineVersion() {
        return APP_ENGINE_VERSION.value();
    }

    /**
     * The kind of server environment application is running on.
     */
    public DeploymentType getDeploymentType() {
        Optional<String> value = APP_ENGINE_ENVIRONMENT.value();
        if (value.isPresent()) {
            if (APP_ENGINE_ENVIRONMENT_DEVELOPMENT_VALUE.equals(value.get())) {
                return APPENGINE_EMULATOR;
            }
            if (APP_ENGINE_ENVIRONMENT_PRODUCTION_VALUE.equals(value.get())) {
                return APPENGINE_CLOUD;
            }
        }
        return STANDALONE;
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
