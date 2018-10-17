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
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyIterator;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A {@link SystemWriteSide} which memoizes the posted system commands.
 *
 * <p>This class is a test-only facility, used in order to avoid mocking {@link SystemWriteSide}
 * instances.
 */
public final class MemoizingWriteSide implements SystemWriteSide {

    private @MonotonicNonNull MemoizedMessage lastSeenCommand;
    private @MonotonicNonNull MemoizedMessage lastSeenEvent;
    private @MonotonicNonNull MemoizedMessage lastSeenQuery;

    private final boolean multitenant;

    private MemoizingWriteSide(boolean multitenant) {
        this.multitenant = multitenant;
    }

    /**
     * Creates a new instance of {@code MemoizingWriteSide} for a single-tenant execution environment.
     *
     * @return new {@code MemoizingWriteSide}
     */
    public static MemoizingWriteSide singleTenant() {
        return new MemoizingWriteSide(false);
    }

    /**
     * Creates a new instance of {@code MemoizingWriteSide} for a multitenant execution environment.
     *
     * @return new {@code MemoizingWriteSide}
     */
    public static MemoizingWriteSide multitenant() {
        return new MemoizingWriteSide(true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Memoizes the given command message and the {@link TenantId} which it was posted for.
     *
     * @see #lastSeenCommand()
     */
    @Override
    public void postCommand(CommandMessage systemCommand) {
        TenantId tenantId = currentTenant();
        lastSeenCommand = new MemoizedMessage(systemCommand, tenantId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Memoizes the given event message and the {@link TenantId} which it was posted for.
     *
     * @see #lastSeenEvent()
     */
    @Override
    public void postEvent(EventMessage systemEvent) {
        TenantId tenantId = currentTenant();
        lastSeenEvent = new MemoizedMessage(systemEvent, tenantId);
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        TenantId tenantId = currentTenant();
        lastSeenQuery = new MemoizedMessage(query, tenantId);
        return emptyIterator();
    }

    /** Obtains the ID of the current tenant. */
    private TenantId currentTenant() {
        TenantId result = new TenantFunction<TenantId>(multitenant) {
            @Override
            public TenantId apply(TenantId id) {
                return id;
            }
        }.execute();
        return checkNotNull(result);
    }

    /**
     * A command received by the {@code MemoizingWriteSide}.
     */
    public static final class MemoizedMessage {

        private final Message message;
        private final TenantId tenantId;

        private MemoizedMessage(Message message, TenantId id) {
            this.message = message;
            tenantId = id;
        }

        public Message message() {
            return message;
        }

        public TenantId tenant() {
            return tenantId;
        }
    }

    /**
     * Obtains the last command message posted to {@link SystemWriteSide}.
     *
     * <p>Fails if no commands were posted yet.
     */
    public MemoizedMessage lastSeenCommand() {
        assertNotNull(lastSeenCommand);
        return lastSeenCommand;
    }

    /**
     * Obtains the last event message posted to {@link SystemWriteSide}.
     *
     * <p>Fails if no events were posted yet.
     */
    public MemoizedMessage lastSeenEvent() {
        assertNotNull(lastSeenEvent);
        return lastSeenEvent;
    }

    /**
     * Checks that this gateway has never seen an event.
     *
     * <p>Fails if the check does not pass.
     */
    public void assertNoEvents() {
        assertNull(lastSeenEvent, () -> lastSeenEvent.message().toString());
    }

    /**
     * Obtains the last query submitted to {@link SystemWriteSide}.
     *
     * <p>Fails if no queries were submitted yet.
     */
    public MemoizedMessage lastSeenQuery() {
        assertNotNull(lastSeenQuery);
        return lastSeenQuery;
    }
}
