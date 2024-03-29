/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.core.TenantId;

import java.util.Set;

/**
 * A null-object implementation of {@code TenantIndex} for being used in single-tenant
 * execution context.
 */
enum SingleTenantIndex implements TenantIndex {

    INSTANCE;

    /**
     * A stub instance of {@code TenantId} to be used by the storage in single-tenant context.
     */
    private static final TenantId singleTenant =
            TenantId.newBuilder()
                    .setValue("SINGLE_TENANT")
                    .build();

    private static final ImmutableSet<TenantId> index = ImmutableSet.of(singleTenant);

    /**
     * Returns a constant for single-tenant applications.
     */
    static TenantId tenantId() {
        return singleTenant;
    }

    @Override
    public void keep(TenantId id) {
        // Do nothing.
    }

    @Override
    public Set<TenantId> all() {
        return index;
    }

    @Override
    public void close() {
        // Do nothing.
    }
}
