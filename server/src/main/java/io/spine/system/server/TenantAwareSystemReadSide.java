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

package io.spine.system.server;

import com.google.protobuf.Any;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.server.event.EventDispatcher;
import io.spine.server.tenant.TenantAwareRunner;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of {@link SystemReadSide} which executes all the operations for a given tenant.
 *
 * <p>The {@code SystemReadSide} delegates operations to the given delegate.
 */
final class TenantAwareSystemReadSide implements SystemReadSide {

    private final SystemReadSide delegate;
    private final TenantAwareRunner runner;

    private TenantAwareSystemReadSide(TenantId tenantId, SystemReadSide delegate) {
        this.runner = TenantAwareRunner.with(checkNotNull(tenantId));
        this.delegate = checkNotNull(delegate);
    }

    static SystemReadSide forTenant(TenantId tenantId, SystemReadSide delegate) {
        checkNotNull(tenantId);
        checkNotNull(delegate);
        SystemReadSide result = new TenantAwareSystemReadSide(tenantId, delegate);
        return result;
    }

    @Override
    public void register(EventDispatcher<?> dispatcher) {
        runner.run(() -> delegate.register(dispatcher));
    }

    @Override
    public void unregister(EventDispatcher<?> dispatcher) {
        runner.run(() -> delegate.unregister(dispatcher));
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        return runner.evaluate(() -> delegate.readDomainAggregate(query));
    }
}
