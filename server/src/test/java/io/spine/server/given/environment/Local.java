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

package io.spine.server.given.environment;

import io.spine.base.EnvironmentType;

/**
 * A local environment.
 *
 * <p>Is a singleton. Controlled by static methods: {@link #enable()}, {@link #disable()}.
 */
public final class Local extends EnvironmentType {

    @Override
    protected boolean enabled() {
        return Singleton.INSTANCE.enabled;
    }

    public static void enable() {
        Singleton.INSTANCE.enabled = true;
    }

    public static void disable() {
        Singleton.INSTANCE.enabled = false;
    }

    public static Local type() {
        return Singleton.INSTANCE.local;
    }

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private enum Singleton {

        INSTANCE;

        private final Local local = new Local();
        private boolean enabled;
    }
}