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
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.RecordStorageIO;
import io.spine.server.storage.memory.grpc.RecordStorageRequest;
import io.spine.server.storage.memory.grpc.RecordStorageServiceGrpc;
import io.spine.server.storage.memory.grpc.RecordStorageServiceGrpc.RecordStorageServiceBlockingStub;
import io.spine.type.TypeUrl;
import io.spine.users.TenantId;

import java.util.Iterator;

/**
 * A {@link RecordStorageIO} for {@link InMemoryRecordStorage}.
 *
 * @author Alexander Yevsyukov
 */
public class InMemoryRecordStorageIO<I> extends RecordStorageIO<I> {

    private final String boundedContextName;
    private final TypeUrl entityStateUrl;

    InMemoryRecordStorageIO(String boundedContextName, Class<I> idClass, TypeUrl entityStateUrl) {
        super(idClass);
        this.boundedContextName = boundedContextName;
        this.entityStateUrl = entityStateUrl;
    }

    public static <I> InMemoryRecordStorageIO<I> create(Class<I> idClass,
                                                        InMemoryRecordStorage<I> storage) {
        return new InMemoryRecordStorageIO<>(storage.getBoundedContextName(),
                                             idClass,
                                             storage.getEntityStateUrl());
    }

    @Override
    public ReadFn<I> readFn(TenantId tenantId) {
        return new InMemReadFn<>(boundedContextName, tenantId, entityStateUrl);
    }

    @Override
    public FindFn findFn(TenantId tenantId) {
        return new InMemFindFn(boundedContextName, tenantId, entityStateUrl);
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return new InMemWriteFn<>(boundedContextName, tenantId, entityStateUrl);
    }

    /**
     * A {@link BoundedContextChannel} channel to exposing {@link RecordStorageServiceBlockingStub}.
     */
    private static class RecordStorageServiceChannel
            extends BoundedContextChannel<RecordStorageServiceBlockingStub> {

        RecordStorageServiceChannel(String boundedContextName) {
            super(boundedContextName);
        }

        @Override
        protected RecordStorageServiceBlockingStub createStub(ManagedChannel channel) {
            return RecordStorageServiceGrpc.newBlockingStub(channel);
        }
    }

    private static class InMemReadFn<I> extends ReadFn<I> {

        private static final long serialVersionUID = 0L;
        private final String boundedContextName;
        private final TypeUrl entityStateUrl;
        private transient RecordStorageServiceChannel channel;

        private InMemReadFn(String boundedContextName, TenantId tenantId, TypeUrl entityState) {
            super(tenantId);
            this.boundedContextName = boundedContextName;
            this.entityStateUrl = entityState;
        }

        @SuppressWarnings("unused") // called by Beam
        @StartBundle
        public void startBundle() {
            channel = new RecordStorageServiceChannel(boundedContextName);
            channel.open();
        }

        @SuppressWarnings("unused") // called by Beam
        @FinishBundle
        public void finishBundle() {
            channel.shutDown();
        }

        @Override
        protected EntityRecord doRead(TenantId tenantId, I id) {
            final RecordStorageRequest req =
                    RecordStorageRequest.newBuilder()
                                        .setTenantId(tenantId)
                                        .setEntityStateTypeUrl(entityStateUrl.value())
                                        .setRead(EntityId.newBuilder()
                                                         .setId(Identifier.pack(id)))
                                        .build();
            final EntityRecord result = channel.getStub()
                                               .read(req);
            return result;
        }
    }

    /**
     * Writes {@link EntityRecord}s via in-process gRPC service.
     */
    private static class InMemWriteFn<I> extends WriteFn<I> {

        private static final long serialVersionUID = 0L;
        private final String boundedContextName;
        private final TypeUrl entityStateUrl;
        private transient RecordStorageServiceChannel channel;

        private InMemWriteFn(String boundedContextName, TenantId tenantId, TypeUrl entityStateUrl) {
            super(tenantId);
            this.boundedContextName = boundedContextName;
            this.entityStateUrl = entityStateUrl;
        }

        @StartBundle
        public void startBundle() {
            channel = new RecordStorageServiceChannel(boundedContextName);
            channel.open();
        }

        @FinishBundle
        public void finishBundle() {
            channel.shutDown();
        }

        @Override
        protected void doWrite(TenantId tenantId, I key, EntityRecord record) {
            final RecordStorageRequest req =
                    RecordStorageRequest.newBuilder()
                                        .setTenantId(tenantId)
                                        .setEntityStateTypeUrl(entityStateUrl.value())
                                        .setWrite(record)
                                        .build();
            channel.getStub()
                   .write(req);
        }
    }

    /**
     * Finds entity records by {@link EntityFilters} via in-process gRPC service.
     */
    private static class InMemFindFn extends FindFn {

        private static final long serialVersionUID = 0L;
        private final String boundedContextName;
        private final TypeUrl entityStateUrl;
        private transient RecordStorageServiceChannel channel;

        private InMemFindFn(String boundedContextName, TenantId tenantId, TypeUrl entityStateUrl) {
            super(tenantId);
            this.boundedContextName = boundedContextName;
            this.entityStateUrl = entityStateUrl;
        }

        @StartBundle
        public void startBundle() {
            channel = new RecordStorageServiceChannel(boundedContextName);
            channel.open();
        }

        @FinishBundle
        public void finishBundle() {
            channel.shutDown();
        }

        @Override
        protected Iterator<EntityRecord> doFind(TenantId tenantId, EntityFilters filters) {
            final RecordStorageRequest req =
                    RecordStorageRequest.newBuilder()
                                        .setTenantId(tenantId)
                                        .setEntityStateTypeUrl(entityStateUrl.value())
                                        .setQuery(filters)
                                        .build();
            final Iterator<EntityRecord> iterator = channel.getStub()
                                                           .find(req);
            return iterator;
        }
    }
}
