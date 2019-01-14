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
import io.spine.annotation.Internal;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;
import static java.lang.ThreadLocal.withInitial;

/**
 * The server conditions and configuration under which the application operates.
 */
public final class ServerEnvironment {

    private static final ServerEnvironment INSTANCE = new ServerEnvironment();

    private static final String ENV_KEY_APP_ENGINE_RUNTIME_VERSION =
            "com.google.appengine.runtime.version";

    @SuppressWarnings("AccessOfSystemProperties") /*  Based on system property. */
    private static final @Nullable String appEngineRuntimeVersion =
            emptyToNull(System.getProperty(ENV_KEY_APP_ENGINE_RUNTIME_VERSION));

    private static final ThreadLocal<Provider> provider =
            withInitial(SystemEnvironmentProvider::newInstance);

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
        boolean isVersionPresent = appEngineRuntimeVersion != null;
        return isVersionPresent;
    }

    /**
     * Returns an optional with current Google App Engine version
     * or {@code empty} if the program is not running on the App Engine.
     *
     * @deprecated this method will be removed in 1.0.
     */
    @Deprecated
    public Optional<String> appEngineVersion() {
        return Optional.ofNullable(appEngineRuntimeVersion);
    }

    /**
     * The type of the environment application is deployed to.
     */
    public static DeploymentType getDeploymentType() {
        return provider.get()
                       .getDeploymentType();
    }

    /**
     * Sets the default deployment type provider which utilizes system properties.
     */
    @VisibleForTesting
    static void resetProvider() {
        provider.set(SystemEnvironmentProvider.newInstance());
    }

    /**
     * The provider of the deployment type.
     */
    @Internal
    public interface Provider {

        DeploymentType getDeploymentType();
    }
}
