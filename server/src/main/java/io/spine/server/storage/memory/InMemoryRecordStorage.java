/*
 * Copyright 2018, TeamDev. All rights reserved.
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
                          Class<? extends Entity> entityClass) {
        super(multitenant, entityClass);
        this.spec = spec;
        this.multitenantStorage = new MultitenantStorage<TenantRecords<I>>(multitenant) {
            @Override
            TenantRecords<I> createSlice() {
                return new TenantRecords<>();
            }
        };
    }

    protected static <I> InMemoryRecordStorage<I> newInstance(StorageSpec<I> spec,
                                                              boolean multitenant,
                                                              Class<? extends Entity> entityClass) {
        return new InMemoryRecordStorage<>(spec, multitenant, entityClass);
    }

    StorageSpec<I> getSpec() {
        return spec;
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
    protected Iterator<@Nullable EntityRecord> readMultipleRecords(Iterable<I> givenIds,
                                                                   FieldMask fieldMask) {
        TenantRecords<I> storage = getStorage();

        // It is not possible to return an immutable collection,
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
        return getStorage().readAllRecords();
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(FieldMask fieldMask) {
        return getStorage().readAllRecords(fieldMask);
    }

    @Override
    protected Iterator<EntityRecord> readAllRecords(EntityQuery<I> query, FieldMask fieldMask) {
        EntityQuery<I> completeQuery = toCompleteQuery(query);
        return getStorage().readAllRecords(completeQuery, fieldMask);
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
     * Otherwise, a new instance containing with active lifecycle attributes is returned.
     *
     * <p>If the type of the Entity which this repository works with is not derived from
     * the {@link io.spine.server.entity.EntityWithLifecycle}, then no lifecycle attributes are
     * appended and the {@code query} is returned as is.
     *
     * @param query
     *         the source {@link EntityQuery} to take the parameters from
     * @return an {@link EntityQuery} which includes
     *         the {@link io.spine.server.storage.LifecycleFlagField Lifecycle Flags Columns} 
     *         unless they are not supported
     */
    private EntityQuery<I> toCompleteQuery(EntityQuery<I> query) {
        if (isLifecycleSupported() && !query.isLifecycleAttributesSet()) {
            return query.withActiveLifecycle(this);
        }
        return query;
    }

    private TenantRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    @Override
    protected Optional<EntityRecord> readRecord(I id) {
        return getStorage().get(id)
                           .map(EntityRecordUnpacker.INSTANCE);
    }

    @Override
    protected void writeRecord(I id, EntityRecordWithColumns record) {
        getStorage().put(id, record);
    }

    @Override
    protected void writeRecords(Map<I, EntityRecordWithColumns> records) {
        TenantRecords<I> storage = getStorage();
        for (Map.Entry<I, EntityRecordWithColumns> record : records.entrySet()) {
            storage.put(record.getKey(), record.getValue());
        }
    }
}
