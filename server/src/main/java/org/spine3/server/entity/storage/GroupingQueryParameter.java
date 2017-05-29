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
import com.google.common.collect.Multimap;
import org.spine3.client.GroupingColumnFilter.GroupingOperator;
import org.spine3.client.ColumnFilter;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMultimap.copyOf;

/**
 * A set of {@link ColumnFilter} instances joined by a logical aggregating
 * {@link GroupingOperator operator}.
 *
 * @author Dmytro Dashenkov
 */
public final class GroupingQueryParameter implements Serializable {

    private static final long serialVersionUID = -475685759190562528L;

    private final GroupingOperator operator;

    private final ImmutableMultimap<Column, ColumnFilter> filters;

    /**
     * Creates a new instance of {@code GroupingQueryParameter} from the given filters joined
     * by the given operator.
     *
     * @param filters  the filters to aggregate
     * @param operator the operator to apply to the given filters
     * @return new instance of {@code GroupingQueryParameter}
     */
    static GroupingQueryParameter from(Multimap<Column, ColumnFilter> filters,
                                          GroupingOperator operator) {
        checkNotNull(filters);
        checkNotNull(operator);
        checkArgument(operator.getNumber() > 0, "Invalid aggregating operator %s.", operator);

        return new GroupingQueryParameter(operator, filters);
    }

    private GroupingQueryParameter(GroupingOperator operator,
                                      Multimap<Column, ColumnFilter> filters) {
        this.operator = operator;
        this.filters = copyOf(filters);
    }

    /**
     * @return the aggregating operator
     */
    public GroupingOperator getOperator() {
        return operator;
    }

    /**
     * @return the aggregated {@link ColumnFilter Column filters}
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public ImmutableMultimap<Column, ColumnFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupingQueryParameter parameter = (GroupingQueryParameter) o;
        return getOperator() == parameter.getOperator() &&
                Objects.equal(getFilters(), parameter.getFilters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getOperator(), getFilters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("operator", operator)
                          .add("filters", filters)
                          .toString();
    }
}
