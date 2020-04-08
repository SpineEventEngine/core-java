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

package io.spine.server.storage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.server.entity.storage.EntityRecordWithColumns;

import java.util.Map;
import java.util.Set;

import static io.spine.server.storage.QueryParameters.FIELD_PARAMETERS;

/**
 * A query to a {@link RecordStorage MessageStorage} for the records
 * matching the given parameters.
 *
 * <p>Acts like a DTO between the
 * {@linkplain io.spine.server.entity.RecordBasedRepository repository} and the
 * {@link RecordStorage MessageStorage}.
 *
 * <p>The query contains the acceptable values of the record IDs and the
 * {@linkplain Column entity columns}.
 *
 * <p>A storage may ignore the query or throw an exception if it's specified. By default,
 * {@link RecordStorage MessageStorage} supports the Entity queries.
 *
 * <p>If the {@linkplain RecordQuery#getIds() accepted IDs set} is empty, all the IDs are
 * considered to be queried.
 *
 * <p>Empty {@linkplain RecordQuery#getParameters() query parameters} are not considered when
 * the actual data query is performed as well as the parameters which have no accepted values.
 *
 * <p>If the {@link Column} specified in the query is absent in a record,
 * the record is considered <b>not matching</b>.
 *
 * <p>If both the {@linkplain RecordQuery#getIds() accepted IDs set} and
 * {@linkplain RecordQuery#getParameters() query parameters} are empty, all the records are
 * considered matching.
 *
 * <p>If the query specifies the values of
 * the {@linkplain io.spine.server.entity.LifecycleFlags Entity lifecycle Columns}, then
 * the {@linkplain RecordStorage#readAll default behavior} will be
 * overridden meaning that the records resulting to such query may active or inactive.
 *
 * @param <I>
 *         the type of the IDs of the query target
 * @see EntityRecordWithColumns
 */
public final class RecordQuery<I> {

    private final ImmutableSet<I> ids;
    private final QueryParameters parameters;

    RecordQuery(Iterable<I> ids, QueryParameters parameters) {
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

    public RecordQuery<I> append(QueryParameters moreParams) {
        ImmutableList<CompositeQueryParameter> toAdd = ImmutableList.copyOf(moreParams.iterator());
        QueryParameters newParams = QueryParameters.newBuilder(this.parameters)
                                                   .addAll(toAdd)
                                                   .build();
        return new RecordQuery<>(ids, newParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecordQuery<?> query = (RecordQuery<?>) o;
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
                          .add("ID filter", ids)
                          .add(FIELD_PARAMETERS, parameters)
                          .toString();
    }
}
