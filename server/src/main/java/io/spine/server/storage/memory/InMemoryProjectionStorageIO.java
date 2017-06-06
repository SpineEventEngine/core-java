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

package io.spine.server.storage.memory;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.spine.server.projection.ProjectionStorageIO;
import io.spine.server.storage.RecordStorageIO;
import io.spine.server.storage.memory.grpc.InMemoryGrpcServer;
import io.spine.server.storage.memory.grpc.LastHandledEventRequest;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceBlockingStub;
import io.spine.type.TypeUrl;
import io.spine.users.TenantId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BeamIO operations for in-memory projection storage.
 *
 * @author Alexander Yevsyukov
 */
class InMemoryProjectionStorageIO<I> extends ProjectionStorageIO<I> {

    private final String boundedContextName;
    private final TypeUrl stateTypeUrl;
    private final RecordStorageIO<I> storageIO;

    InMemoryProjectionStorageIO(
            String boundedContextName, TypeUrl stateTypeUrl,
            RecordStorageIO<I> storageIO) {
        super(storageIO.getIdClass());
        this.boundedContextName = boundedContextName;
        this.stateTypeUrl = stateTypeUrl;
        this.storageIO = storageIO;
    }

    @Override
    public WriteTimestampFn writeTimestampFn(TenantId tenantId) {
        return new WriteTimestampOverGrpc(boundedContextName, tenantId, stateTypeUrl);
    }

    @Override
    public ReadFn<I> readFn(TenantId tenantId) {
        return storageIO.readFn(tenantId);
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return storageIO.writeFn(tenantId);
    }

    private static class WriteTimestampOverGrpc extends WriteTimestampFn {

        private static final long serialVersionUID = 0L;
        private final String boundedContextName;
        private final TypeUrl stateTypeUrl;

        private transient ManagedChannel channel;
        private transient ProjectionStorageServiceBlockingStub blockingStub;

        private WriteTimestampOverGrpc(String boundedContextName, TenantId tenantId,
                                       TypeUrl typeUrl) {
            super(tenantId);
            this.boundedContextName = boundedContextName;
            this.stateTypeUrl = typeUrl;
        }

        @SuppressWarnings("unused") // called by Beam
        @StartBundle
        public void startBundle() {
            channel = InMemoryGrpcServer.createChannel(boundedContextName);
            blockingStub = ProjectionStorageServiceGrpc.newBlockingStub(channel);
        }

        @SuppressWarnings("unused") // called by Beam
        @FinishBundle
        public void finishBundle() {
            channel.shutdownNow();
        }

        @Override
        protected void doWrite(TenantId tenantId, Timestamp timestamp) {
            checkNotNull(tenantId);
            checkNotNull(timestamp);
            final LastHandledEventRequest req =
                    LastHandledEventRequest.newBuilder()
                                           .setTenantId(tenantId)
                                           .setProjectionStateTypeUrl(stateTypeUrl.value())
                                           .setTimestamp(timestamp)
                                           .build();
            blockingStub.writeLastHandledEventTimestamp(req);
        }
    }
}
