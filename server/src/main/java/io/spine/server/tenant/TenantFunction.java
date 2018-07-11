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

import com.google.common.base.Function;
import io.spine.annotation.Internal;
import io.spine.core.TenantId;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A function which can work in single-tenant and multi-tenant context and return a
 * value depending on the current tenant set.
 *
 * @param <T> the type of the result returned by the function
 * @author Alexander Yevsyukov
 */
@Internal
public abstract class TenantFunction<T> extends TenantAware implements Function<TenantId, T> {

    /**
     * Creates a new instance of the function.
     *
     * @param multitenant if {@code true} the function is executed in the multi-tenant context,
     *                    {@code false} for single-tenant context
     */
    protected TenantFunction(boolean multitenant) {
        super(TenantAware.getCurrentTenant(multitenant));
    }

    /**
     * Applies the function and returns the result.
     *
     * @return the result of the function
     */
    public @Nullable T execute() {
        final TenantId currentTenant = tenantId();
        final T result = apply(currentTenant);
        return result;
    }
}
