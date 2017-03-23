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

package org.spine3.server.storage;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import org.spine3.server.entity.AbstractEntity;
import org.spine3.server.entity.DefaultRecordBasedRepository;
import org.spine3.users.TenantId;

import java.util.Iterator;
import java.util.Set;

/**
 * Abstract base for repositories storing information about tenants.
 *
 * @param <T> the type of data associated with the tenant ID
 * @author Alexander Yevsyukov
 */
public abstract class TenantRepository<T extends Message, E extends TenantRepository.Entity<T>>
        extends DefaultRecordBasedRepository<TenantId, E, T>
        implements TenantIndex {

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the default behaviour because {@link TenantRepository.Entity
     * TenantRepository.Entity} does not have a generic parameter for the ID.
     */
    @Override
    protected Class<TenantId> getIdClass() {
        return TenantId.class;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If there is an entity with the passed ID, the method quites. Otherwise,
     * a new entity with the default state will be created and stored.
     *
     * @param id the tenant ID to store
     */
    @Override
    public void keep(TenantId id) {
        final Optional<E> optional = load(id);
        if (!optional.isPresent()) {
            final E newEntity = create(id);
            store(newEntity);
        }
    }

    @Override
    public Set<TenantId> getAll() {
        final Storage<TenantId, ?> storage = getStorage();
        final Iterator<TenantId> index = storage != null
                                         ? storage.index()
                                         : null;
        final Set<TenantId> result = index != null
                                     ? ImmutableSet.copyOf(index)
                                     : ImmutableSet.<TenantId>of();
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
    }
}
