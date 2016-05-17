/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

/**
 * Provides information about the environment (current platform used, etc).
 *
 * @author Alexander Litus
 */
public class Environment {

    /**
     * The key of the Google AppEngine runtime version system property.
     */
    public static final String APP_ENGINE_RUNTIME_VERSION_KEY = "com.google.appengine.runtime.version";

    @SuppressWarnings("AccessOfSystemProperties")
    private static final String APP_ENGINE_RUNTIME_VERSION = System.getProperty(APP_ENGINE_RUNTIME_VERSION_KEY);

    protected Environment() {}

    /**
     * Returns the singleton instance.
     */
    public static Environment getInstance() {
        return Singleton.INSTANCE.value;
    }

    /**
     * Returns {@code true} if the code is running on the Google AppEngine,
     * {@code false} otherwise.
     */
    public boolean isAppEngine() {
        final boolean isVersionPresent = (APP_ENGINE_RUNTIME_VERSION != null) &&
                !APP_ENGINE_RUNTIME_VERSION.isEmpty();
        return isVersionPresent;
    }

    /**
     * Returns the current Google AppEngine version
     * retrieved by the key {@link Environment#APP_ENGINE_RUNTIME_VERSION_KEY}.
     */
    public String getAppEngineVersion() {
        return APP_ENGINE_RUNTIME_VERSION;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Environment value = new Environment();
    }
}
