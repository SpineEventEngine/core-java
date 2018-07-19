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

import io.spine.core.TenantId;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.validate.Validate.isNotDefault;

/**
 * This class allows to obtain a ID of the current tenant when handling
 * a command or a query in a multi-tenant application.
 *
 * @author Alexander Yevsyukov
 * @see <a href="https://msdn.microsoft.com/en-us/library/aa479086.aspx">Multi-Tenant Data Architecture</a>
 */
@Internal
public class CurrentTenant {

    private static final ThreadLocal<TenantId> threadLocal = new ThreadLocal<>();

    /** A stub instance of {@code TenantId} to be used by the storage in single-tenant context. */
    private static final TenantId singleTenant = TenantId.newBuilder()
                                                         .setValue("SINGLE_TENANT")
                                                         .build();

    private CurrentTenant() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Returns a constant for single-tenant applications.
     */
    static TenantId singleTenant() {
        return singleTenant;
    }

    /**
     * Obtains the ID of a tenant served in the current thread.
     *
     * @return ID of the tenant or {@linkplain Optional#empty() empty Optional} if
     *         the current thread works not in a multi-tenant context
     */
    public static Optional<TenantId> get() {
        final TenantId result = threadLocal.get();
        return Optional.ofNullable(result);
    }

    /**
     * Ensures that there is {@code TenantId} set for the current execution context.
     *
     * <p>If this is not the case, {@code IllegalStateException} will be thrown.
     *
     * @return the ID of the current tenant
     * @throws IllegalStateException if the is no current tenant ID set
     */
    static TenantId ensure() throws IllegalStateException {
        final Optional<TenantId> currentTenant = get();
        if (!currentTenant.isPresent()) {
            throw new IllegalStateException(
                    "No current TenantId set in multi-tenant execution context.");
        }
        return currentTenant.get();
    }

    /**
     * Sets the ID of the tenant served in the current thread.
     *
     * @param tenantId a non-null and non-default instance of {@code TenantId}
     */
    public static void set(TenantId tenantId) {
        checkNotNull(tenantId);
        checkArgument(isNotDefault(tenantId));
        threadLocal.set(tenantId);
    }

    /**
     * Clears the stored value.
     */
    public static void clear() {
        threadLocal.set(null);
    }
}
