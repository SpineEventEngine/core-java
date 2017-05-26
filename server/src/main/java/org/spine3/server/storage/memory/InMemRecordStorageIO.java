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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.base.Identifier;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.server.storage.grpc.ProjectionStorageServiceGrpc;
import org.spine3.server.storage.grpc.ProjectionStorageServiceGrpc.ProjectionStorageServiceBlockingStub;
import org.spine3.server.storage.grpc.RecordStorageRequest;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.type.TypeUrl;
import org.spine3.users.TenantId;

import java.util.Iterator;
import java.util.Map;

import static org.spine3.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

/**
 * A {@link RecordStorageIO} for {@link InMemoryRecordStorage}.
 *
 * @author Alexander Yevsyukov
 */
class InMemRecordStorageIO<I> extends RecordStorageIO<I> {

    private final TypeUrl entityStateUrl;

    InMemRecordStorageIO(Class<I> idClass, TypeUrl entityStateUrl) {
        super(idClass);
        this.entityStateUrl = entityStateUrl;
    }

    /**
     * Creates a default channel for read/write operations to in-memory storages via
     * {@link ProjectionStorageService}.
     */
    static ManagedChannel createDefaultChannel() {
        return ManagedChannelBuilder.forAddress("localhost", DEFAULT_CLIENT_SERVICE_PORT)
                                    .build();
    }

    @Override
    public ReadFn<I> readFn(TenantId tenantId) {
        return new InMemReadFn<>(tenantId, entityStateUrl);
    }

    @Override
    public Find<I> find(TenantId tenantId, Query<I> query) {
        //TODO:2017-05-25:alexander.yevsyukov: Implement
        final Map<I, EntityRecord> all = null; // readAll(tenantId);
        final Map<I, EntityRecord> filtered = filter(all, query);
        final ImmutableList.Builder<KV<I, EntityRecord>> records = ImmutableList.builder();
        for (Map.Entry<I, EntityRecord> entry : filtered.entrySet()) {
            records.add(KV.of(entry.getKey(), entry.getValue()));
        }
        return new AsTransform<>(tenantId, query, records.build(), getKvCoder());
    }

    @Override
    public WriteFn<I> writeFn(TenantId tenantId) {
        return new InMemWriteFn<>(tenantId, entityStateUrl);
    }

    private static <I> ImmutableMap<I, EntityRecord> readAll(TenantId tenantId,
                                                             InMemoryRecordStorage<I> storage) {
        final TenantAwareFunction0<ImmutableMap<I, EntityRecord>> func =
                new ReadAllRecordsFunc<>(tenantId, storage);
        final ImmutableMap<I, EntityRecord> allRecords = func.execute();
        return allRecords;
    }

    private static <I> Map<I, EntityRecord> filter(Map<I, EntityRecord> map, Query<I> query) {
        final ImmutableMap.Builder<I, EntityRecord> filtered = ImmutableMap.builder();
        final Predicate<KV<I, EntityRecord>> predicate = query.toPredicate();
        for (Map.Entry<I, EntityRecord> entry : map.entrySet()) {
            if (predicate.apply(KV.of(entry.getKey(), entry.getValue()))) {
                filtered.put(entry);
            }
        }
        return filtered.build();
    }

    private static class InMemReadFn<I> extends ReadFn<I> {

        private static final long serialVersionUID = 3686720299328852578L;
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
            channel = createDefaultChannel();
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

    /**
     * Transforms passed records into {@link PCollection}.
     */
    private static class AsTransform<I> extends Find<I> {
        private static final long serialVersionUID = 0L;
        private final ImmutableList<KV<I, EntityRecord>> records;
        private final KvCoder<I, EntityRecord> kvCoder;

        private AsTransform(TenantId tenantId,
                            Query<I> query,
                            ImmutableList<KV<I, EntityRecord>> records,
                            KvCoder<I, EntityRecord> kvCoder) {
            super(ValueProvider.StaticValueProvider.of(tenantId), query);
            this.records = records;
            this.kvCoder = kvCoder;
        }

        @Override
        public PCollection<KV<I, EntityRecord>> expand(PBegin input) {
            final PCollection<KV<I, EntityRecord>> result =
                    input.apply(Create.of(records)
                                      .withCoder(kvCoder));
            return result;
        }
    }

    private static class ReadAllRecordsFunc<I>
            extends TenantAwareFunction0<ImmutableMap<I, EntityRecord>> {

        private final InMemoryRecordStorage<I> storage;

        private ReadAllRecordsFunc(TenantId tenantId, InMemoryRecordStorage<I> storage) {
            super(tenantId);
            this.storage = storage;
        }

        @Override
        public ImmutableMap<I, EntityRecord> apply() {
            ImmutableMap.Builder<I, EntityRecord> result = ImmutableMap.builder();
            final Iterator<I> index = storage.index();
            while (index.hasNext()) {
                I id = index.next();
                final EntityRecord record = storage.readRecord(id)
                                                   .get();
                result.put(id, record);
            }
            return result.build();
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

        @SuppressWarnings("unused") // called by Beam
        @StartBundle
        public void startBundle() {
            channel = createDefaultChannel();
            blockingStub = ProjectionStorageServiceGrpc.newBlockingStub(channel);
        }

        @SuppressWarnings("unused") // called by Beam
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
