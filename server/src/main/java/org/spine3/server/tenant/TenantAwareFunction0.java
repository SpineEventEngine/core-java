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

import com.google.protobuf.Empty;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;

/**
 * A tenant-aware function that takes no parameters.
 *
 * @param <T> the type of the result returned by the function
 * @author Alexander Yevsyukov
 */
public abstract class TenantAwareFunction0<T> extends TenantAwareFunction<Empty, T> {

    protected TenantAwareFunction0() throws IllegalStateException {
        super();
    }

    protected TenantAwareFunction0(TenantId tenantId) {
        super(tenantId);
    }

    public abstract T apply();

    @Nullable
    @Override
    public T apply(@Nullable Empty input) {
        return apply();
    }

    public T execute() {
        return super.execute(Empty.getDefaultInstance());
    }
}
