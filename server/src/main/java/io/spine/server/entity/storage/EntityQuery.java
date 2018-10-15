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

package io.spine.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.server.storage.RecordStorage;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.server.entity.storage.QueryParameters.FIELD_PARAMETERS;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;

/**
 * A query to a {@link io.spine.server.storage.RecordStorage RecordStorage} for the records
 * matching the given parameters.
 *
 * <p>Acts like a DTO between the
 * {@linkplain io.spine.server.entity.RecordBasedRepository repository} and the
 * {@link io.spine.server.storage.RecordStorage RecordStorage}.
 *
 * <p>The query contains the acceptable values of the record IDs and the
 * {@linkplain EntityColumn entity columns}.
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
 * <p>If the {@link EntityColumn} specified in the query is absent in a record,
 * the record is considered <b>not matching</b>.
 *
 * <p>If both the {@linkplain EntityQuery#getIds() accepted IDs set} and
 * {@linkplain EntityQuery#getParameters() query parameters} are empty, all the records are
 * considered matching.
 *
 * <p>If the query specifies the values of
 * the {@linkplain io.spine.server.entity.LifecycleFlags Entity lifecycle Columns}, then
 * the {@linkplain io.spine.server.storage.RecordStorage#readAll(EntityQuery,
 * com.google.protobuf.FieldMask) default behavior} will be overridden i.e. the records resulting
 * to such query may or may not be active.
 *
 * @param <I>
 *         the type of the IDs of the query target
 * @author Dmytro Dashenkov
 * @see EntityRecordWithColumns
 */
public final class EntityQuery<I> implements Serializable {

    private static final long serialVersionUID = 0L;

    private final ImmutableSet<I> ids;
    private final QueryParameters parameters;

    /**
     * Creates new instance of {@code EntityQuery}.
     *
     * @param ids
     *         accepted ID values
     * @param parameters
     *         the values of the {@link EntityColumn}s stored in a mapping of the
     *         {@link EntityColumn}'s metadata to the (multiple) acceptable values;
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
     * Obtains a {@link Map} of the {@link EntityColumn} metadata to the column required value.
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
     * and the default values of the {@link io.spine.server.entity.LifecycleFlags LifecycleFlags}
     * expected.
     *
     * <p>The precondition for this method is that current instance
     * {@linkplain #isLifecycleAttributesSet() does not specify the values}.
     *
     * @param storage
     *         the {@linkplain RecordStorage storage} for which this {@code EntityQuery} is created
     * @return new instance of {@code EntityQuery}
     */
    @Internal
    public EntityQuery<I> withLifecycleFlags(RecordStorage<I> storage) {
        checkState(canAppendLifecycleFlags(),
                   "The query overrides Lifecycle Flags default values.");
        Map<String, EntityColumn> lifecycleColumns = storage.entityLifecycleColumns();
        EntityColumn archivedColumn = lifecycleColumns.get(archived.name());
        EntityColumn deletedColumn = lifecycleColumns.get(deleted.name());
        CompositeQueryParameter lifecycleParameter = CompositeQueryParameter.from(
                ImmutableMultimap.of(archivedColumn, eq(archived.name(), false),
                                     deletedColumn, eq(deletedColumn.getName(), false)),
                ALL
        );
        QueryParameters parameters = QueryParameters.newBuilder()
                                                    .addAll(getParameters())
                                                    .add(lifecycleParameter)
                                                    .orderBy(getParameters().orderBy())
                                                    .limit(getParameters().limit())
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
