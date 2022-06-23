/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.annotation.Internal;
import io.spine.core.TenantId;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A convenience API for {@code TenantAware} operations.
 */
@Internal
public final class TenantAwareRunner {

    private final TenantId tenant;

    private TenantAwareRunner(TenantId tenant) {
        this.tenant = tenant;
    }

    /**
     * Creates a new {@code TenantAwareRunner}, which runs all the given operations for the given
     * {@code tenant}.
     *
     * @param tenant
     *         the target tenant
     * @return new instance of {@code TenantAwareRunner}
     */
    public static TenantAwareRunner with(TenantId tenant) {
        checkNotNull(tenant);
        return new TenantAwareRunner(tenant);
    }

    /**
     * Creates a new {@code TenantAwareRunner}, which runs all the given operations for the current
     * {@code tenant}.
     *
     * @param multitenant
     *         if the current environment is multitenant
     * @return new instance of {@code TenantAwareRunner}
     */
    public static TenantAwareRunner withCurrentTenant(boolean multitenant) {
        return with(TenantAware.getCurrentTenant(multitenant));
    }

    /**
     * Runs the given {@code operation} for the given tenant and returns the result of
     * the operation.
     *
     * @param operation
     *         the operation to run
     * @param <T> the type of the operation result
     * @return the result of the operation
     */
    public <T> T evaluate(Supplier<T> operation) {
        checkNotNull(operation);
        T result = new TenantAwareFunction0<T>(tenant) {
            @Override
            public T apply() {
                return operation.get();
            }
        }.execute();
        return result;
    }

    /**
     * Runs the given {@code operation} for the given tenant.
     *
     * @param operation
     *         the operation to run
     */
    public void run(Runnable operation) {
        checkNotNull(operation);
        new TenantAwareOperation(tenant) {
            @Override
            public void run() {
                operation.run();
            }
        }.execute();
    }
}
