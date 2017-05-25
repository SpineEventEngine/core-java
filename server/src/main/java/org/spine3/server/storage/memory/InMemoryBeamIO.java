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
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.storage.EntityRecordWithColumns;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.users.TenantId;

import java.util.Iterator;
import java.util.Map;

import static org.spine3.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

/**
 * A {@link RecordStorageIO} for {@link InMemoryRecordStorage}.
 *
 * @author Alexander Yevsyukov
 */
class InMemoryBeamIO<I> extends RecordStorageIO<I> {

    private final InMemoryRecordStorage<I> storage;

    InMemoryBeamIO(Class<I> idClass, InMemoryRecordStorage<I> storage) {
        super(idClass);
        this.storage = storage;
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
    public FindByQuery<I> findFn(TenantId tenantId) {
        return new InMemFindByQuery<>(readAll(tenantId));
    }

    @Override
    public Read<I> read(TenantId tenantId, Query<I> query) {
        final Map<I, EntityRecord> all = readAll(tenantId);
        final Map<I, EntityRecord> filtered = filter(all, query);
        final ImmutableList.Builder<KV<I, EntityRecord>> records = ImmutableList.builder();
        for (Map.Entry<I, EntityRecord> entry : filtered.entrySet()) {
            records.add(KV.of(entry.getKey(), entry.getValue()));
        }
        return new AsTransform<>(tenantId, query, records.build(), getKvCoder());
    }

    @Override
    @SuppressWarnings("SerializableInnerClassWithNonSerializableOuterClass")
        /* OK for in-memory test-only implementation. */
    public WriteFn<I> writeFn(TenantId tenantId) {
        return new WriteFn<I>(tenantId) {
            private static final long serialVersionUID = 0L;

            @Override
            protected void doWrite(I key, EntityRecord value) {
                storage.writeRecord(key, EntityRecordWithColumns.of(value));
            }
        };
    }

    private ImmutableMap<I, EntityRecord> readAll(TenantId tenantId) {
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

    private static class InMemFindByQuery<I> extends FindByQuery<I> {

        private static final long serialVersionUID = 0L;
        private final ImmutableMap<I, EntityRecord> records;

        private InMemFindByQuery(ImmutableMap<I, EntityRecord> records) {
            this.records = records;
        }

        @Override
        public Iterable<EntityRecord> apply(Query<I> input) {
            return filter(records, input).values();
        }
    }

    /**
     * Transforms passed records into {@link PCollection}.
     */
    private static class AsTransform<I> extends Read<I> {
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
}
