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

import org.spine3.server.users.CurrentTenant;
import org.spine3.users.TenantId;

import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

/**
 * The multitenant storage
 *
 * @param <S> the type of the storage "slice" for each tenant
 * @author Alexander Yevsyukov
 */
abstract class MultitenantStorage<S extends TenantStorage<?, ?>> {

    /** A stub instance of {@code TenantId} to be used by the storage in single-tenant context. */
    private static final TenantId singleTenant = TenantId.newBuilder()
                                                         .setValue("SINGLE_TENANT")
                                                         .build();
    /** The map from {@code TenantId} to its slice of data. */
    private final Map<TenantId, S> tenantSlices = newHashMap();

    /** If {@code true} the storage will contain a data slice for each tenant. */
    private final boolean multitenant;

    MultitenantStorage(boolean multitenant) {
        this.multitenant = multitenant;
    }

    /**
     * Obtains the data slice for the current tenant.
     *
     * <p>If the slice has not been created for this tenant, it will be created.
     */
    S getStorage() {
        final TenantId tenantId = isMultitenant() ? CurrentTenant.get() : singleTenant;
        checkState(tenantId != null, "Current tenant is null");

        S storage = tenantSlices.get(tenantId);
        if (storage == null) {
            storage = createSlice();
            tenantSlices.put(tenantId, storage);
        }
        return storage;
    }

    abstract S createSlice();

    boolean isMultitenant() {
        return multitenant;
    }
}
