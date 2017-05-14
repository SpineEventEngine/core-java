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
import com.google.common.collect.ImmutableList;
import com.google.protobuf.FieldMask;
import org.apache.beam.sdk.coders.protobuf.ProtoCoder;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.storage.EntityRecordWithColumns;
import org.spine3.server.storage.ReadRecords;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.users.TenantId;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.builder;
import static com.google.common.collect.ImmutableList.copyOf;

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

    protected InMemoryRecordStorage(boolean multitenant) {
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
     */

    @Override
    public RecordStorage.BeamIO<I> getIO() {
        return new BeamIO<>(this);
    }

    private static class BeamIO<I> extends RecordStorage.BeamIO<I> {

        private final InMemoryRecordStorage<I> storage;

        private BeamIO(InMemoryRecordStorage<I> storage) {
            this.storage = storage;
        }

        @Override
        public ReadRecords readAll(TenantId tenantId) {
            final List<I> index = readIndex();
            final ImmutableList<EntityRecord> records = readMany(tenantId, index);
            return new AsTransform(records);
        }

        @Override
        public ReadRecords read(TenantId tenantId, Iterable<I> ids) {
            final ImmutableList<EntityRecord> records = readMany(tenantId, ids);
            return new AsTransform(records);
        }

        private ImmutableList<EntityRecord> readMany(final TenantId tenantId,
                                                     final Iterable<I> index) {
            final TenantAwareFunction0<ImmutableList<EntityRecord>> func =
                    new TenantAwareFunction0<ImmutableList<EntityRecord>>(tenantId) {
                        @Override
                        public ImmutableList<EntityRecord> apply() {
                            final ImmutableList.Builder<EntityRecord> records = builder();
                            for (I id : index) {
                                final EntityRecord record = storage.readRecord(id).get();
                                records.add(record);
                            }
                            return records.build();
                        }
                    };
            return func.execute();
        }

        private ImmutableList<I> readIndex() {
            final TenantAwareFunction0<ImmutableList<I>> func =
                    new TenantAwareFunction0<ImmutableList<I>>() {
                        @Override
                        public ImmutableList<I> apply() {
                            return copyOf(storage.index());
                        }
                    };
            return func.execute();
        }

        /**
         * Transforms passed records into {@link PCollection}.
         */
        private static class AsTransform extends ReadRecords {
            private static final long serialVersionUID = 0L;
            private final ImmutableList<EntityRecord> records;

            private AsTransform(ImmutableList<EntityRecord> records) {
                this.records = records;
            }

            @Override
            public PCollection<EntityRecord> expand(PBegin input) {
                final PCollection<EntityRecord> result =
                        input.apply(Create.of(records)
                                          .withCoder(ProtoCoder.of(EntityRecord.class)));
                return result;
            }
        }
    }
}
