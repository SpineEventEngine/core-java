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
import io.spine.core.Event;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.client.Filters.composeFilters;
import static java.util.stream.Collectors.toList;

/**
 * Filters events by composite criteria which can test both event messages and their contexts.
 */
public final class CompositeEventFilter implements CompositeMessageFilter<Event> {

    /** The filter data as composed when creating a topic. */
    private final CompositeFilter filter;
    /** Filters adopted to filter events basing on the passed filter data. */
    private final ImmutableList<MessageFilter<Event>> filters;

    CompositeEventFilter(CompositeFilter filter) {
        this.filter = checkNotNull(filter);
        this.filters =
                filter.getFilterList()
                      .stream()
                      .map(EventFilter::new)
                      .collect(toImmutableList());
    }

    CompositeEventFilter(Collection<EventFilter> filters,
                         CompositeFilter.CompositeOperator operator) {
        List<Filter> filterList = checkNotNull(filters)
                .stream()
                .map(EventFilter::filter)
                .collect(toList());
        this.filter = composeFilters(filterList, operator);
        this.filters = ImmutableList.copyOf(filters);
    }

    @Override
    public List<MessageFilter<Event>> filters() {
        return filters;
    }

    @Override
    public CompositeFilter.CompositeOperator operator() {
        return filter.operator();
    }

    CompositeFilter filter() {
        return filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositeEventFilter other = (CompositeEventFilter) o;
        return filter.equals(other.filter);
    }

    @Override
    public int hashCode() {
        return filter.hashCode();
    }
}
