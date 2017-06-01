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
import org.spine3.base.Identifier;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.server.storage.memory.grpc.InMemoryGrpcServer;
import org.spine3.server.storage.memory.grpc.ProjectionStorageServiceGrpc;
import org.spine3.server.storage.memory.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceBlockingStub;
import org.spine3.server.storage.memory.grpc.RecordStorageRequest;
import org.spine3.type.TypeUrl;
import org.spine3.users.TenantId;

/**
 * A {@link RecordStorageIO} for {@link InMemoryRecordStorage}.
 *
 * @author Alexander Yevsyukov
 */
class InMemoryRecordStorageIO<I> extends RecordStorageIO<I> {

    private final TypeUrl entityStateUrl;

    InMemoryRecordStorageIO(Class<I> idClass, TypeUrl entityStateUrl) {
        super(idClass);
        this.entityStateUrl = entityStateUrl;
    }

    @Override
    public ReadFn<I> readFn(TenantId tenantId) {
        return new InMemReadFn<>(tenantId, entityStateUrl);
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return new InMemWriteFn<>(tenantId, entityStateUrl);
    }

    private static class InMemReadFn<I> extends ReadFn<I> {

        private static final long serialVersionUID = 0L;
        private final TypeUrl entityStateUrl;
        private transient ManagedChannel channel;
        private transient ProjectionStorageServiceBlockingStub blockingStub;

        protected InMemReadFn(TenantId tenantId, TypeUrl entityState) {
            super(tenantId);
            this.entityStateUrl = entityState;
        }

        @SuppressWarnings("unused") // called by Beam
        @StartBundle
        public void startBundle() {
            channel = InMemoryGrpcServer.createDefaultChannel();
            blockingStub = ProjectionStorageServiceGrpc.newBlockingStub(channel);
        }

        @SuppressWarnings("unused") // called by Beam
        @FinishBundle
        public void finishBundle() {
            channel.shutdownNow();
        }

        @Override
        protected EntityRecord doRead(TenantId tenantId, I id) {
            final RecordStorageRequest req =
                    RecordStorageRequest.newBuilder()
                                        .setTenantId(tenantId)
                                        .setEntityId(Identifier.pack(id))
                                        .setEntityStateTypeUrl(entityStateUrl.value())
                                        .build();
            final EntityRecord result = blockingStub.read(req);
            return result;
        }
    }

    private static class InMemWriteFn<I> extends WriteFn<I> {

        private static final long serialVersionUID = 0L;
        private final TypeUrl entityStateUrl;
        private transient ManagedChannel channel;
        private transient ProjectionStorageServiceBlockingStub blockingStub;

        private InMemWriteFn(TenantId tenantId, TypeUrl entityStateUrl) {
            super(tenantId);
            this.entityStateUrl = entityStateUrl;
        }

        @StartBundle
        public void startBundle() {
            channel = InMemoryGrpcServer.createDefaultChannel();
            blockingStub = ProjectionStorageServiceGrpc.newBlockingStub(channel);
        }

        @FinishBundle
        public void finishBundle() {
            channel.shutdownNow();
        }

        @Override
        protected void doWrite(TenantId tenantId, I key, EntityRecord record) {
            final RecordStorageRequest req =
                    RecordStorageRequest.newBuilder()
                                        .setTenantId(tenantId)
                                        .setEntityId(Identifier.pack(key))
                                        .setEntityStateTypeUrl(entityStateUrl.value())
                                        .setRecord(record)
                                        .build();
            blockingStub.write(req);
        }
    }
}
