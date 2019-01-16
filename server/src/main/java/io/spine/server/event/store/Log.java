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

package io.spine.server.event.store;

import com.google.protobuf.TextFormat;
import io.grpc.stub.StreamObserver;
import io.spine.core.Event;
import io.spine.server.event.EventStreamQuery;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

/**
 * Logging for operations of {@link EventStore}.
 */
final class Log {

    private final @Nullable Logger logger;

    Log(@Nullable Logger logger) {
        this.logger = logger;
    }

    void stored(Event event) {
        if (logger == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Stored: {}", TextFormat.shortDebugString(event));
        }
    }

    void stored(Iterable<Event> events) {
        for (Event event : events) {
            stored(event);
        }
    }

    void readingStart(EventStreamQuery query, StreamObserver<Event> responseObserver) {
        if (logger == null) {
            return;
        }

        if (logger.isDebugEnabled()) {
            String requestData = TextFormat.shortDebugString(query);
            logger.debug("Creating stream on request: {} for observer: {}",
                         requestData,
                         responseObserver);
        }
    }

    void readingComplete(StreamObserver<Event> observer) {
        if (logger == null) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Observer {} got all queried events.", observer);
        }
    }
}
