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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.FieldMask;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.storage.EntityRecordWithColumns;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.storage.RecordStorageIO;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.users.TenantId;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Litus
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
class InMemoryRecordStorage<I> extends RecordStorage<I> {

    private final MultitenantStorage<TenantRecords<I>> multitenantStorage;

    InMemoryRecordStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<TenantRecords<I>>(multitenant) {
            @Override
            TenantRecords<I> createSlice() {
                return new TenantRecords<>();
            }
        };
    }

    protected static <I> InMemoryRecordStorage<I> newInstance(boolean multitenant) {
        return new InMemoryRecordStorage<>(multitenant);
    }

    @Override
    public Iterator<I> index() {
        return getStorage().index();
    }

    @Override
    public boolean delete(I id) {
        return getStorage().delete(id);
    }

    @Override
    protected Optional<EntityRecord> readRecord(I id) {
        return getStorage().get(id);
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(Iterable<I> ids) {
        return readMultipleRecords(ids, FieldMask.getDefaultInstance());
    }

    @Override
    protected Iterable<EntityRecord> readMultipleRecords(final Iterable<I> givenIds,
                                                         FieldMask fieldMask) {
        final TenantRecords<I> storage = getStorage();

        // It is not possible to return an immutable collection,
        // since {@code null} may be present in it.
        final Collection<EntityRecord> result = new LinkedList<>();

        for (I givenId : givenIds) {
            final EntityRecord matchingResult = storage.findAndApplyFieldMask(givenId, fieldMask);
            result.add(matchingResult);
        }
        return result;
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords() {
        return getStorage().readAllRecords();
    }

    @Override
    protected Map<I, EntityRecord> readAllRecords(FieldMask fieldMask) {
        return getStorage().readAllRecords(fieldMask);
    }

    @Override
    protected void writeRecord(I id, EntityRecordWithColumns record) {
        getStorage().put(id, record.getRecord());
    }

    @Override
    protected void writeRecords(Map<I, EntityRecordWithColumns> records) {
        final TenantRecords<I> storage = getStorage();
        for (Map.Entry<I, EntityRecordWithColumns> record : records.entrySet()) {
            storage.put(record.getKey(), record.getValue()
                                               .getRecord());
        }
    }

    private TenantRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    /*
     * Beam support
     ******************/

    @Override
    public RecordStorageIO<I> getIO() {
        return new BeamIO<>(this);
    }

    private static class BeamIO<I> extends RecordStorageIO<I> {

        private final InMemoryRecordStorage<I> storage;

        private BeamIO(InMemoryRecordStorage<I> storage) {
            this.storage = storage;
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
            return new AsTransform<>(tenantId, query, records.build());
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

        private static  <I> Map<I, EntityRecord> filter(Map<I, EntityRecord> map, Query<I> query) {
            final ImmutableMap.Builder<I, EntityRecord> filtered = ImmutableMap.builder();
            final Predicate<KV<I, EntityRecord>> predicate = query.toPredicate();
            for (Map.Entry<I, EntityRecord> entry : map.entrySet()) {
                if (predicate.apply(KV.of(entry.getKey(), entry.getValue()))) {
                    filtered.put(entry);
                }
            }
            return filtered.build();
        }

        private static class InMemFindByQuery<I> extends RecordStorageIO.FindByQuery<I> {

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

            private AsTransform(TenantId tenantId,
                                Query<I> query,
                                ImmutableList<KV<I, EntityRecord>> records) {
                super(StaticValueProvider.of(tenantId), query);
                this.records = records;
            }

            @Override
            public PCollection<KV<I, EntityRecord>> expand(PBegin input) {
                final PCollection<KV<I, EntityRecord>> result = input.apply(Create.of(records));
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
}
