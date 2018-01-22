/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.core.ActorContext;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Queries.queryBuilderFor;
import static java.lang.String.format;

/**
 * Public API for creating {@link Query} instances, using the {@code ActorRequestFactory}
 * configuration.
 *
 * @see ActorRequestFactory#query()
 */
public final class QueryFactory {

    /**
     * The format of all {@linkplain QueryId query identifiers}.
     */
    private static final String QUERY_ID_FORMAT = "query-%s";

    private static final String ENTITY_IDS_EMPTY_MSG = "Entity ID set must not be empty";

    private final ActorContext actorContext;

    QueryFactory(ActorRequestFactory actorRequestFactory) {
        checkNotNull(actorRequestFactory);
        this.actorContext = actorRequestFactory.actorContext();
    }

    private static QueryId newQueryId() {
        final String formattedId = format(QUERY_ID_FORMAT, Identifier.newUuid());
        return QueryId.newBuilder()
                      .setValue(formattedId)
                      .build();
    }

    /**
     * Creates a new instance of {@link QueryBuilder} for the further {@linkplain Query}
     * construction.
     *
     * @param targetType the {@linkplain Query query} target type
     * @return new instance of {@link QueryBuilder}
     */
    public QueryBuilder select(Class<? extends Message> targetType) {
        checkNotNull(targetType);
        final QueryBuilder queryBuilder = new QueryBuilder(targetType, this);
        return queryBuilder;
    }

    /**
     * Creates a {@link Query} to read certain entity states by IDs with the {@link FieldMask}
     * applied to each of the results.
     *
     * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing.
     * The processing results will contain only the entities, which IDs are present among
     * the {@code ids}.
     *
     * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query
     * results. This processing is performed according to the
     * <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field),
     * such invalid paths are silently ignored.
     *
     * @param entityClass the class of a target entity
     * @param ids         the entity IDs of interest
     * @param maskPaths   the property paths for the {@code FieldMask} applied
     *                    to each of results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query byIdsWithMask(Class<? extends Message> entityClass,
                               Set<? extends Message> ids,
                               String... maskPaths) {
        checkNotNull(ids);
        checkArgument(!ids.isEmpty(), ENTITY_IDS_EMPTY_MSG);

        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(maskPaths))
                                             .build();
        final Query result = composeQuery(entityClass, ids, null, fieldMask);
        return result;
    }

    /**
     * Creates a {@link Query} to read certain entity states by IDs.
     *
     * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing.
     * The processing results will contain only the entities, which IDs are present among
     * the {@code ids}.
     *
     * <p>Unlike {@link #byIdsWithMask(Class, Set, String...)}, the {@code Query} processing
     * will not change the resulting entities.
     *
     * @param entityClass the class of a target entity
     * @param ids         the entity IDs of interest
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query byIds(Class<? extends Message> entityClass,
                       Set<? extends Message> ids) {
        checkNotNull(entityClass);
        checkNotNull(ids);

        return composeQuery(entityClass, ids, null, null);
    }

    /**
     * Creates a {@link Query} to read all entity states with the {@link FieldMask}
     * applied to each of the results.
     *
     * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query
     * results. This processing is performed according to the
     * <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field), such invalid paths
     * are silently ignored.
     *
     * @param entityClass the class of a target entity
     * @param maskPaths   the property paths for the {@code FieldMask} applied
     *                    to each of results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query allWithMask(Class<? extends Message> entityClass, String... maskPaths) {
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(maskPaths))
                                             .build();
        final Query result = composeQuery(entityClass, null, null, fieldMask);
        return result;
    }

    /**
     * Creates a {@link Query} to read all states of a certain entity.
     *
     * <p>Unlike {@link #allWithMask(Class, String...)}, the {@code Query} processing will
     * not change the resulting entities.
     *
     * @param entityClass the class of a target entity
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query all(Class<? extends Message> entityClass) {
        checkNotNull(entityClass);

        return composeQuery(entityClass, null, null, null);
    }

    Query composeQuery(Class<? extends Message> entityClass,
                       @Nullable Set<? extends Message> ids,
                       @Nullable Set<CompositeColumnFilter> columnFilters,
                       @Nullable FieldMask fieldMask) {
        checkNotNull(entityClass, "The class of Entity must be specified for a Query");

        final Query.Builder builder = queryBuilderFor(entityClass,
                                                      ids,
                                                      columnFilters,
                                                      fieldMask);
        builder.setId(newQueryId())
               .setContext(actorContext);
        return builder.build();
    }
}
