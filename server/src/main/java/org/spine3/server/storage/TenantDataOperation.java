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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.spine3.users.TenantId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.validate.Validate.isDefault;

/**
 * An abstract base for operations on a tenant data.
 *
 * Such an operation goes through the following steps:
 * <ol>
 *     <li>The current tenant ID is obtained and remembered.
 *     <li>The tenant ID passed to the constructor is set as current.
 *     <li>The {@link #run()} method is called.
 *     <li>The previous tenant ID is set as current.
 * </ol>
 *
 * @author Alexander Yevsyukov
 */
public abstract class TenantDataOperation implements Runnable {

    private final TenantId tenantId;

    /**
     * Creates an instance for the operation for the tenant specified
     * by the passed ID.
     *
     * <p>If default instance of {@link TenantId} is passed (because
     * the application works in a single-tenant mode, the value
     * returned by {@link CurrentTenant#singleTenant()} will be substituted.
     *
     * @param tenantId the tenant ID or default value
     */
    protected TenantDataOperation(TenantId tenantId) {
        checkNotNull(tenantId);
        this.tenantId = isDefault(tenantId)
                        ? CurrentTenant.singleTenant()
                        : tenantId;
    }

    @VisibleForTesting
    TenantId tenantId() {
        return tenantId;
    }

    public void execute() {
        final Optional<TenantId> remembered = CurrentTenant.get();
        try {
            CurrentTenant.set(tenantId());
            run();
        } finally {
            if (remembered.isPresent()) {
                CurrentTenant.set(remembered.get());
            } else {
                CurrentTenant.clear();
            }
        }
    }
}
