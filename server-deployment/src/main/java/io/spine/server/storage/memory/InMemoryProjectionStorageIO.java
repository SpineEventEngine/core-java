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
import io.spine.server.storage.memory.grpc.LastHandledEventRequest;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc;
import io.spine.server.storage.memory.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceBlockingStub;
import io.spine.users.TenantId;
import io.spine.util.Exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BeamIO operations for in-memory projection storage.
 *
 * @author Alexander Yevsyukov
 */
public class InMemoryProjectionStorageIO<I> extends ProjectionStorageIO<I> {

    private final StorageSpec<I> spec;
    private final RecordStorageIO<I> storageIO;

    private InMemoryProjectionStorageIO(StorageSpec<I> spec, RecordStorageIO<I> storageIO) {
        super(storageIO.getIdClass());
        this.spec = spec;
        this.storageIO = storageIO;
    }

    public static <I> InMemoryProjectionStorageIO<I> of(InMemoryProjectionStorage<I> storage) {
        final StorageSpec<I> spec = storage.getSpec();
        final RecordStorageIO<I> recordStorageIO = RecordStorageIO.of(storage.recordStorage());
        return new InMemoryProjectionStorageIO<>(spec, recordStorageIO);
    }

    @Override
    public WriteTimestampFn writeTimestampFn(TenantId tenantId) {
        return new WriteTimestampOverGrpc(tenantId, spec);
    }

    @Override
    public ReadFn<I> readFn(TenantId tenantId) {
        return storageIO.readFn(tenantId);
    }

    @Override
    public FindFn findFn(TenantId tenantId) {
        throw Exceptions.unsupported("Finding in ProjectionStorage is not supported");
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return storageIO.writeFn(tenantId);
    }

    private static class ProjectionStorageServiceChannel
            extends BoundedContextChannel<ProjectionStorageServiceBlockingStub> {

        ProjectionStorageServiceChannel(String boundedContextName) {
            super(boundedContextName);
        }

        @Override
        protected ProjectionStorageServiceBlockingStub createStub(ManagedChannel channel) {
            return ProjectionStorageServiceGrpc.newBlockingStub(channel);
        }
    }

    private static class WriteTimestampOverGrpc extends WriteTimestampFn {

        private static final long serialVersionUID = 0L;
        private final StorageSpec<?> spec;
        private transient ProjectionStorageServiceChannel channel;

        private WriteTimestampOverGrpc(TenantId tenantId, StorageSpec<?> spec) {
            super(tenantId);
            this.spec = spec;
        }

        @StartBundle
        public void startBundle() {
            channel = new ProjectionStorageServiceChannel(spec.getBoundedContextName());
            channel.open();
        }

        @FinishBundle
        public void finishBundle() {
            channel.shutDown();
        }

        @Override
        protected void doWrite(TenantId tenantId, Timestamp timestamp) {
            checkNotNull(tenantId);
            checkNotNull(timestamp);
            final LastHandledEventRequest req =
                    LastHandledEventRequest.newBuilder()
                                           .setTenantId(tenantId)
                                           .setProjectionStateTypeUrl(spec.getEntityStateUrl()
                                                                          .value())
                                           .setTimestamp(timestamp)
                                           .build();
            channel.getStub()
                   .writeLastHandledEventTimestamp(req);
        }
    }
}
