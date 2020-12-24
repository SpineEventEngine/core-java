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

package io.spine.server.event;

import io.grpc.stub.StreamObserver;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.server.Closeable;

/**
 * A store of all events in a Bounded Context.
 */
public interface EventStore extends Closeable {

    /**
     * Appends the passed event to the history of events.
     *
     * @param event the record to append
     */
    void append(Event event);

    /**
     * Appends the passed events to the history of events.
     *
     * <p>If the passed {@link Iterable} is empty, no action is performed.
     *
     * <p>If the passed {@linkplain Event Events} belong to the different
     * {@linkplain TenantId tenants}, an {@link IllegalArgumentException} is thrown.
     *
     * @param events the events to append
     */
    void appendAll(Iterable<Event> events);

    /**
     * Creates the stream with events matching the passed query.
     *
     * @param request          the query with filtering parameters for the event history
     * @param responseObserver observer for the resulting stream
     */
    void read(EventStreamQuery request, StreamObserver<Event> responseObserver);
}
