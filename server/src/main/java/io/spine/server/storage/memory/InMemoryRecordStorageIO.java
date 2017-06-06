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

import io.grpc.ManagedChannel;
import io.spine.base.Identifier;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.RecordStorageIO;
import io.spine.server.storage.memory.grpc.InMemoryGrpcServer;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceBlockingStub;
import io.spine.server.storage.memory.grpc.RecordStorageRequest;
import io.spine.type.TypeUrl;
import io.spine.users.TenantId;

/**
 * A {@link RecordStorageIO} for {@link InMemoryRecordStorage}.
 *
 * @author Alexander Yevsyukov
 */
class InMemoryRecordStorageIO<I> extends RecordStorageIO<I> {

    private final String boundedContextName;
    private final TypeUrl entityStateUrl;

    InMemoryRecordStorageIO(String boundedContextName, Class<I> idClass,
                            TypeUrl entityStateUrl) {
        super(idClass);
        this.boundedContextName = boundedContextName;
        this.entityStateUrl = entityStateUrl;
    }

    @Override
    public ReadFn<I> readFn(TenantId tenantId) {
        return new InMemReadFn<>(boundedContextName, tenantId, entityStateUrl);
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return new InMemWriteFn<>(boundedContextName, tenantId, entityStateUrl);
    }

    private static class InMemReadFn<I> extends ReadFn<I> {

        private static final long serialVersionUID = 0L;
        private final String boundedContextName;
        private final TypeUrl entityStateUrl;
        private transient ManagedChannel channel;
        private transient ProjectionStorageServiceBlockingStub blockingStub;

        protected InMemReadFn(String boundedContextName, TenantId tenantId,
                              TypeUrl entityState) {
            super(tenantId);
            this.boundedContextName = boundedContextName;
            this.entityStateUrl = entityState;
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
        private final String boundedContextName;
        private final TypeUrl entityStateUrl;
        private transient ManagedChannel channel;
        private transient ProjectionStorageServiceBlockingStub blockingStub;

        private InMemWriteFn(String boundedContextName,
                             TenantId tenantId,
                             TypeUrl entityStateUrl) {
            super(tenantId);
            this.boundedContextName = boundedContextName;
            this.entityStateUrl = entityStateUrl;
        }

        @StartBundle
        public void startBundle() {
            channel = InMemoryGrpcServer.createChannel(boundedContextName);
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
