/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.client;

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.annotations.Internal;
import org.spine3.base.FieldFilter;
import org.spine3.type.TypeName;
import org.spine3.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.client.Targets.allOf;
import static org.spine3.client.Targets.someOf;

/**
 * Client-side utilities for working with queries.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
@Internal
public final class Queries {

    private Queries() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Extract the type of {@link Target} for the given {@link Query}.
     *
     * <p>Returns null if the {@code Target} type is unknown to the application.
     *
     * @param query the query of interest.
     * @return the URL of the type of the query {@linkplain Query#getTarget() target}
     */
    public static TypeUrl typeOf(Query query) {
        checkNotNull(query);

        final Target target = query.getTarget();
        final TypeName typeName = TypeName.of(target.getType());
        final TypeUrl type = typeName.toUrl();
        return type;
    }

    static Query.Builder queryBuilderFor(Class<? extends Message> entityClass,
                                         @Nullable Set<? extends Message> ids,
                                         @Nullable Iterable<FieldFilter> columnFilters,
                                         @Nullable FieldMask fieldMask) {
        checkNotNull(entityClass);

        final Target target = forParams(entityClass, ids, columnFilters);
        final Query.Builder queryBuilder = Query.newBuilder()
                                                .setTarget(target);
        if (fieldMask != null) {
            queryBuilder.setFieldMask(fieldMask);
        }
        return queryBuilder;
    }

    private static Target forParams(Class<? extends Message> entityClass,
                                    @Nullable Set<? extends Message> ids,
                                    @Nullable Iterable<FieldFilter> columnFilters) {
        final Target target;
        if (ids == null && columnFilters == null) {
            target = allOf(entityClass);
        } else if (ids == null) {
            target = someOf(entityClass, columnFilters);
        } else if (columnFilters == null) {
            target = someOf(entityClass, ids);
        } else {
            target = someOf(entityClass, ids, columnFilters);
        }
        return target;
    }

}
