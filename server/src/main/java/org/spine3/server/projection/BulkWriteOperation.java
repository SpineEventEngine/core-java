/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.protobuf.Timestamps2;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.protobuf.util.Timestamps.add;
import static java.lang.String.format;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * Represents a write operation for the projection storage, designed to modify
 * multiple items in the {@code Storage} at once.
 *
 * <p>Acts as an intermediate buffer for the changes to apply.
 *
 * <p>Primary usage is letting the {@link ProjectionRepository} write
 * loaded projections all at once.
 *
 * @param <I> type of the ID of the projection
 * @param <P> type of the projection
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
class BulkWriteOperation<I, P extends Projection<I, ?>> implements AutoCloseable {

    private final Timestamp expirationTime;
    private final AtomicBoolean active = new AtomicBoolean(false);

    private final Set<P> pendingProjections = Collections.synchronizedSet(new HashSet<P>());
    private Timestamp lastHandledEventTime = Timestamp.getDefaultInstance();

    /**
     * Callback to execute after the operation is complete.
     *
     * <p>Can be {@code null} if the {@link BulkWriteOperation#close()} have been already called.
     */
    @Nullable
    private FlushCallback<P> flushCallback;

    BulkWriteOperation(Duration maximumDuration, FlushCallback<P> flushCallback) {
        this.flushCallback = checkNotNull(flushCallback);
        this.expirationTime = add(getCurrentTime(), maximumDuration);
        this.active.set(true);
    }

    /**
     * Checks if this operation has been started and is still in progress (is active).
     *
     * @return {@code true} if the operation is in progress, {@code false} otherwise
     */
    boolean isInProgress() {
        return active.get();
    }

    /**
     * Verifies if the operation lasts for too long. If so, completes the operation,
     * otherwise performs no action.
     *
     * <p>Performs no action if the operation is already finished.
     *
     * @see #complete()
     */
    void checkExpiration() {
        if (!isInProgress()) {
            return;
        }

        final Timestamp currentTime = getCurrentTime();
        if (Timestamps2.compare(currentTime, expirationTime) > 0) {
            log().warn("Completing bulk write operation before all the events are processed.");

            complete();
        }
    }

    /**
     * Adds new {@link Projection} to store.
     *
     * <p>All the projections will be passed to the {@link FlushCallback callback}
     * on {@link #complete()}.
     *
     * @param projection new {@link Projection} to store
     */
    void storeProjection(P projection) {
        pendingProjections.add(projection);
    }

    /**
     * Updates the {@code lastHandledEventTime} field.
     *
     * <p>Only the last value is saved and passed to
     * the {@link FlushCallback callback} on {@link #complete()}.
     *
     * @param lastHandledEventTime the new value to set
     */
    synchronized void storeLastHandledEventTime(Timestamp lastHandledEventTime) {
        this.lastHandledEventTime = checkNotNull(lastHandledEventTime);
    }

    private synchronized Timestamp getLastHandledEventTime() {
        return lastHandledEventTime;
    }
    /**
     * Completes the operation and calls the {@link FlushCallback} passed as
     * a parameter to the constructor.
     *
     * <p>While executing the callback the operation is still considered to be active.
     * See {@link #isInProgress()} for details.
     */
    void complete() {
        final boolean closed = flushCallback == null
                               || !active.get();
        checkState(
            !closed,
            format("Can not complete the %s. Already closed.",
                   BulkWriteOperation.class.getSimpleName())
        );

        flushCallback.onFlushResults(pendingProjections, getLastHandledEventTime());
        close();
    }

    /**
     * Makes the operation not active and flushes the callback.
     *
     * <p>Called automatically on {@link #complete() operation complete} after
     * the callback is triggered.
     *
     * @see #isInProgress()
     */
    @Override
    public void close() {
        active.set(false);
        flushCallback = null;
    }

    /**
     * A callback to execute when the {@link BulkWriteOperation operation} is complete.
     */
    interface FlushCallback<P extends Projection<?, ?>> {

        /**
         * Process the accumulated results.
         *
         * @param projections          accumulated {@link Projection}s to store
         * @param lastHandledEventTime last handled event to store
         */
        void onFlushResults(Set<P> projections, Timestamp lastHandledEventTime);
    }

    private static Logger log() {
        return LoggerSingleton.INSTANCE.logger;
    }

    private enum LoggerSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger logger = LoggerFactory.getLogger(BulkWriteOperation.class);
    }
}
