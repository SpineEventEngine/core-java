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
import io.spine.annotation.Internal;
import io.spine.base.Environment;
import io.spine.core.TenantId;
import io.spine.server.storage.StorageFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Abstract base for test suites that test {@linkplain TenantAware tenant-aware} functionality.
 *
 * <p>This class must be used only from {@linkplain Environment#isTests() test execution context}.
 *
 * @author Alexander Yevsyukov
 */
@Internal
@VisibleForTesting
public abstract class TenantAwareTest {

    public static TenantIndex createTenantIndex(boolean multitenant,
                                                StorageFactory storageFactory) {
        return multitenant
               ? TenantIndex.Factory.createDefault(storageFactory)
               : TenantIndex.Factory.singleTenant();
    }

    /**
     * Sets the current tenant ID to the passed value.
     *
     * <p>Call this method in a set-up method of the derived test suite class.
     */
    protected void setCurrentTenant(TenantId tenantId) {
        checkNotNull(tenantId);
        checkInTests();
        CurrentTenant.set(tenantId);
    }

    /**
     * Clears the current tenant ID.
     *
     * <p>Call this method in a clean-up method of the derived test suite class.
     */
    protected void clearCurrentTenant() {
        checkInTests();
        CurrentTenant.clear();
    }

    /**
     * Obtains current tenant ID.
     *
     * @throws IllegalStateException if the current tenant ID was
     *  {@linkplain #setCurrentTenant(TenantId) not set} or already
     *  {@linkplain #clearCurrentTenant() cleared}
     */
    @SuppressWarnings("ConstantConditions") // see Javadoc
    protected TenantId tenantId() {
        Optional<TenantId> optional = CurrentTenant.get();
        return optional.get();
    }

    private static void checkInTests() {
        checkState(Environment.getInstance()
                              .isTests());
    }
}
