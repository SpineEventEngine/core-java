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
import com.google.protobuf.Message;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.value.ValueHolder;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.client.Filters.composeFilters;
import static io.spine.client.Filters.extractFilters;

abstract class CompositeFilterHolder<M extends Message>
        extends ValueHolder<CompositeFilter>
        implements CompositeMessageFilter<M> {

    private static final long serialVersionUID = 0L;

    private final ImmutableList<MessageFilter<M>> filters;

    CompositeFilterHolder(CompositeFilter filter, Function<Filter, MessageFilter<M>> wrapper) {
        super(filter);
        this.filters = filter.getFilterList()
                             .stream()
                             .map(wrapper)
                             .collect(toImmutableList());
    }

    CompositeFilterHolder(Collection<? extends FilterHolder<M>> filters,
                          CompositeOperator operator) {
        super(composeFilters(extractFilters(filters), operator));
        this.filters = ImmutableList.copyOf(filters);
    }

    CompositeFilter filter() {
        return value();
    }

    @Override
    public List<MessageFilter<M>> filters() {
        return filters;
    }

    @Override
    public CompositeOperator operator() {
        return filter().operator();
    }
}
