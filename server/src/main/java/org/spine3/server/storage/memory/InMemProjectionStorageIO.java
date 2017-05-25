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

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import org.spine3.server.projection.ProjectionStorageIO;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.server.storage.grpc.LastHandledEventRequest;
import org.spine3.server.storage.grpc.ProjectionStorageServiceGrpc;
import org.spine3.server.storage.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceBlockingStub;
import org.spine3.type.TypeUrl;
import org.spine3.users.TenantId;

/**
 * BeamIO operations for in-memory projection storage.
 *
 * @author Alexander Yevsyukov
 */
class InMemProjectionStorageIO<I> extends ProjectionStorageIO<I> {

    private final TypeUrl stateTypeUrl;
    private final RecordStorageIO<I> storageIO;

    InMemProjectionStorageIO(
            TypeUrl stateTypeUrl,
            RecordStorageIO<I> storageIO) {
        super(storageIO.getIdClass());
        this.stateTypeUrl = stateTypeUrl;
        this.storageIO = storageIO;
    }

    @Override
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
    // OK for test-only in-memory implementation.
    public WriteLastHandledEventTimeFn writeLastHandledEventTimeFn(TenantId tenantId) {
        return new WriteTimestampOverGrpc(tenantId, stateTypeUrl);
    }

    @Override
    public FindByQuery<I> findFn(TenantId tenantId) {
        return storageIO.findFn(tenantId);
    }

    @Override
    public Read<I> read(TenantId tenantId, Query<I> query) {
        return storageIO.read(tenantId, query);
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return storageIO.writeFn(tenantId);
    }

    private static class WriteTimestampOverGrpc extends WriteLastHandledEventTimeFn {

        private static final long serialVersionUID = 0L;
        private final TypeUrl stateTypeUrl;
        private ProjectionStorageServiceBlockingStub blockingStub;

        protected WriteTimestampOverGrpc(TenantId tenantId, TypeUrl typeUrl) {
            super(tenantId);
            this.stateTypeUrl = typeUrl;
        }

        @StartBundle
        public void startBundle() {
            final ManagedChannel channel = InMemoryBeamIO.createDefaultChannel();
            blockingStub = ProjectionStorageServiceGrpc.newBlockingStub(channel);
        }

        @Override
        protected void doWrite(TenantId tenantId, Timestamp timestamp) {
            //TODO:2017-05-25:alexander.yevsyukov: Implement
            final LastHandledEventRequest req =
                    LastHandledEventRequest.newBuilder()
                                           .setProjectionStateTypeUrl(stateTypeUrl.value())
                                           .build();
            blockingStub.writeLastHandledEventTimestamp(req);
        }
    }

}
