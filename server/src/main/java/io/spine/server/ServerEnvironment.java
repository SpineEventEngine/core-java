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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

@SuppressWarnings("AccessOfSystemProperties") // OK as we need system properties for this class.
public class ServerEnvironment {

    /** The key of the Google AppEngine runtime version system property. */
    @VisibleForTesting
    static final String ENV_KEY_APP_ENGINE_RUNTIME_VERSION = "com.google.appengine.runtime.version";

    /** If set, contains the version of AppEngine obtained from the system property. */
    private static final @Nullable String appEngineRuntimeVersion =
            System.getProperty(ENV_KEY_APP_ENGINE_RUNTIME_VERSION);

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
        boolean isVersionPresent = (appEngineRuntimeVersion != null) &&
                !appEngineRuntimeVersion.isEmpty();
        return isVersionPresent;
    }

    /**
     * Returns the current Google AppEngine version
     * or {@code null} if the program is running not on the AppEngine.
     */
    public Optional<String> appEngineVersion() {
        return Optional.ofNullable(appEngineRuntimeVersion);
    }

    /**
     * A singleton holder.
     *
     * <p>Server environment is a singleton in scope of JVM.
     */
    private static final class Singleton {
        private static final ServerEnvironment INSTANCE = new ServerEnvironment();
    }
}
