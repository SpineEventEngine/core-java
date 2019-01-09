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

package io.spine.testing.server.tenant;

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import io.spine.base.Environment;
import io.spine.core.TenantId;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareTestSupport;
import io.spine.server.tenant.TenantFunction;
import io.spine.server.tenant.TenantIndex;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Abstract base for test suites that test tenant-aware functionality.
 *
 * <p>This class must be used only from {@linkplain Environment#isTests() test execution context}.
 *
 * @author Alexander Yevsyukov
 */
@Internal
@VisibleForTesting
public abstract class TenantAwareTest {

    public static
    TenantIndex createTenantIndex(boolean multitenant, StorageFactory storageFactory) {
        return multitenant
               ? TenantIndex.createDefault(storageFactory)
               : TenantIndex.singleTenant();
    }

    /**
     * Sets the current tenant ID to the passed value.
     *
     * <p>Call this method in a set-up method of the derived test suite class.
     */
    protected void setCurrentTenant(TenantId tenantId) {
        checkNotNull(tenantId);
        checkInTests();
        TenantAwareTestSupport.inject(tenantId);
    }

    /**
     * Clears the current tenant ID.
     *
     * <p>Call this method in a clean-up method of the derived test suite class.
     */
    protected void clearCurrentTenant() {
        checkInTests();
        TenantAwareTestSupport.clear();
    }

    /**
     * Obtains current tenant ID.
     *
     * @throws IllegalStateException
     *         if the current tenant ID was {@linkplain #setCurrentTenant(TenantId) not set}, or
     *         already {@linkplain #clearCurrentTenant() cleared}
     */
    protected TenantId tenantId() {
        return currentTenant();
    }

    private static void checkInTests() {
        checkState(Environment.getInstance()
                              .isTests());
    }

    private static TenantId currentTenant() {
        TenantId result = new TenantFunction<TenantId>(true) {
            @Override
            public TenantId apply(TenantId id) {
                return id;
            }
        }.execute();
        return checkNotNull(result);
    }
}
