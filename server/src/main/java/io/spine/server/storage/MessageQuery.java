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
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.QueryParameters;

import java.util.Map;
import java.util.Set;

import static io.spine.server.entity.storage.QueryParameters.FIELD_PARAMETERS;

/**
 * @author Alex Tymchenko
 */
public final class MessageQuery<I> {

    private final ImmutableSet<I> ids;
    private final QueryParameters parameters;

    MessageQuery(Iterable<I> ids, QueryParameters parameters) {
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

    public MessageQuery<I> append(QueryParameters moreParams) {
        ImmutableList<CompositeQueryParameter> toAdd = ImmutableList.copyOf(moreParams.iterator());
        QueryParameters newParams = QueryParameters.newBuilder(this.parameters)
                                                   .addAll(toAdd)
                                                   .build();
        return new MessageQuery<>(ids, newParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessageQuery<?> query = (MessageQuery<?>) o;
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
