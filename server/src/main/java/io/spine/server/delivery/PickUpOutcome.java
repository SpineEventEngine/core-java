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
import io.spine.server.NodeId;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An outcome of the {@link ShardedWorkRegistry#pickUp(ShardIndex, NodeId) pickUp()} method of
 * {@link ShardedWorkRegistry} indicating the result of the pickup attempt.
 *
 * <p>The outcome contains one of 3 following results:
 * <p>1. Shard picked up successfully. In this case the outcome will contain
 * a new {@link ShardProcessingSession}.
 * <p>2. Shard could not be picked up as it's already picked by another worker. In this case
 * the outcome will contain the {@code WorkerId} of the worker owning the session.
 * <p>3. Shard could not be picked up for some technical problem (Exception). In this case
 * the outcome will contain the occurred {@code Exception}.
 */
public final class PickUpOutcome {

    @Nullable
    private final ShardProcessingSession session;

    @Nullable
    private final WorkerId alreadyPickedBy;

    @Nullable
    private final Exception occurredException;

    /**
     * Creates a new {@code PickUpOutcome} with the given occurred {@code exception}.
     */
    public static PickUpOutcome error(Exception exception) {
        checkNotNull(exception);
        return new PickUpOutcome(null, null, exception);
    }

    /**
     * Creates a new {@code PickUpOutcome} of successfully picked shard
     * with the given {@code ShardProcessingSession}.
     */
    public static PickUpOutcome pickedUp(ShardProcessingSession session) {
        checkNotNull(session);
        return new PickUpOutcome(session, null, null);
    }

    /**
     * Creates a new {@code PickUpOutcome} indicating that the shard is already picked by the given
     * {@code worker}.
     */
    public static PickUpOutcome alreadyPickedBy(WorkerId worker) {
        checkNotNull(worker);
        return new PickUpOutcome(null, worker, null);
    }

    private PickUpOutcome(@Nullable ShardProcessingSession session,
                          @Nullable WorkerId alreadyPickedBy,
                          @Nullable Exception occurredException) {
        this.session = session;
        this.alreadyPickedBy = alreadyPickedBy;
        this.occurredException = occurredException;
    }

    /**
     * Calls the given {@code consumer} with the {@code ShardProcessingSession} if the shard is
     * successfully picked.
     */
    @CanIgnoreReturnValue
    public PickUpOutcome ifPicked(Consumer<ShardProcessingSession> consumer) {
        if (session != null) {
            consumer.accept(session);
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
    public PickUpOutcome ifAlreadyPicked(Consumer<WorkerId> consumer) {
        if (alreadyPickedBy != null) {
            consumer.accept(alreadyPickedBy);
        }
        return this;
    }

    /**
     * Calls the given {@code consumer} if the shard could not be picked because of the occurred
     * exception.
     *
     * <p>The occurred exception will be passed to the {@code consumer}.
     */
    @CanIgnoreReturnValue
    public PickUpOutcome ifExceptionOccurred(Consumer<Exception> consumer) {
        if (occurredException != null) {
            consumer.accept(occurredException);
        }
        return this;
    }

    /**
     * Returns {@code true} if this outcome indicates that shard is successfully picked and contains
     * the {@code ShardProcessingSession}, or {@code false} otherwise.
     */
    public boolean isPicked() {
        return session != null;
    }

    /**
     * Returns {@code true} if this outcome indicates that shard could not be picked
     * as it's already picked by another worker, and contains the {@code WorkerId} of the worker
     * who ows the session, or {@code false} otherwise.
     */
    public boolean isAlreadyPicked() {
        return alreadyPickedBy != null;
    }

    /**
     * Returns {@code true} if this outcome indicates an error occurred during the pickup attempt
     * and contains the occurred exception, or {@code false} otherwise.
     */
    public boolean isExceptionOccurred() {
        return occurredException != null;
    }

    /**
     * Gets the {@code ShardProcessingSession} if the shard is successfully picked or throws
     * {@code NullPointerException} otherwise.
     *
     * <p>Call this method if a caller is sure that the shard is picked up by the request (checked
     * by {@link #isPicked()}). In other cases use {@link #session()},
     * or {@link #ifPicked(Consumer) ifPicked()}.
     */
    public ShardProcessingSession getSession() {
        return checkNotNull(session);
    }

    /**
     * Gets the {@code WorkerId} of the worker who owns the session if the shard could not be picked
     * as it's already picked by another worker, or throws {@code NullPointerException} otherwise.
     *
     * <p>Call this method if a caller is sure that the shard is not picked up because it's already
     * picked by another worker (checked by {@link #isAlreadyPicked()}). In other cases use
     * {@link #alreadyPickedBy()}, or {@link #ifAlreadyPicked(Consumer) ifAlreadyPicked()}.
     */
    public WorkerId getAlreadyPickedBy() {
        return checkNotNull(alreadyPickedBy);
    }

    /**
     * Gets the occurred {@code Exception} if the shard could not be picked because of an error,
     * or throws the {@code NullPointerException} otherwise.
     *
     * <p>Call this method if a caller is sure that the shard is not picked because of an exception
     * (checked by {@link #isExceptionOccurred()}). In other cases use {@link #occurredException()},
     * or {@link #ifExceptionOccurred(Consumer) ifErrorOccurred()}.
     */
    public Exception getOccurredException() {
        return checkNotNull(occurredException);
    }

    /**
     * Returns {@code ShardProcessingSession} if this outcome indicates that shard is successfully
     * picked, or empty {@code Optional} otherwise.
     */
    public Optional<ShardProcessingSession> session() {
        return Optional.ofNullable(session);
    }

    /**
     * Returns {@code WorkerId} if this outcome indicates that shard could not be picked
     * as it's already picked by another worker, or empty {@code Optional} otherwise.
     */
    public Optional<WorkerId> alreadyPickedBy() {
        return Optional.ofNullable(alreadyPickedBy);
    }

    /**
     * Returns {@code Exception} if this outcome indicates an error occurred during
     * the pickup attempt, or empty {@code Optional} otherwise.
     */
    public Optional<Exception> occurredException() {
        return Optional.ofNullable(occurredException);
    }
}
