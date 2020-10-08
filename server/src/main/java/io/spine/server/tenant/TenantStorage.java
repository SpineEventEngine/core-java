/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.spine.annotation.SPI;
import io.spine.core.TenantId;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.RecordStorage;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base for storage storing information about tenants.
 *
 * @param <T>
 *         the type of data associated with the tenant ID
 */
@SPI
public abstract class TenantStorage<T extends Message>
        extends MessageStorage<TenantId, T>
        implements TenantIndex {

    private final Set<TenantId> cache = Sets.newConcurrentHashSet();

    protected TenantStorage(RecordStorage<TenantId, T> delegate) {
        super(delegate);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If there is an entity with the passed ID, the method quits. Otherwise,
     * a new entity with the default state will be created and stored.
     *
     * @param id
     *         the tenant ID to store
     */
    @Override
    public final void keep(TenantId id) {
        if (cache.contains(id)) {
            return;
        }

        Optional<T> optional = read(id);
        if (!optional.isPresent()) {
            T newRecord = create(id);
            write(id, newRecord);
        }
        cache(id);
    }

    protected abstract T create(TenantId id);

    private void cache(TenantId id) {
        cache.add(id);
    }

    @VisibleForTesting
    final boolean cached(TenantId id) {
        return cache.contains(id);
    }

    /**
     * Removes the passed value from the in-memory cache of known tenant IDs.
     *
     * <p>Implementations should call this method for removing the cached value
     * for a tenant for which the record was removed from the repository.
     *
     * @param id
     *         the ID to remove from the cache
     * @return {@code true} if the value was cached before and removed, {@code false} otherwise
     */
    protected final boolean unCache(TenantId id) {
        boolean result = cache.remove(id);
        return result;
    }

    /**
     * Clears the cache of known tenant IDs.
     */
    protected final void clearCache() {
        cache.clear();
    }

    @Override
    public final Set<TenantId> all() {
        Iterator<TenantId> index = index();
        Set<TenantId> result = ImmutableSet.copyOf(index);
        cache.addAll(result);
        return result;
    }
}
