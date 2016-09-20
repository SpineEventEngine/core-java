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
package org.spine3.base;

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.client.Query;
import org.spine3.client.Target;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Client-side utilities for working with queries.
 *
 * @author Alex Tymchenko
 */
public class Queries {

    private Queries() {
    }

    public static Query readByIds(Class<? extends Message> entityClass, Set<Message> ids) {
        final Query result = composeQuery(entityClass, ids, null);
        return result;
    }

    public static Query readAll(Class<? extends Message> entityClass) {
        final Query result = composeQuery(entityClass, null, null);
        return result;
    }

    public static Target someOf(Class<? extends Message> entityClass, Set<Message> ids) {
        final Target result = composeTarget(entityClass, ids);
        return result;
    }

    public static Target allOf(Class<? extends Message> entityClass) {
        final Target result = composeTarget(entityClass, null);
        return result;
    }


    // TODO[alex.tymchenko]: think of Optional instead. Consider Java 8 vs Guava Optional.
    private static Query composeQuery(Class<? extends Message> entityClass, @Nullable Set<Message> ids, @Nullable FieldMask fieldMask) {
        final Target target = composeTarget(entityClass, ids);


        final Query.Builder queryBuilder = Query.newBuilder()
                                                .setTarget(target);
        if (fieldMask != null) {
            queryBuilder.setFieldMask(fieldMask);
        }
        final Query result = queryBuilder
                .build();
        return result;
    }

    private static Target composeTarget(Class<? extends Message> entityClass, @Nullable Set<Message> ids) {
        final TypeUrl type = TypeUrl.of(entityClass);

        final boolean includeAll = ids == null;

        final EntityIdFilter.Builder idFilterBuilder = EntityIdFilter.newBuilder();

        if (!includeAll) {
            for (Message rawId : ids) {
                final Any packedId = AnyPacker.pack(rawId);
                final EntityId entityId = EntityId.newBuilder()
                                                  .setId(packedId)
                                                  .build();
                idFilterBuilder.addIds(entityId);
            }
        }
        final EntityIdFilter idFilter = idFilterBuilder.build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .build();

        return Target.newBuilder()
                     .setIncludeAll(includeAll)
                     .setType(type.getTypeName())
                     .setFilters(filters)
                     .build();
    }
}
