/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.Lists;
import com.google.protobuf.FieldMask;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordStorage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @param <I>
 *         the type of entity IDs
 */
public class InMemoryRecordStorage<I> extends RecordStorage<I> {

    private final StorageSpec<I> spec;
    private final MultitenantStorage<TenantRecords<I>> multitenantStorage;

    InMemoryRecordStorage(StorageSpec<I> spec, boolean multitenant, 
                          Class<? extends Entity<?, ?>> entityClass) {
        super(multitenant, entityClass);
        this.spec = spec;
        this.multitenantStorage = new MultitenantStorage<TenantRecords<I>>(multitenant) {
            @Override
            TenantRecords<I> createSlice() {
                return new TenantRecords<>();
            }
        };
    }

    StorageSpec<I> spec() {
        return spec;
    }

    @Override
    public Iterator<I> index() {
        return records().index();
    }

    @Override
    public boolean delete(I id) {
        return records().delete(id);
    }

    @Override
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> givenIds,
                                                                   FieldMask fieldMask) {
        TenantRecords<I> storage = records();

        // It is impossible to return an immutable collection,
        // since null may be present in it.
        Collection<EntityRecord> result = Lists.newLinkedList();

        for (I givenId : givenIds) {
            EntityRecord matchingResult = storage.findAndApplyFieldMask(givenId, fieldMask);
            result.add(matchingResult);
        }
        return result.iterator();
    }

    @Override
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> ids) {
        return readMultipleRecords(ids, FieldMask.getDefaultInstance());
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords() {
        return records().readAll();
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(FieldMask fieldMask) {
        return records().readAll(fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        EntityQuery<I> completeQuery = toCompleteQuery(query);
        return records().readAll(completeQuery, fieldMask);
    }

    /**
     * Creates an {@link EntityQuery} instance which has:
     * <ul>
     *     <li>all the parameters from the {@code src} query;
     *     <li>at least one parameter limiting the 
     *     {@link io.spine.server.storage.LifecycleFlagField Lifecycle Flags Columns}.
     * </ul>
     *
     * <p>If the {@code query} instance {@linkplain EntityQuery#isLifecycleAttributesSet() 
     * contains the lifecycle attributes}, then it is returned without any changes. 
     * Otherwise, a new instance containing active lifecycle attributes is returned.
     *
     * @param query
     *         the source {@link EntityQuery} to take the parameters from
     * @return an {@link EntityQuery} which includes
     *         the {@link io.spine.server.storage.LifecycleFlagField Lifecycle Flags Columns} 
     *         unless they are not supported
     */
    private EntityQuery<I> toCompleteQuery(EntityQuery<I> query) {
        if (!query.isLifecycleAttributesSet()) {
            return query.withActiveLifecycle(this);
        }
        return query;
    }

    private TenantRecords<I> records() {
        return multitenantStorage.currentSlice();
    }

    @Override
    protected Optional<EntityRecord> readRecord(I id) {
        return records().get(id)
                        .map(EntityRecordUnpacker.INSTANCE);
    }

    @Override
    protected void writeRecord(I id, EntityRecordWithColumns record) {
        records().put(id, record);
    }

    @Override
    protected void writeRecords(Map<I, EntityRecordWithColumns> records) {
        TenantRecords<I> storage = records();
        for (Map.Entry<I, EntityRecordWithColumns> record : records.entrySet()) {
            storage.put(record.getKey(), record.getValue());
        }
    }
}
