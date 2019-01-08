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

package io.spine.server.tenant;

import io.spine.core.TenantId;
import io.spine.server.storage.StorageFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The index of tenant IDs in a multi-tenant application.
 *
 * @author Alexander Yevsyukov
 */
public interface TenantIndex extends AutoCloseable {

    /**
     * Stores the passed tenant ID in the index.
     */
    void keep(TenantId id);

    /**
     * Obtains the set of all stored tenant IDs.
     */
    Set<TenantId> getAll();

    /**
     * Closes the index for further read or write operations.
     *
     * <p>Implementations may throw specific exceptions.
     */
    @Override
    void close();

    /**
     * Creates default implementation of {@code TenantIndex} for multi-tenant context.
     *
     * <p>Storage of {@code TenantIndex} data is performed in single-tenant context, and a
     * {@linkplain StorageFactory#toSingleTenant() single-tenant} version of the passed storage
     * factory. Therefore, it is safe to pass both single-tenant and multi-tenant storage
     * factories to this method as long as the passed factory implements
     * {@link StorageFactory#toSingleTenant()}.
     */
    static TenantIndex createDefault(StorageFactory storageFactory) {
        checkNotNull(storageFactory);
        @SuppressWarnings("ClassReferencesSubclass") // OK for this default impl.
        DefaultTenantRepository tenantRepo = new DefaultTenantRepository();
        tenantRepo.initStorage(storageFactory);
        return tenantRepo;
    }

    /**
     * Obtains a {@code TenantIndex} to be used in single-tenant context.
     */
    static TenantIndex singleTenant() {
        return SingleTenantIndex.INSTANCE;
    }
}
