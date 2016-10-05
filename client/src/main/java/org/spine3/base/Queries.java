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
import java.util.Arrays;
import java.util.Set;

import static org.spine3.base.Queries.Targets.allOf;
import static org.spine3.base.Queries.Targets.someOf;

/**
 * Client-side utilities for working with queries.
 *
 * @author Alex Tymchenko
 * @author Dmytro Dashenkov
 */
public class Queries {

    private Queries() {
    }

    /**
     * Create a {@link Query} to read certain entity states by IDs with the {@link FieldMask}
     * applied to each of the results.
     *
     * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing. The processing
     * results will contain only the entities, which IDs are present among the {@code ids}.
     *
     * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query results.
     * This processing is performed according to the
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask>FieldMask specs</a>.
     *
     * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field), such invalid paths are silently ignored.
     *
     * @param entityClass the class of a target entity
     * @param ids         the entity IDs of interest
     * @param paths       the property paths for the {@code FieldMask} applied to each of results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public static Query readByIds(Class<? extends Message> entityClass, Set<? extends Message> ids, String... paths) {
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();
        final Query result = composeQuery(entityClass, ids, fieldMask);
        return result;
    }

    /**
     * Create a {@link Query} to read all entity states with the {@link FieldMask}
     * applied to each of the results.
     *
     * <p>Allows to set property paths for a {@link FieldMask}, applied to each of the query results.
     * This processing is performed according to the
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/google.protobuf#google.protobuf.FieldMask>FieldMask specs</a>.
     *
     * <p>In case the {@code paths} array contains entries inapplicable to the resulting entity
     * (e.g. a {@code path} references a missing field), such invalid paths are silently ignored.
     *
     * @param entityClass the class of a target entity
     * @param paths       the property paths for the {@code FieldMask} applied to each of results
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public static Query readAll(Class<? extends Message> entityClass, String... paths) {
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addAllPaths(Arrays.asList(paths))
                                             .build();
        final Query result = composeQuery(entityClass, null, fieldMask);
        return result;
    }

    /**
     * Create a {@link Query} to read certain entity states by IDs.
     *
     * <p>Allows to specify a set of identifiers to be used during the {@code Query} processing. The processing
     * results will contain only the entities, which IDs are present among the {@code ids}.
     *
     * <p>Unlike {@link Queries#readByIds(Class, Set, String...)}, the {@code Query} processing will not change
     * the resulting entities.
     *
     * @param entityClass the class of a target entity
     * @param ids         the entity IDs of interest
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public static Query readByIds(Class<? extends Message> entityClass, Set<? extends Message> ids) {
        return composeQuery(entityClass, ids, null);
    }

    /**
     * Create a {@link Query} to read all states of a certain entity.
     *
     * <p>Unlike {@link Queries#readAll(Class, String...)}, the {@code Query} processing will not change
     * the resulting entities.
     *
     * @param entityClass the class of a target entity
     * @return an instance of {@code Query} formed according to the passed parameters
     */
    public static Query readAll(Class<? extends Message> entityClass) {
        return composeQuery(entityClass, null, null);
    }

    private static Query composeQuery(Class<? extends Message> entityClass, @Nullable Set<? extends Message> ids, @Nullable FieldMask fieldMask) {
        final Target target = ids == null ? allOf(entityClass) : someOf(entityClass, ids);
        final Query.Builder queryBuilder = Query.newBuilder()
                                                .setTarget(target);
        if (fieldMask != null) {
            queryBuilder.setFieldMask(fieldMask);
        }
        final Query result = queryBuilder
                .build();
        return result;
    }

    /**
     * Client-side utilities for working with {@link Query} and {@link org.spine3.client.Subscription} targets.
     *
     * @author Alex Tymchenko
     * @author Dmytro Dashenkov
     */
    public static class Targets {

        private Targets() {
        }

        /**
         * Create a {@link Target} for a subset of the entity states by specifying their IDs.
         *
         * @param entityClass the class of a target entity
         * @param ids         the IDs of interest
         * @return the instance of {@code Target} assembled according to the parameters.
         */
        public static Target someOf(Class<? extends Message> entityClass, Set<? extends Message> ids) {
            final Target result = composeTarget(entityClass, ids);
            return result;
        }

        /**
         * Create a {@link Target} for all of the specified entity states.
         *
         * @param entityClass the class of a target entity
         * @return the instance of {@code Target} assembled according to the parameters.
         */
        public static Target allOf(Class<? extends Message> entityClass) {
            final Target result = composeTarget(entityClass, null);
            return result;
        }

        /* package */
        static Target composeTarget(Class<? extends Message> entityClass, @Nullable Set<? extends Message> ids) {
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
            final Target.Builder builder = Target.newBuilder()
                                                 .setType(type.getTypeName());
            if (includeAll) {
                builder.setIncludeAll(true);
            } else {
                builder.setFilters(filters);
            }

            return builder.build();
        }
    }
}
