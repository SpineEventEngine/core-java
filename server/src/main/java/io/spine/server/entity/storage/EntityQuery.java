/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.server.storage.RecordStorage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.server.entity.storage.QueryParameters.FIELD_PARAMETERS;
import static io.spine.server.entity.storage.QueryParameters.activeEntityQueryParams;

/**
 * A query to a {@link io.spine.server.storage.RecordStorage RecordStorage} for the records
 * matching the given parameters.
 *
 * <p>Acts like a DTO between the
 * {@linkplain io.spine.server.entity.RecordBasedRepository repository} and the
 * {@link io.spine.server.storage.RecordStorage RecordStorage}.
 *
 * <p>The query contains the acceptable values of the record IDs and the
 * {@linkplain Column entity columns}.
 *
 * <p>A storage may ignore the query or throw an exception if it's specified. By default,
 * {@link io.spine.server.storage.RecordStorage RecordStorage} supports the Entity queries.
 *
 * <p>If the {@linkplain EntityQuery#getIds() accepted IDs set} is empty, all the IDs are
 * considered to be queried.
 *
 * <p>Empty {@linkplain EntityQuery#getParameters() query parameters} are not considered when
 * the actual data query is performed as well as the parameters which have no accepted values.
 *
 * <p>If the {@link Column} specified in the query is absent in a record,
 * the record is considered <b>not matching</b>.
 *
 * <p>If both the {@linkplain EntityQuery#getIds() accepted IDs set} and
 * {@linkplain EntityQuery#getParameters() query parameters} are empty, all the records are
 * considered matching.
 *
 * <p>If the query specifies the values of
 * the {@linkplain io.spine.server.entity.LifecycleFlags Entity lifecycle Columns}, then
 * the {@linkplain io.spine.server.storage.RecordStorage#readAll default behavior} will be
 * overridden meaning that the records resulting to such query may active or inactive.
 *
 * @param <I>
 *         the type of the IDs of the query target
 * @see EntityRecordWithColumns
 */
public final class EntityQuery<I> {

    private final ImmutableSet<I> ids;
    private final QueryParameters parameters;

    /**
     * Creates new instance of {@code EntityQuery}.
     *
     * @param ids
     *         the accepted ID values
     * @param parameters
     *         the values of the {@link Column}s stored in a mapping of the
     *         {@link Column}'s metadata to the (multiple) acceptable values;
     *         if there are no values, all the values are matched upon such a column
     * @return new instance of {@code EntityQuery}
     */
    static <I> EntityQuery<I> of(Collection<I> ids, QueryParameters parameters) {
        checkNotNull(ids);
        checkNotNull(parameters);
        return new EntityQuery<>(ids, parameters);
    }

    private EntityQuery(Collection<I> ids, QueryParameters parameters) {
        this.ids = ImmutableSet.copyOf(ids);
        this.parameters = parameters;
    }

    /**
     * Obtains an immutable set of accepted ID values.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public Set<I> getIds() {
        return ids;
    }

    /**
     * Obtains a {@link Map} of the {@link Column} metadata to the column required value.
     */
    public QueryParameters getParameters() {
        return parameters;
    }

    /**
     * Obtains {@code true} if the query overrides the default lifecycle handling strategy,
     * {@code false} otherwise.
     */
    @Internal
    public boolean isLifecycleAttributesSet() {
        return parameters.isLifecycleAttributesSet();
    }

    /**
     * Creates a new instance of {@code EntityQuery} with all the parameters from current instance
     * and the default values of the {@link io.spine.server.entity.LifecycleFlags LifecycleFlags}.
     *
     * <p>The precondition for this method is that current instance
     * {@linkplain #isLifecycleAttributesSet() does not specify the values}.
     *
     * @param storage
     *         the {@linkplain RecordStorage storage} for which this {@code EntityQuery} is created
     * @return new instance of {@code EntityQuery}
     */
    @Internal
    public EntityQuery<I> withActiveLifecycle(RecordStorage<I> storage) {
        checkState(canAppendLifecycleFlags(),
                   "The query overrides Lifecycle Flags default values.");
        QueryParameters parameters = QueryParameters.newBuilder(getParameters())
                                                    .addAll(activeEntityQueryParams(storage))
                                                    .build();
        EntityQuery<I> result = new EntityQuery<>(ids, parameters);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityQuery<?> query = (EntityQuery<?>) o;
        return Objects.equal(getIds(), query.getIds()) &&
                Objects.equal(getParameters(), query.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIds(), getParameters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("idFilter", ids)
                          .add(FIELD_PARAMETERS, parameters)
                          .toString();
    }

    @VisibleForTesting
    boolean canAppendLifecycleFlags() {
        return !isLifecycleAttributesSet();
    }
}
