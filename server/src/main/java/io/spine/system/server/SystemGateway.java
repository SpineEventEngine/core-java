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

import java.util.Iterator;

/**
 * A gateway for sending messages into a {@link SystemContext}.
 *
 * @author Dmytro Dashenkov
 */
public interface SystemGateway {

    /**
     * Posts a system command.
     *
     * <p>If the associated bounded context is
     * {@linkplain io.spine.server.BoundedContext#isMultitenant() multitenant}, the command is
     * posted for the {@linkplain io.spine.server.tenant.TenantAwareOperation current tenant}.
     *
     * @param systemCommand command message
     */
    void postCommand(Message systemCommand);

    /**
     * Posts a system event.
     *
     * <p>If the associated bounded context is
     * {@linkplain io.spine.server.BoundedContext#isMultitenant() multitenant}, the event is
     * posted for the {@linkplain io.spine.server.tenant.TenantAwareOperation current tenant}.
     *
     * @param systemEvent event message
     */
    void postEvent(Message systemEvent);

    /**
     * Creates new instance of the gateway which serves the passed System Bounded Context.
     */
    static SystemGateway newInstance(SystemContext system) {
        return new DefaultSystemGateway(system);
    }

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
}
