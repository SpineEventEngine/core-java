/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.GeneratedMixin;

import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mixin of the {@link PickUpAck} that provides convenient methods for accessing the result.
 */
@GeneratedMixin
public interface PickUpAckMixin {

    /**
     * Creates a new {@code PickUpOutcome} of successfully picked shard
     * with the given {@code ShardSessionRecord}.
     */
    @SuppressWarnings("ClassReferencesSubclass") // This is a mixin of the type.
    static PickUpAck pickedUp(ShardSessionRecord session) {
        checkNotNull(session);
        return PickUpAck
                .newBuilder()
                .setSession(session)
                .vBuild();
    }

    /**
     * Creates a new {@code PickUpOutcome} indicating that the shard is already picked by the given
     * {@code worker}.
     */
    @SuppressWarnings("ClassReferencesSubclass") // This is a mixin of the type.
    static PickUpAck alreadyPickedBy(WorkerId worker) {
        checkNotNull(worker);
        return PickUpAck
                .newBuilder()
                .setAlreadyPickedBy(worker)
                .vBuild();
    }

    /**
     * Calls the given {@code consumer} with the {@code ShardProcessingSession} if the shard is
     * successfully picked.
     */
    @CanIgnoreReturnValue
    default PickUpAckMixin ifPicked(Consumer<ShardSessionRecord> consumer) {
        if (getSession() != null) {
            consumer.accept(getSession());
        }
        return this;
    }

    /**
     * Calls the given {@code consumer} if the shard could not be picked because it's already
     * picked by another worker.
     *
     * <p>The worker who owns the session will be passed to the {@code consumer}.
     */
    @CanIgnoreReturnValue
    default PickUpAckMixin ifAlreadyPicked(Consumer<WorkerId> consumer) {
        if (getAlreadyPickedBy() != null) {
            consumer.accept(getAlreadyPickedBy());
        }
        return this;
    }

    /**
     * Returns {@code ShardProcessingSession} if this outcome indicates that shard is successfully
     * picked, or empty {@code Optional} otherwise.
     */
    default Optional<ShardSessionRecord> session() {
        return Optional.ofNullable(getSession());
    }

    /**
     * Returns {@code WorkerId} if this outcome indicates that shard could not be picked
     * as it's already picked by another worker, or empty {@code Optional} otherwise.
     */
    default Optional<WorkerId> alreadyPickedBy() {
        return Optional.ofNullable(getAlreadyPickedBy());
    }

    ShardSessionRecord getSession();

    WorkerId getAlreadyPickedBy();
}
