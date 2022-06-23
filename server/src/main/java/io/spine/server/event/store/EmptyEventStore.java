/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.event.store;

import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;

/**
 * An implementation of {@link EventStore} which never stores any events.
 */
@Internal
public final class EmptyEventStore implements EventStore {

    private boolean open = true;
    private boolean registered = false;

    @Override
    public void append(Event event) {
        // NOP.
    }

    @Override
    public void appendAll(Iterable<Event> events) {
        // NOP.
    }

    @Override
    public void read(EventStreamQuery request, StreamObserver<Event> responseObserver) {
        responseObserver.onCompleted();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void registerWith(BoundedContext context) {
        this.registered = true;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public void close() {
        open = false;
    }
}
