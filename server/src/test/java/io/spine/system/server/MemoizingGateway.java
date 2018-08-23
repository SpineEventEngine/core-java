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

import com.google.protobuf.Message;
import io.spine.core.CommandId;
import io.spine.core.EventId;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SystemGateway} which memoizes the posted system commands.
 *
 * <p>This class is a test-only facility, used in order to avoid mocking {@link SystemGateway}
 * instances.
 */
public final class MemoizingGateway implements SystemGateway {

    private @MonotonicNonNull MemoizedCommand lastSeenCommand;

    private final boolean multitenant;

    private MemoizingGateway(boolean multitenant) {
        this.multitenant = multitenant;
    }

    /**
     * Creates a new instance of {@code MemoizingGateway} for a single-tenant execution environment.
     *
     * @return new {@code MemoizingGateway}
     */
    public static MemoizingGateway singleTenant() {
        return new MemoizingGateway(false);
    }

    /**
     * Creates a new instance of {@code MemoizingGateway} for a multitenant execution environment.
     *
     * @return new {@code MemoizingGateway}
     */
    public static MemoizingGateway multitenant() {
        return new MemoizingGateway(true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Memoizes the given command message and the {@link TenantId} which it was posted for.
     *
     * @see #lastSeen()
     */
    @Override
    public void postCommand(Message systemCommand) {
        TenantId tenantId = new TenantFunction<TenantId>(multitenant) {
            @Override
            public TenantId apply(TenantId id) {
                return id;
            }
        }.execute();
        checkNotNull(tenantId);
        lastSeenCommand = new MemoizedCommand(systemCommand, tenantId);
    }

    @Override
    public boolean hasHandled(EntityHistoryId entity, CommandId commandId) {
        return NoOpSystemGateway.INSTANCE.hasHandled(entity, commandId);
    }

    @Override
    public boolean hasHandled(EntityHistoryId entity, EventId eventId) {
        return NoOpSystemGateway.INSTANCE.hasHandled(entity, eventId);
    }

    /**
     * A command received by the {@code MemoizingGateway}.
     */
    public static final class MemoizedCommand {

        private final Message commandMessage;
        private final TenantId tenantId;

        private MemoizedCommand(Message message, TenantId id) {
            commandMessage = message;
            tenantId = id;
        }

        public Message command() {
            return commandMessage;
        }

        public TenantId tenant() {
            return tenantId;
        }
    }

    /**
     * Obtains the last seen by the gateway system command.
     */
    public MemoizedCommand lastSeen() {
        return lastSeenCommand;
    }
}
