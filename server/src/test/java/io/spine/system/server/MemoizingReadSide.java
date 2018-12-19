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

import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.server.event.EventDispatcher;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyIterator;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A {@link SystemReadSide} which memoizes the queries passed to it.
 *
 * <p>This class is a test-only facility, used in order to avoid mocking {@link SystemReadSide}
 * instances.
 *
 * <p>Note that the dispatcher registration is not implemented. See {@link #register} and
 * {@link #unregister} for the details.
 */
public final class MemoizingReadSide implements SystemReadSide {

    private @MonotonicNonNull MemoizedSystemMessage lastSeenQuery;

    private final boolean multitenant;

    private MemoizingReadSide(boolean multitenant) {
        this.multitenant = multitenant;
    }

    /**
     * Creates a new instance of {@code MemoizingWriteSide} for a single-tenant execution environment.
     *
     * @return new {@code MemoizingWriteSide}
     */
    public static MemoizingReadSide singleTenant() {
        return new MemoizingReadSide(false);
    }

    /**
     * Creates a new instance of {@code MemoizingWriteSide} for a multitenant execution environment.
     *
     * @return new {@code MemoizingWriteSide}
     */
    public static MemoizingReadSide multitenant() {
        return new MemoizingReadSide(true);
    }

    /**
     * Throws an {@link UnsupportedOperationException}.
     *
     * @deprecated This method is not implemented, since there are no use cases for such
     *             an implementation in tests.
     */
    @Deprecated
    @Override
    public void register(EventDispatcher<?> dispatcher) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Method register is not implemented!");
    }

    /**
     * Throws an {@link UnsupportedOperationException}.
     *
     * @deprecated This method is not implemented, since there are no use cases for such
     *             an implementation in tests.
     */
    @Deprecated
    @Override
    public void unregister(EventDispatcher<?> dispatcher) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Method unregister is not implemented!");
    }

    /**
     * Memoizes the given query and returns an empty iterator.
     *
     * @param query
     *         a query to memoize
     * @return always an empty iterator
     */
    @Override
    public Iterator<EntityStateWithVersion> readDomainAggregate(Query query) {
        TenantId tenantId = currentTenant();
        lastSeenQuery = new MemoizedSystemMessage(query, tenantId);
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
     * Obtains the last query submitted to this {@link SystemReadSide}.
     *
     * <p>Fails if no queries were submitted yet.
     */
    public MemoizedSystemMessage lastSeenQuery() {
        assertNotNull(lastSeenQuery);
        return lastSeenQuery;
    }

}
