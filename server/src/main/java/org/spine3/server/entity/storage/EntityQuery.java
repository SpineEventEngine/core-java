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

package org.spine3.server.entity.storage;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A query to a {@link org.spine3.server.storage.RecordStorage RecordStorage} for the records
 * matching the given parameters.
 *
 * <p>Acts like a DTO between the
 * {@linkplain org.spine3.server.entity.RecordBasedRepository repository} and the
 * {@link org.spine3.server.storage.RecordStorage RecordStorage}.
 *
 * <p>The query contains the acceptable values of the record IDs and the
 * {@linkplain Column Entity Columns}.
 *
 * <p>A storage may ignore the query or throw an exception if it's specified (see
 * {@link org.spine3.server.stand.StandStorage StandSotrage}). By default,
 * {@link org.spine3.server.storage.RecordStorage RecordStorage} supports the Entity queries.
 *
 * <p>If the {@linkplain EntityQuery#getIds() accepted IDs set} is empty, all the IDs are
 * considered to be queried.
 *
 * <p>Empty {@linkplain EntityQuery#getParameters() query parameters} are not considered when
 * the actual data query is performed as well as the parameters which have no accepted values.
 *
 * <p>If the {@linkplain Column Entity Column} specified in the query is absent in a record,
 * the record is considered <b>not matching</b>.
 *
 * <p>If both the {@linkplain EntityQuery#getIds() accepted IDs set} and
 * {@linkplain EntityQuery#getParameters() query parameters} are empty, all the records are
 * considered matching.
 *
 * @author Dmytro Dashenkov
 * @see EntityRecordWithColumns
 */
public final class EntityQuery {

    private final ImmutableSet<Object> ids;
    private final ImmutableMultimap<Column<?>, Object> parameters;

    /**
     * Creates new instance of {@code EntityQuery}.
     *
     * @param ids        accepted ID values
     * @param parameters the values of the {@link Column}s stored in a mapping of the
     *                   {@link Column}'s metadata to the (multiple) acceptable values; if there are
     *                   no values, all the values are matched upon such Column
     * @return new instance of {@code EntityQuery}
     */
    static EntityQuery of(Collection<?> ids, Multimap<Column<?>, Object> parameters) {
        checkNotNull(ids);
        checkNotNull(parameters);
        return new EntityQuery(ids, parameters);
    }

    private EntityQuery(Collection<?> ids, Multimap<Column<?>, Object> parameters) {
        this.ids = ImmutableSet.copyOf(ids);
        this.parameters = ImmutableMultimap.copyOf(parameters);
    }

    /**
     * @return a set of accepted ID values
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public ImmutableSet<Object> getIds() {
        return ids;
    }

    /**
     * @return a {@linkplain Multimap map} of the {@linkplain Column Column metadata} to the
     * accepted values of the column
     */
    public Multimap<Column<?>, Object> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityQuery that = (EntityQuery) o;
        return Objects.equal(ids, that.ids) &&
                Objects.equal(getParameters(), that.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids, getParameters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("idFilter", ids)
                          .add("parameters", parameters)
                          .toString();
    }
}
