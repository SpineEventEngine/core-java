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
import com.google.protobuf.Message;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.server.tenant.TenantAwareOperation;

import java.util.Iterator;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SystemGateway} which works for a specified tenant.
 *
 * <p>A system command is {@linkplain #postCommand(Message) posted} within
 * a {@link TenantAwareOperation} with the given tenant set.
 *
 * <p>Any call to this gateway is delegated to another instance of {@link SystemGateway}
 * passed on construction.
 *
 * @author Dmytro Dashenkov
 * @see TenantAwareOperation
 */
final class TenantAwareSystemGateway implements SystemGateway {

    private final SystemGateway delegate;
    private final TenantId tenantId;

    private TenantAwareSystemGateway(TenantId tenantId, SystemGateway delegate) {
        this.tenantId = checkNotNull(tenantId);
        this.delegate = checkNotNull(delegate);
    }

    static SystemGateway forTenant(TenantId tenantId, SystemGateway systemGateway) {
        checkNotNull(tenantId);
        checkNotNull(systemGateway);
        SystemGateway result = new TenantAwareSystemGateway(tenantId, systemGateway);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts the given system command under the context of the specified tenant.
     */
    @Override
    public void postCommand(Message systemCommand) {
        run(() -> delegate.postCommand(systemCommand));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts the given system event under the context of the specified tenant.
     */
    @Override
    public void postEvent(Message systemEvent) {
        run(() -> delegate.postEvent(systemEvent));
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        return run(() -> delegate.readDomainAggregate(query));
    }

    private void run(Runnable action) {
        TenantAwareOperation operation = new Operation(tenantId, action);
        operation.execute();
    }

    private <T> T run(Supplier<T> action) {
        T result = new TenantAwareFunction0<T>(tenantId) {
            @Override
            public T apply() {
                return action.get();
            }
        }.execute();
        return result;
    }

    /**
     * A {@link TenantAwareOperation} which executes the given action for a specific tenant.
     */
    private static final class Operation extends TenantAwareOperation {

        private final Runnable action;

        private Operation(TenantId tenantId, Runnable action) {
            super(tenantId);
            this.action = action;
        }

        @Override
        public void run() {
            action.run();
        }
    }
}
