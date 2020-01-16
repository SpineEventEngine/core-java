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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.FieldMask;
import io.spine.base.EntityState;
import io.spine.core.ActorContext;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.FieldMaskUtil.fromStringList;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.Targets.composeTarget;
import static java.lang.String.format;

/**
 * A factory of {@link Query} instances.
 *
 * <p>Uses the given {@link ActorRequestFactory} as a source of the query meta information,
 * such as the actor.
 *
 * @see ActorRequestFactory#query()
 */
public final class QueryFactory {

    /**
     * The format of all {@linkplain QueryId query identifiers}.
     */
    private static final String QUERY_ID_FORMAT = "query-%s";

    private static final String ENTITY_IDS_EMPTY_MSG = "Entity ID set must not be empty.";

    private final ActorContext actorContext;

    QueryFactory(ActorRequestFactory actorRequestFactory) {
        checkNotNull(actorRequestFactory);
        this.actorContext = actorRequestFactory.newActorContext();
    }

    private static QueryId newQueryId() {
        String formattedId = format(QUERY_ID_FORMAT, newUuid());
        return QueryId.newBuilder()
                      .setValue(formattedId)
                      .build();
    }

    /**
     * Creates a new instance of {@link QueryBuilder} for the further {@link Query}
     * construction.
     *
     * @param targetType
     *         the {@linkplain Query query} target type
     * @return new instance of {@link QueryBuilder}
     */
    public QueryBuilder select(Class<? extends EntityState> targetType) {
        checkNotNull(targetType);
        QueryBuilder queryBuilder = new QueryBuilder(targetType, this);
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
     * <p>Allows to set property paths for a {@link FieldMask} applied to each of the query
     * results. This processing is performed according to the
     * <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>If the {@code paths} array contains entries inapplicable to the resulting entity
     * (for example a {@code path} references a missing field),
     * such invalid paths are silently ignored.
     *
     * @param entityClass
     *         the class of a target entity
     * @param ids
     *         the IDs of interest of type {@link io.spine.base.Identifier#checkSupported(Class)
     *         which is supported as identifier}
     * @param maskPaths
     *         the property paths for the {@code FieldMask} applied
     *         to each of the results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query byIdsWithMask(Class<? extends EntityState> entityClass,
                               Set<?> ids,
                               String... maskPaths) {
        checkSpecified(entityClass);
        checkNotNull(ids);
        checkArgument(!ids.isEmpty(), ENTITY_IDS_EMPTY_MSG);
        FieldMask fieldMask = fromStringList(null, ImmutableList.copyOf(maskPaths));
        Query result = composeQuery(entityClass, ids, null, fieldMask);
        return result;
    }

    /**
     * Creates a {@link Query} to read certain entity states by IDs.
     *
     * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing.
     * The processing results will contain only the entities which IDs are present among
     * the {@code ids}.
     *
     * <p>Unlike {@link #byIdsWithMask(Class, Set, String...)}, the {@code Query} processing
     * will not change the resulting entities.
     *
     * @param entityClass
     *         the class of a target entity
     * @param ids
     *         the IDs of interest of type {@link io.spine.base.Identifier#checkSupported(Class)
     *         which is supported as identifier}
     * @return an instance of {@code Query} formed according to the passed parameters
     * @throws IllegalArgumentException
     *         if any of IDs have invalid type or are {@code null}
     */
    public Query byIds(Class<? extends EntityState> entityClass, Set<?> ids) {
        checkSpecified(entityClass);
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
     * <p>If the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field), such invalid paths
     * are silently ignored.
     *
     * @param entityClass
     *         the class of a target entity
     * @param maskPaths
     *         the property paths for the {@code FieldMask} applied to each of the results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query allWithMask(Class<? extends EntityState> entityClass, String... maskPaths) {
        checkSpecified(entityClass);
        checkNotNull(maskPaths);
        FieldMask fieldMask = fromStringList(null, ImmutableList.copyOf(maskPaths));
        Query result = composeQuery(entityClass, null, null, fieldMask);
        return result;
    }

    /**
     * Creates a {@link Query} to read all states of a certain entity.
     *
     * <p>Unlike {@link #allWithMask(Class, String...)}, the {@code Query} processing will
     * not change the resulting entities.
     *
     * @param entityClass
     *         the class of a target entity
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query all(Class<? extends EntityState> entityClass) {
        checkSpecified(entityClass);
        return composeQuery(entityClass, null, null, null);
    }

    private Query composeQuery(Class<? extends EntityState> entityClass,
                               @Nullable Set<?> ids,
                               @Nullable Set<CompositeFilter> filters,
                               @Nullable FieldMask fieldMask) {
        ResponseFormat format = responseFormat(fieldMask, null, 0);
        Query.Builder builder = queryBuilderFor(entityClass, ids, filters)
                .setFormat(format);
        Query query = newQuery(builder);
        return query;
    }

    private static void checkSpecified(Class<? extends EntityState> entityClass) {
        checkNotNull(entityClass, "The class of `Entity` must be specified for a `Query`.");
    }

    private static Query.Builder queryBuilderFor(Class<? extends EntityState> entityClass,
                                                 @Nullable Set<?> ids,
                                                 @Nullable Set<CompositeFilter> filters) {
        Target target = composeTarget(entityClass, ids, filters);
        Query.Builder builder = queryBuilderFor(target);
        return builder;
    }

    Query composeQuery(Target target, @Nullable FieldMask fieldMask) {
        checkTargetNotNull(target);
        ResponseFormat format = responseFormat(fieldMask, null, 0);
        Query.Builder builder = queryBuilderFor(target)
                .setFormat(format);
        Query query = newQuery(builder);
        return query;
    }

    @SuppressWarnings("CheckReturnValue")
    private static Query.Builder queryBuilderFor(Target target) {
        Query.Builder builder = Query.newBuilder()
                                     .setTarget(target);
        return builder;
    }

    Query composeQuery(Target target,
                       OrderBy orderBy,
                       @Nullable FieldMask fieldMask) {
        checkTargetNotNull(target);
        checkNotNull(orderBy);
        ResponseFormat format = responseFormat(fieldMask, orderBy, 0);
        Query.Builder builder = queryBuilderFor(target)
                .setFormat(format);
        Query query = newQuery(builder);
        return query;
    }

    Query composeQuery(Target target,
                       OrderBy orderBy,
                       int limit,
                       @Nullable FieldMask fieldMask) {
        checkTargetNotNull(target);
        checkNotNull(orderBy);

        ResponseFormat format = responseFormat(fieldMask, orderBy, limit);
        Query.Builder builder = queryBuilderFor(target)
                .setFormat(format);
        Query query = newQuery(builder);
        return query;
    }

    private static void checkTargetNotNull(Target target) {
        checkNotNull(target, "Target must be specified to compose a `Query`.");
    }

    private Query newQuery(Query.Builder builder) {
        return builder.setId(newQueryId())
                      .setContext(actorContext)
                      .vBuild();
    }

    private static ResponseFormat responseFormat(@Nullable FieldMask mask,
                                                 @Nullable OrderBy ordering,
                                                 int limit) {
        ResponseFormat.Builder result = ResponseFormat
                .newBuilder();
        if (mask != null) {
            result.setFieldMask(mask);
        }
        if (ordering != null) {
            result.setOrderBy(ordering);
        }
        if (limit > 0) {
            result.setLimit(limit);
        }
        return result.vBuild();
    }
}
