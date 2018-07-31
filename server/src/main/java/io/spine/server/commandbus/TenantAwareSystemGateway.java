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

package io.spine.server.commandbus;

import com.google.protobuf.Message;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.system.server.SystemGateway;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SystemGateway} which works for a specified tenant.
 *
 * <p>A system command is {@linkplain #postCommand(Message) posted} within
 * a {@link TenantAwareOperation} with the given tenant set.
 *
 * <p>Any call to this gateway is delegated to another instance of {@link SystemGateway}
 * {@linkplain Builder#atopOf(SystemGateway) passed} on construction.
 *
 * @author Dmytro Dashenkov
 * @see TenantAwareOperation
 */
final class TenantAwareSystemGateway implements SystemGateway {

    private final SystemGateway delegate;
    private final TenantId tenantId;

    private TenantAwareSystemGateway(Builder builder) {
        this.delegate = builder.delegate;
        this.tenantId = builder.tenantId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On an instance of {@code TenantAwareSystemGateway}, posts the given system command for
     * the specified tenant.
     */
    @Override
    public void postCommand(Message systemCommand) {
        Runnable action = () -> delegate.postCommand(systemCommand);
        TenantAwareOperation operation = new Operation(tenantId, action);
        operation.execute();
    }

    /**
     * A {@link TenantAwareOperation} which executed the given action for a specific tenant.
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

    /**
     * Creates a new instance of {@code Builder} for {@code TenantAwareSystemGateway} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * A builder for the {@code TenantAwareSystemGateway} instances.
     */
    static final class Builder {

        private SystemGateway delegate;
        private TenantId tenantId;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        /**
         * Specifies the {@link SystemGateway} to delegate calls to.
         *
         * @param gateway the delegate {@link SystemGateway}
         */
        Builder atopOf(SystemGateway gateway) {
            this.delegate = checkNotNull(gateway);
            return this;
        }

        /**
         * Specifies the ID of the tenant to work with.
         *
         * <p>Use {@code TenantId.getDefaultInstance()} in single-tenant environment.
         *
         * @param tenant the tenant ID
         */
        Builder withTenant(TenantId tenant) {
            this.tenantId = checkNotNull(tenant);
            return this;
        }

        /**
         * Creates a new instance of {@code TenantAwareSystemGateway}.
         *
         * @return new instance of {@code TenantAwareSystemGateway}
         */
        TenantAwareSystemGateway build() {
            checkNotNull(delegate);
            checkNotNull(tenantId);

            return new TenantAwareSystemGateway(this);
        }
    }

}
