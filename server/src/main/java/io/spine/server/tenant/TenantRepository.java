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

package io.spine.server.tenant;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.spine.core.TenantId;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.DefaultRecordBasedRepository;
import io.spine.server.storage.Storage;
import io.spine.server.storage.StorageFactory;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Abstract base for repositories storing information about tenants.
 *
 * @param <T> the type of data associated with the tenant ID
 * @author Alexander Yevsyukov
 */
public abstract class TenantRepository<T extends Message, E extends TenantRepository.Entity<T>>
        extends DefaultRecordBasedRepository<TenantId, E, T>
        implements TenantIndex {

    private final Set<TenantId> cache = Sets.newConcurrentHashSet();

    @Override
    public void initStorage(StorageFactory factory) {
        super.initStorage(factory.toSingleTenant());
    }

    /**
     * {@inheritDoc}
     *
     * <p>If there is an entity with the passed ID, the method quits. Otherwise,
     * a new entity with the default state will be created and stored.
     *
     * @param id the tenant ID to store
     */
    @Override
    public void keep(TenantId id) {
        if (cache.contains(id)) {
            return;
        }

        Optional<E> optional = find(id);
        if (!optional.isPresent()) {
            E newEntity = create(id);
            store(newEntity);
        }
        cache(id);
    }

    private void cache(TenantId id) {
        cache.add(id);
    }

    /**
     * Removes the passed value from the in-memory cache of known tenant IDs.
     *
     * <p>Implementations should call this method for removing the cached value
     * for a tenant, which record was removed from the repository.
     *
     * @param id the ID to remove from the cache
     * @return {@code true} if the value was cached before and removed, {@code false} otherwise
     */
    protected boolean unCache(TenantId id) {
        boolean result = cache.remove(id);
        return result;
    }

    /**
     * Clears the cache of known tenant IDs.
     */
    protected void clearCache() {
        cache.clear();
    }

    @Override
    public Set<TenantId> getAll() {
        Storage<TenantId, ?, ?> storage = getStorage();
        Iterator<TenantId> index = storage != null
                                   ? storage.index()
                                   : null;
        Set<TenantId> result = index != null
                               ? ImmutableSet.copyOf(index)
                               : ImmutableSet.of();
        cache.addAll(result);
        return result;
    }

    /**
     * Stores data associated with a tenant ID.
     *
     * @param <T> the type of the data associated with the tenant ID
     */
    public static class Entity<T extends Message> extends AbstractEntity<TenantId, T> {

        protected Entity(TenantId id) {
            super(id);
        }

        protected Entity(TenantId id, Function<TenantId, T> defaultState) {
            super(id, defaultState);
        }
    }
}
