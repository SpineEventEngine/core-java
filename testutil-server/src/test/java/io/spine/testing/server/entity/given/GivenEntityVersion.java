/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.testing.server.entity.given;

import com.google.protobuf.util.Timestamps;
import io.spine.core.Version;

import static io.spine.base.Time.currentTime;

public final class GivenEntityVersion {

    /** Prevents instantiation of this test env class. */
    private GivenEntityVersion() {
    }

    public static Version version() {
        Version version = Version
                .newBuilder()
                .setNumber(42)
                .setTimestamp(currentTime())
                .vBuild();
        return version;
    }

    public static Version olderVersion() {
        Version version = Version
                .newBuilder()
                .setNumber(15)
                .setTimestamp(Timestamps.MIN_VALUE)
                .vBuild();
        return version;
    }

    public static Version newerVersion() {
        Version version = Version
                .newBuilder()
                .setNumber(125)
                .setTimestamp(Timestamps.MAX_VALUE)
                .vBuild();
        return version;
    }
}
