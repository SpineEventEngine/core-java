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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.spine.annotation.Internal;
import io.spine.core.TenantId;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.validate.Validate.isDefault;

/**
 * An abstract base for operations performed in a tenant context.
 *
 * @author Alexander Yevsyukov
 */
abstract class TenantAware {

    private final TenantId tenantId;

    /**
     * Creates an instance, which uses the {@code TenantId}
     * set in the current non-command handling execution context.
     *
     * @throws IllegalStateException if there is no current {@code TenantId}
     * @see CurrentTenant#ensure()
     */
    TenantAware() throws IllegalStateException {
        this(CurrentTenant.ensure());
    }

    /**
     * Creates an instance for the tenant specified by the passed ID.
     *
     * <p>If a default instance of {@link TenantId} is passed (because the application works in
     * a single-tenant mode, {@linkplain CurrentTenant#singleTenant() singleTenant()} value will be
     * substituted.
     *
     * @param tenantId the tenant ID or {@linkplain TenantId#getDefaultInstance() default value}
     */
    TenantAware(TenantId tenantId) {
        checkNotNull(tenantId);
        this.tenantId = isDefault(tenantId)
                        ? CurrentTenant.singleTenant()
                        : tenantId;
    }

    /**
     * Verifies whether a current tenant is set in the execution context.
     */
    @Internal
    @VisibleForTesting
    public static boolean isTenantSet() {
        Optional<TenantId> currentTenant = CurrentTenant.get();
        return currentTenant.isPresent();
    }

    /**
     * Obtains current tenant ID.
     *
     * <p>In the multi-tenant context obtains the currently set tenant ID.
     *
     * <p>In single-tenant context, returns {@link CurrentTenant#singleTenant() singleTenant()}
     *
     * @param multitenantContext {@code true} if execution context is multi-tenant
     * @return current tenant ID or {@link CurrentTenant#singleTenant() singleTenant()}
     * @throws IllegalStateException if there is no current tenant set in a multi-tenant context
     */
    static TenantId getCurrentTenant(boolean multitenantContext) {
        if (!multitenantContext) {
            return CurrentTenant.singleTenant();
        }
        return CurrentTenant.ensure();
    }

    @VisibleForTesting
    TenantId tenantId() {
        return tenantId;
    }
}
