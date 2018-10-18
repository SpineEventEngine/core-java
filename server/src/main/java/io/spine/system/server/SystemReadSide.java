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
import io.spine.server.event.EventDispatcher;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

public interface SystemReadSide {

    void register(EventDispatcher<?> dispatcher);

    void unregister(EventDispatcher<?> dispatcher);

    /**
     * Executes the given query for a domain aggregate state.
     *
     * <p>This read operation supports following types of queries:
     * <ul>
     *     <li>queries for all instances of an aggregate type (which are not archived or deleted);
     *     <li>queries by the aggregate IDs;
     *     <li>queries for archived or/and deleted instance (combined with the other query types,
     *         if necessary).
     * </ul>
     *
     * @param query
     *         a query for a domain aggregate
     * @return an {@code Iterator} over the query results packed as {@link Any}s.
     * @see MirrorProjection
     * @see io.spine.client.QueryFactory
     */
    Iterator<Any> readDomainAggregate(Query query);

    /**
     * Creates a new instance of {@code SystemReadSide} for the given system context.
     *
     * @param context
     *         the system context to broadcast the events of
     * @return a new instance of {@code SystemReadSide}
     */
    static SystemReadSide newInstance(SystemContext context) {
        checkNotNull(context);
        return new DefaultSystemReadSide(context);
    }
}
