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

package io.spine.system.server;

import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.server.event.EventDispatcher;

import java.util.Iterator;

import static java.util.Collections.emptyIterator;

/**
 * An implementation of {@link SystemReadSide} which never performs an operation.
 *
 * <p>All the methods inherited from {@link SystemReadSide} exit without any action or exception.
 *
 * <p>This implementation is used by the system bounded context itself, since there is no system
 * bounded context for a system bounded context.
 */
public enum NoOpSystemReadSide implements SystemReadSide {

    INSTANCE;

    @Override
    public void register(EventDispatcher<?> dispatcher) {
        // NOP.
    }

    @Override
    public void unregister(EventDispatcher<?> dispatcher) {
        // NOP.
    }

    @Override
    public Iterator<EntityStateWithVersion> readDomainAggregate(Query query) {
        return emptyIterator();
    }
}
