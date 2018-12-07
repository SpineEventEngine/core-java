/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

/**
 * Something with {@link LifecycleFlags}.
 */
public interface WithLifecycle {

    /**
     * Obtains current lifecycle flags.
     */
    @SuppressWarnings("override") // not marked in the generated code
    LifecycleFlags getLifecycleFlags();

    /**
     * Shows if current instance is marked as archived or not.
     */
    default boolean isArchived() {
        return getLifecycleFlags().getArchived();
    }

    /**
     * Shows if current instance is marked as deleted or not.
     */
    default boolean isDeleted() {
        return getLifecycleFlags().getDeleted();
    }

    /**
     * Verifies if any of the lifecycle attributes is set.
     *
     * @return {@code false} if any of the attributes is set, {@code true} otherwise
     */
    default boolean isActive() {
        return !(isArchived() || isDeleted());
    }
}
