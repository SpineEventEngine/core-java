/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.FieldMasks;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.RecordStorage;
import org.spine3.server.users.CurrentTenant;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Memory-based implementation of {@link RecordStorage}.
 *
 * @author Alexander Litus, Alex Tymchenko
 */
/* package */ class InMemoryRecordStorage<I> extends RecordStorage<I> {

    /** A stub instance of {@code TenantId} to be used by the storage in single-tenant context. */
    private static final TenantId singleTenant = TenantId.newBuilder()
                                                         .setValue("SINGLE_TENANT")
                                                         .build();

    private final Map<TenantId, Map<I, EntityStorageRecord>> tenantToStorageMap = newHashMap();

    protected InMemoryRecordStorage(boolean multitenant) {
        super(multitenant);
    }

    @SuppressWarnings("MethodWithMultipleLoops")    /* It's OK for in-memory implementation
                                                     * as it is used primarily in tests. */
    @Override
    protected Iterable<EntityStorageRecord> readBulkInternal(final Iterable<I> givenIds, FieldMask fieldMask) {
        final Map<I, EntityStorageRecord> storage = getStorage();

        // It is not possible to return an immutable collection, since {@code null} may be present in it.
        final Collection<EntityStorageRecord> result = new LinkedList<>();

        TypeUrl typeUrl = null;

        int resultExpectedSize = 0;

        for (I givenId : givenIds) {
            resultExpectedSize++;

            for (I recordId : storage.keySet()) {
                if (recordId.equals(givenId)) {
                    final EntityStorageRecord.Builder matchingRecord = storage.get(recordId).toBuilder();
                    final Any state = matchingRecord.getState();

                    if (typeUrl == null) {
                        typeUrl = TypeUrl.of(state.getTypeUrl());
                    }

                    final Message wholeState = AnyPacker.unpack(state);
                    final Message maskedState = FieldMasks.applyIfValid(fieldMask, wholeState, typeUrl);
                    final Any processed = AnyPacker.pack(maskedState);

                    matchingRecord.setState(processed);

                    result.add(matchingRecord.build());
                }
            }

            // If no record was found
            if (result.size() < resultExpectedSize) {
                result.add(null);
                resultExpectedSize++;
            }
        }
        return result;
    }

    @Override
    protected Iterable<EntityStorageRecord> readBulkInternal(Iterable<I> ids) {
        return readBulkInternal(ids, FieldMask.getDefaultInstance());
    }

    @Override
    protected Map<I, EntityStorageRecord> readAllInternal() {
        final Map<I, EntityStorageRecord> storage = getStorage();

        final ImmutableMap<I, EntityStorageRecord> result = ImmutableMap.copyOf(storage);
        return result;
    }

    @Override
    protected Map<I, EntityStorageRecord> readAllInternal(FieldMask fieldMask) {
        if (fieldMask.getPathsList().isEmpty()) {
            return readAllInternal();
        }

        final Map<I, EntityStorageRecord> storage = getStorage();

        if (storage.isEmpty()) {
            return newHashMap();
        }

        final ImmutableMap.Builder<I, EntityStorageRecord> result = ImmutableMap.builder();

        final Collection<Message> records = FieldMasks.applyMask(fieldMask, Collections2.transform(storage.values(), new Function<EntityStorageRecord, Message>() {
            @Nullable
            @Override
            public Message apply(@Nullable EntityStorageRecord input) {
                return input == null ? null : AnyPacker.unpack(input.getState());
            }
        }), TypeUrl.of(storage.entrySet().iterator().next().getValue().getState().getTypeUrl()));

        final Iterator<Message> messageIterator = records.iterator();
        for (I key : storage.keySet()) {
            result.put(key, storage.get(key).toBuilder().setState(AnyPacker.pack(messageIterator.next())).build());
        }

        return result.build();
    }

    protected static <I> InMemoryRecordStorage<I> newInstance(boolean multitenant) {
        return new InMemoryRecordStorage<>(multitenant);
    }

    private Map<I, EntityStorageRecord> getStorage() {
        final TenantId tenantId = isMultitenant() ? CurrentTenant.get() : singleTenant;
        checkState(tenantId != null, "Current tenant is null");

        Map<I, EntityStorageRecord> storage = tenantToStorageMap.get(tenantId);
        if (storage == null) {
            storage = newHashMap();
            tenantToStorageMap.put(tenantId, storage);
        }
        return storage;
    }

    @Override
    protected EntityStorageRecord readInternal(I id) {
        return getStorage().get(id);
    }

    @Override
    protected void writeInternal(I id, EntityStorageRecord record) {
        getStorage().put(id, record);
    }

    @Override
    public void close() throws Exception {
        super.close();
    }
}
