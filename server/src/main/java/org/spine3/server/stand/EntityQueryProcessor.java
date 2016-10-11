/*
 *
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package org.spine3.server.stand;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.client.EntityFilters;
import org.spine3.client.Query;
import org.spine3.client.Target;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.EntityRepository;

/**
 * Processes the queries targeting {@link org.spine3.server.entity.Entity} objects.
 *
 * @author Alex Tymchenko
 */
/* package */ class EntityQueryProcessor implements QueryProcessor {

    private final EntityRepository<?, ? extends Entity, ? extends Message> repository;

    /* package */ EntityQueryProcessor(EntityRepository<?, ? extends Entity, ? extends Message> repository) {
        this.repository = repository;
    }

    @Override
    public ImmutableCollection<Any> process(Query query) {
        final ImmutableList.Builder<Any> resultBuilder = ImmutableList.builder();

        final Target target = query.getTarget();
        final FieldMask fieldMask = query.getFieldMask();

        final ImmutableCollection<? extends Entity> entities;
        if (target.getIncludeAll() && fieldMask.getPathsList()
                                               .isEmpty()) {
            entities = repository.loadAll();
        } else {
            final EntityFilters filters = target.getFilters();
            entities = repository.find(filters, fieldMask);
        }

        for (Entity entity : entities) {
            final Message state = entity.getState();
            final Any packedState = AnyPacker.pack(state);
            resultBuilder.add(packedState);
        }

        final ImmutableList<Any> result = resultBuilder.build();
        return result;
    }
}
