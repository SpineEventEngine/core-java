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

package org.spine3.server.storage.memory;

import io.grpc.ManagedChannel;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.event.EventStoreIO;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.storage.EventRecordStorage;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.server.storage.memory.grpc.EventStreamRequest;
import org.spine3.server.storage.memory.grpc.EventStreamServiceGrpc;
import org.spine3.server.storage.memory.grpc.EventStreamServiceGrpc.EventStreamServiceBlockingStub;
import org.spine3.server.storage.memory.grpc.GrpcServer;
import org.spine3.users.TenantId;

import java.util.Iterator;
import java.util.Map;

/**
 * {@inheritDoc}
 */
class InMemoryEventRecordStorage extends EventRecordStorage {

    InMemoryEventRecordStorage(RecordStorage<EventId> storage) {
        super(storage);
    }

    @Override
    protected Map<EventId, EntityRecord> readRecords(EventStreamQuery query) {
        final Map<EventId, EntityRecord> allRecords = readAll();
        return allRecords;
    }

    /*
     * Beam support
     *****************/

    @Override
    public RecordStorageIO<EventId> getIO(Class<EventId> idClass) {
        return getDelegateStorage().getIO(idClass);
    }

    @Override
    public EventStoreIO.QueryFn queryFn(TenantId tenantId) {
        return new InMemQueryFn(tenantId);
    }

    private static class InMemQueryFn extends EventStoreIO.QueryFn {

        private static final long serialVersionUID = 0L;
        private transient ManagedChannel channel;
        private transient EventStreamServiceBlockingStub blockingStub;

        private InMemQueryFn(TenantId tenantId) {
            super(tenantId);
        }

        @StartBundle
        public void startBundle() {
            channel = GrpcServer.createDefaultChannel();
            blockingStub = EventStreamServiceGrpc.newBlockingStub(channel);
        }

        @FinishBundle
        public void finishBundle() {
            channel.shutdownNow();
            blockingStub = null;
        }

        @Override
        protected Iterator<Event> read(TenantId tenantId, EventStreamQuery query) {
            final EventStreamRequest request = EventStreamRequest.newBuilder()
                                                               .setTenantId(tenantId)
                                                               .setQuery(query)
                                                               .build();
            final Iterator<Event> result = blockingStub.query(request);
            return result;
        }
    }
}
