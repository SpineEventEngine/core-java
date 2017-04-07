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
import org.spine3.time.ZoneOffset;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.client.Queries.queryBuilderFor;

/**
 * * The factory to generate new {@link Query} instances.
 *
 * @author Alex Tymchenko
 */
public class QueryFactory extends ActorRequestFactory<QueryFactory> {

    protected QueryFactory(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
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
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask">FieldMask specs</a>.
     *
     * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field), such invalid paths are silently ignored.
     *
     * @param entityClass the class of a target entity
     * @param ids         the entity IDs of interest
     * @param paths       the property paths for the {@code FieldMask} applied to each of results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query readByIds(Class<? extends Message> entityClass,
                           Set<? extends Message> ids,
                           String... paths) {
        checkNotNull(ids);
        checkArgument(!ids.isEmpty(), "Entity ID set must not be empty");

        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();
        final Query result = composeQuery(entityClass, ids, fieldMask);
        return result;
    }

    /**
     * Creates a {@link Query} to read all entity states with the {@link FieldMask}
     * applied to each of the results.
     *
     * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query
     * results. This processing is performed according to the
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask">FieldMask specs</a>.
     *
     * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field), such invalid paths are silently ignored.
     *
     * @param entityClass the class of a target entity
     * @param paths       the property paths for the {@code FieldMask} applied to each of results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query readAll(Class<? extends Message> entityClass, String... paths) {
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();
        final Query result = composeQuery(entityClass, null, fieldMask);
        return result;
    }

    /**
     * Creates a {@link Query} to read certain entity states by IDs.
     *
     * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing.
     * The processing results will contain only the entities, which IDs are present among
     * the {@code ids}.
     *
     * <p>Unlike {@link #readByIds(Class, Set, String...)}, the {@code Query} processing
     * will not change the resulting entities.
     *
     * @param entityClass the class of a target entity
     * @param ids         the entity IDs of interest
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query readByIds(Class<? extends Message> entityClass,
                           Set<? extends Message> ids) {
        return composeQuery(entityClass, ids, null);
    }

    /**
     * Creates a {@link Query} to read all states of a certain entity.
     *
     * <p>Unlike {@link #readAll(Class, String...)}, the {@code Query} processing will
     * not change the resulting entities.
     *
     * @param entityClass the class of a target entity
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public Query readAll(Class<? extends Message> entityClass) {
        return composeQuery(entityClass, null, null);
    }

    private Query composeQuery(Class<? extends Message> entityClass,
                                      @Nullable Set<? extends Message> ids,
                                      @Nullable FieldMask fieldMask) {
        checkNotNull(entityClass, "The class of Entity must be specified for a Query");

        final Query.Builder builder = queryBuilderFor(entityClass, ids, fieldMask);
        builder.setContext(actorContext());
        return builder.build();
    }

    /**
     * Creates new factory with the same user and tenant ID, but with new time zone offset.
     *
     * @param zoneOffset the offset of the time zone
     * @return new query factory at new time zone
     */
    @Override
    public QueryFactory switchTimezone(ZoneOffset zoneOffset) {
        return switchTimezone(zoneOffset, newBuilder());
    }

    public static class Builder
            extends ActorRequestFactory.AbstractBuilder<QueryFactory, QueryFactory.Builder> {

        @Override
        protected QueryFactory.Builder thisInstance() {
            return this;
        }

        @Override
        public QueryFactory build() {
            super.build();
            final QueryFactory result = new QueryFactory(this);
            return result;
        }
    }
}
