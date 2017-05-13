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
import com.google.common.collect.Lists;
import com.google.protobuf.FieldMask;
import org.apache.beam.sdk.coders.protobuf.ProtoCoder;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.storage.EntityRecordWithColumns;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.tenant.TenantAwareFunction0;
import org.spine3.users.TenantId;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Litus
 * @author Alex Tymchenko
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

    @Override
    protected PTransform<PBegin, PCollection<EntityRecord>>
    readTransform(TenantId tenantId, final SerializableFunction<EntityRecord, Boolean> filter) {

        /**
         * The code below reads all records one by one (instead of obtaining them via
         * {@link readAllRecords()}) in the hope that Beam-based implementation would allow removing
         * {@link readAllRecords()} and {@link readAllRecords(FieldMask).
         */
        final TenantAwareFunction0<List<EntityRecord>> func =
                new TenantAwareFunction0<List<EntityRecord>>(tenantId) {
            @Override
            public List<EntityRecord> apply() {
                final List<I> index = Lists.newArrayList(index());
                final List<EntityRecord> result = Lists.newArrayListWithExpectedSize(index.size());
                for (I id : index) {
                    final EntityRecord record = readRecord(id).get();
                    if (filter.apply(record)) {
                        result.add(record);
                    }
                }
                return result;
            }
        };
        List<EntityRecord> records = func.apply();

        return new AsTransform(records);
    }

    private static class AsTransform extends PTransform<PBegin, PCollection<EntityRecord>> {

        private static final long serialVersionUID = 0L;
        private final ImmutableList<EntityRecord> records;

        private AsTransform(List<EntityRecord> records) {
            this.records = ImmutableList.copyOf(records);
        }

        @Override
        public PCollection<EntityRecord> expand(PBegin input) {
            final PCollection<EntityRecord> result =
                    input.apply(Create.of(records)
                                      .withCoder(ProtoCoder.of(EntityRecord.class)));
            return result;
        }

        @Override
        public String toString() {
            return "InMemoryRecordStorage.readTransform()";
        }
    }
}
