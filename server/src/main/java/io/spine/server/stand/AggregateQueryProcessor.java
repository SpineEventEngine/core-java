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
package io.spine.server.stand;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.server.aggregate.Aggregate;
import io.spine.system.server.SystemGateway;

import java.util.Iterator;

import static com.google.common.collect.ImmutableList.copyOf;
import static io.spine.client.Queries.tenantOf;
import static io.spine.system.server.GatewayFunction.delegatingTo;

/**
 * Processes the queries targeting {@link Aggregate Aggregate} state.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
class AggregateQueryProcessor implements QueryProcessor {

    private final SystemGateway systemGateway;

    AggregateQueryProcessor(SystemGateway systemGateway) {
        this.systemGateway = systemGateway;
    }

    @Override
    public ImmutableCollection<Any> process(Query query) {
        TenantId tenant = tenantOf(query);
        SystemGateway gateway = delegatingTo(systemGateway).get(tenant);
        Iterator<Any> read = gateway.readDomainAggregate(query);
        ImmutableList<Any> result = copyOf(read);
        return result;
    }
}
