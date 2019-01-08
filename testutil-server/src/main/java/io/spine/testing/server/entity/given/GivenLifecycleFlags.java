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

package io.spine.testing.server.entity.given;

import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.LifecycleFlagsVBuilder;

/**
 * @author Alexander Yevsyukov
 */
public class GivenLifecycleFlags {

    /** Prevent instantiation of this utility class. */
    private GivenLifecycleFlags() {
    }

    /**
     * Creates an instance with archived flag set to {@code true}.
     */
    public static LifecycleFlags archived() {
        return LifecycleFlagsVBuilder
                .newBuilder()
                .setArchived(true)
                .build();
    }

    /**
     * Creates an instance with deleted flag set to {@code true}.
     */
    public static LifecycleFlags deleted() {
        return LifecycleFlagsVBuilder
                .newBuilder()
                .setDeleted(true)
                .build();
    }
}
