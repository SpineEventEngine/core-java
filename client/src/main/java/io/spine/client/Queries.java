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
package io.spine.client;

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.annotation.Internal;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.client.Targets.composeTarget;
import static io.spine.type.KnownTypes.getAllUrls;
import static java.lang.String.format;

/**
 * Client-side utilities for working with queries.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
@Internal
public final class Queries {

    /**
     * The format of all {@linkplain QueryId query identifiers}.
     */
    private static final String QUERY_ID_FORMAT = "q-%s";

    private Queries() {
        // Prevent instantiation of this utility class.
    }

    public static QueryId generateId() {
        final String formattedId = format(QUERY_ID_FORMAT, Identifier.newUuid());
        return QueryId.newBuilder()
                      .setValue(formattedId)
                      .build();
    }

    /**
     * Extract the type of {@link Target} for the given {@link Query}.
     *
     * <p>Throws an {@link IllegalStateException} if the {@code Target} type is unknown to
     * the application.
     *
     * @param query the query of interest.
     * @return the URL of the type of the query {@linkplain Query#getTarget() target}
     */
    public static TypeUrl typeOf(Query query) {
        checkNotNull(query);

        final Target target = query.getTarget();
        final String type = target.getType();
        final TypeUrl typeUrl = TypeUrl.parse(type);
        checkState(getAllUrls().contains(typeUrl), "Unknown type URL: `%s`.", type);
        return typeUrl;
    }

    static Query.Builder queryBuilderFor(Class<? extends Message> entityClass,
                                         @Nullable Set<? extends Message> ids,
                                         @Nullable Set<CompositeColumnFilter> columnFilters,
                                         @Nullable FieldMask fieldMask) {
        checkNotNull(entityClass);

        final Target target = composeTarget(entityClass, ids, columnFilters);
        final Query.Builder queryBuilder = Query.newBuilder()
                                                .setTarget(target);
        if (fieldMask != null) {
            queryBuilder.setFieldMask(fieldMask);
        }
        return queryBuilder;
    }
}
