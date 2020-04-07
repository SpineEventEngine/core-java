/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.core.ActorContext;
import io.spine.core.TenantId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.EntityRecord;
import io.spine.server.tenant.TenantAwareRunner;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;

/**
 * Processes the queries targeting {@link Aggregate Aggregate} state.
 */
class AggregateQueryProcessor implements QueryProcessor {

    private final AggregateRepository<?, ?> repository;

    AggregateQueryProcessor(AggregateRepository<?, ?> repository) {
        this.repository = repository;
    }

    @Override
    public ImmutableCollection<EntityStateWithVersion> process(Query query) {
        TargetFilters filters = query.filters();
        ResponseFormat format = query.responseFormat();
        TenantId tenantId = tenantOf(query);
        Iterator<EntityRecord> rawRecords =
                TenantAwareRunner.with(tenantId)
                                 .evaluate(() -> repository.findRecords(filters, format));

        ImmutableList<EntityStateWithVersion> results =
                stream(rawRecords)
                        .map(this::toEntityState)
                        .collect(ImmutableList.toImmutableList());
        return results;
    }

    /**
     * Obtains the {@link TenantId} of the given {@link Query}.
     *
     * <p>In a single-tenant environment, this value should be
     * the {@linkplain TenantId#getDefaultInstance() default ID}.
     *
     * @param query
     *         the query to extract tenant from
     * @return the tenant of this query
     */
    private static TenantId tenantOf(Query query) {
        checkNotNull(query);

        ActorContext context = query.getContext();
        TenantId result = context.getTenantId();
        return result;
    }
}
