/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import com.google.protobuf.Message;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.ResponseFormat;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.RecordBasedRepository;

import java.util.Iterator;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

/**
 * Processes the queries targeting {@link io.spine.server.entity.Entity Entity} objects.
 */
class EntityQueryProcessor implements QueryProcessor {

    private final RecordBasedRepository<?, ? extends Entity, ? extends Message> repository;

    EntityQueryProcessor(RecordBasedRepository<?, ? extends Entity, ? extends Message> repository) {
        this.repository = repository;
    }

    @Override
    public ImmutableCollection<EntityStateWithVersion> process(Query query) {
        Target target = query.getTarget();
        Iterator<EntityRecord> entities = target.getIncludeAll()
                                          ? loadAll(query.getFormat())
                                          : loadByQuery(query);
        ImmutableList<EntityStateWithVersion> result = stream(entities)
                .map(EntityQueryProcessor::toEntityState)
                .collect(toImmutableList());
        return result;
    }

    private Iterator<EntityRecord> loadByQuery(Query query) {
        Target target = query.getTarget();
        TargetFilters filters = target.getFilters();
        Iterator<EntityRecord> entities = repository.findRecords(filters, query.getFormat());
        return entities;
    }

    private Iterator<EntityRecord> loadAll(ResponseFormat format) {
        Iterator<EntityRecord> entities = repository.loadAllRecords(format);
        return entities;
    }

    private static EntityStateWithVersion toEntityState(EntityRecord record) {
        EntityStateWithVersion result = EntityStateWithVersion
                .newBuilder()
                .setState(record.getState())
                .setVersion(record.getVersion())
                .build();
        return result;
    }
}