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

package org.spine3.server.storage.memory.grpc;

import io.grpc.stub.StreamObserver;
import org.spine3.base.Event;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.tenant.TenantAwareOperation;
import org.spine3.users.TenantId;

/**
 * Exposes in-memory {@link EventStore EventStore} for querying via gRPC.
 *
 * @author Alexander Yevsyukov
 */
class EventStreamService extends EventStreamServiceGrpc.EventStreamServiceImplBase {

    private final EventStore eventStore;

    EventStreamService(BoundedContext boundedContext) {
        this.eventStore = boundedContext.getEventBus().getEventStore();
    }

    @Override
    public void query(EventStreamRequest request, final StreamObserver<Event> responseObserver) {
        final TenantId tenantId = request.getTenantId();
        final EventStreamQuery query = request.getQuery();
        final TenantAwareOperation op = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                eventStore.read(query, responseObserver);
            }
        };
        op.execute();
    }
}
