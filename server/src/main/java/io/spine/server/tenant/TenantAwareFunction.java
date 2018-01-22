/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.common.base.Optional;
import io.spine.annotation.SPI;
import io.spine.core.TenantId;

/**
 * A function, which is calculated in a tenant context.
 *
 * @param <F> the type of the input
 * @param <T> the type of the output
 * @author Alexander Yevsykov
 */
@SPI
public abstract class TenantAwareFunction<F, T> extends TenantAware implements Function<F, T> {

    protected TenantAwareFunction() throws IllegalStateException {
        super();
    }

    protected TenantAwareFunction(TenantId tenantId) {
        super(tenantId);
    }

    /**
     * Returns the result of {@linkplain #apply(Object) applying} the function to the passed
     * {@code input}.
     *
     * <p>The execution goes through the following steps:
     * <ol>
     *     <li>The current tenant ID is obtained and remembered.
     *     <li>The tenant ID passed to the constructor is set as current.
     *     <li>The {@link #apply(Object)} method is called.
     *     <li>The previous tenant ID is set as current.
     * </ol>
     */
    public T execute(F input) {
        final T result;
        final Optional<TenantId> remembered = CurrentTenant.get();
        try {
            CurrentTenant.set(tenantId());
            result = apply(input);
            return result;
        } finally {
            if (remembered.isPresent()) {
                CurrentTenant.set(remembered.get());
            } else {
                CurrentTenant.clear();
            }
        }
    }
}
