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

package org.spine3.server.tenant;

import org.spine3.annotations.Internal;
import org.spine3.users.TenantId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An operation which runs for all tenants registered in the passed {@link TenantIndex}.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public abstract class AllTenantOperation implements Runnable {

    private final TenantIndex tenantIndex;

    /**
     * Creates a new instance of an operation that will be {@linkplain #execute()} executed}
     * for all tenants in the passed index.
     */
    protected AllTenantOperation(TenantIndex tenantIndex) {
        checkNotNull(tenantIndex);
        this.tenantIndex = tenantIndex;
    }

    /**
     * Executes {@linkplain #run() an operation} for all tenants.
     */
    public void execute() {
        for (TenantId tenantId : tenantIndex.getAll()) {
            executeForTenant(tenantId);
        }
    }

    private void executeForTenant(TenantId tenantId) {
        final TenantAwareOperation op = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                AllTenantOperation.this.run();
            }
        };
        op.execute();
    }
}
