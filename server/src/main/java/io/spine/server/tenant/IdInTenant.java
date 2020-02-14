/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;
import io.spine.core.TenantId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An identifier that exists for a specific {@link TenantId}.
 *
 * @param <I>
 *         the type of the identifier
 */
@Internal
public final class IdInTenant<I> {

    private final I id;
    private final TenantId tenantId;

    private IdInTenant(I id, TenantId tenantId) {
        this.id = id;
        this.tenantId = tenantId;
    }

    /**
     * Creates a new instance of {@code IdInTenant} with the given identifier value and
     * selected multitenancy mode.
     *
     * <p>Uses the current {@code Tenant}.
     */
    public static <I> IdInTenant<I> of(I id, boolean multitenant) {
        checkNotNull(id);
        TenantId tenant = TenantAware.getCurrentTenant(multitenant);
        return new IdInTenant<>(id, tenant);
    }

    /**
     * Creates a new instance of {@code IdInTenant} with the given identifier value and
     * {@code TenantId}.
     */
    public static <I> IdInTenant<I> of(I id, TenantId tenant) {
        checkNotNull(id);
        checkNotNull(tenant);
        return new IdInTenant<>(id, tenant);
    }

    /**
     * Returns the value of the identifier.
     */
    public I value() {
        return id;
    }

    /**
     * Returns the corresponding {@code TenantId}.
     */
    public TenantId tenant() {
        return tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IdInTenant<?> another = (IdInTenant<?>) o;
        return Objects.equals(id, another.id) &&
                Objects.equals(tenantId, another.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("id", id)
                          .add("tenantId", tenantId)
                          .toString();
    }
}
