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
import org.spine3.protobuf.Timestamps;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Timestamps.add;

/**
 * Represents a write operation for the storage, designed to modify
 * multiple items in the {@code Storage} at once.
 *
 * <p>Acts as an intermediate buffer for the changes to apply.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
class BulkWriteOperation<I, P extends Projection<I, ?>> implements AutoCloseable {

    private final Timestamp expirationTime;
    private final AtomicBoolean active = new AtomicBoolean(false);

    private final Set<P> pendingProjections = Collections.synchronizedSet(new HashSet<P>());
    private Timestamp lastHandledEventTime = Timestamp.getDefaultInstance();

    private FlushCallback<P> flushCallback;

    BulkWriteOperation(Duration maximumDuration, FlushCallback<P> flushCallback) {
        this.flushCallback = flushCallback;
        this.expirationTime = add(Timestamps.getCurrentTime(), maximumDuration);
        this.active.set(true);
    }

    /**
     * Checks if this operation has been started and is still in progress.
     *
     * @return {@code true} if the operation is in progress, {@code false} otherwise
     */
    boolean isInProgress() {
        return active.get();
    }

    void checkExpiration() {
        if (!active.get()) {
            return;
        }

        final Timestamp currentTime = Timestamps.getCurrentTime();
        if (Timestamps.compare(currentTime, expirationTime) > 0) {
            log().warn(
                    "Completing bulk write operation before all the events are processed. Took at least {} seconds.",
                    expirationTime.getSeconds());
            complete();
        }
    }

    void writeProjection(P projection) {
        pendingProjections.add(projection);
    }

    void writeLastHandledEventTime(Timestamp lastHandledEventTime) {
        this.lastHandledEventTime = checkNotNull(lastHandledEventTime);
    }

    void complete() {
        flushCallback.onFlushResults(pendingProjections, lastHandledEventTime);
    }

    @Override
    public void close() {
        active.set(false);
        flushCallback = null;
    }

    interface FlushCallback<P extends Projection<?, ?>> {

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
