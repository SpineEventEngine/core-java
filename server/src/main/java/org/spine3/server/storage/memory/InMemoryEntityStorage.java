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

import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.users.CurrentTenant;
import org.spine3.users.TenantId;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Memory-based implementation of {@link EntityStorage}.
 *
 * @author Alexander Litus
 */
/* package */ class InMemoryEntityStorage<I> extends EntityStorage<I> {

    /** A stub instance of {@code TenantId} to be used by the storage in single-tenant context. */
    private static final TenantId singleTenant = TenantId.newBuilder().setValue("SINGLE_TENANT").build();

    private final Map<TenantId, Map<I, EntityStorageRecord>> tenantToStorageMap = newHashMap();

    protected InMemoryEntityStorage(boolean multitenant) {
        super(multitenant);
    }

    @SuppressWarnings("MethodWithMultipleLoops")    /* It's OK for in-memory implementation
                                                     * as it is used primarily in tests. */
    @Override
    protected Iterable<EntityStorageRecord> readBulkInternal(final Iterable<I> givenIds) {
        final Map<I, EntityStorageRecord> storage = getStorage();

        final Collection<EntityStorageRecord> result = new LinkedList<>();
        for (I recordId : storage.keySet()) {
            for (I givenId : givenIds) {
                if(recordId.equals(givenId)) {
                    final EntityStorageRecord matchingRecord = storage.get(recordId);
                    result.add(matchingRecord);
                    continue;
                }
                result.add(null);
            }
        }
        return result;
    }

    protected static <I> InMemoryEntityStorage<I> newInstance(boolean multitenant) {
        return new InMemoryEntityStorage<>(multitenant);
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
