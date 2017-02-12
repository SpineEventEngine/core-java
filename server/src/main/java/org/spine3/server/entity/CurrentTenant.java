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

package org.spine3.server.entity;

import com.google.common.base.Optional;
import org.spine3.SPI;
import org.spine3.users.TenantId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * This class allows to set a tenant when handling a command or a query
 * in a multitenant application.
 *
 * @author Alexander Yevsyukov
 * @see <a href="https://msdn.microsoft.com/en-us/library/aa479086.aspx">Multi-Tenant Data Architecture</a>
 */
@SPI
public class CurrentTenant {

    private static final ThreadLocal<TenantId> threadLocal = new ThreadLocal<>();

    /** Have only static methods for this class. */
    private CurrentTenant() {}

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
     * Obtains the ID of a tenant served in the current thread.
     *
     * @return ID of the tenant or {@linkplain Optional#absent() empty Optional} if
     *         the current thread works not in a multitenant context
     */
    public static Optional<TenantId> get() {
        final TenantId result = threadLocal.get();
        return Optional.fromNullable(result);
    }

    /**
     * Clears the stored value.
     */
    public static void clear() {
        threadLocal.set(null);
    }
}
