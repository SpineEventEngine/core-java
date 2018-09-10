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

package io.spine.server.entity;

import com.google.common.collect.ImmutableList;
import io.spine.core.Event;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author Dmytro Dashenkov
 */
public final class CompositeEventFilter implements EventFilter {

    private final ImmutableList<EventFilter> filters;

    private CompositeEventFilter(Builder builder) {
        this.filters = builder.filters.build();
    }

    @Override
    public Optional<Event> filter(Event event) {
        Optional<Event> result = of(event);
        for (EventFilter filter : filters) {
            result = filter.filter(result.get());
            if (!result.isPresent()) {
                return empty();
            }
        }
        return result;
    }

    /**
     * Creates a new instance of {@code Builder} for {@code CompositeEventFilter} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code CompositeEventFilter} instances.
     */
    public static final class Builder {

        private final ImmutableList.Builder<EventFilter> filters = ImmutableList.builder();

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        public Builder add(EventFilter filter) {
            checkNotNull(filter);
            filters.add(filter);
            return this;
        }

        /**
         * Creates a new instance of {@code CompositeEventFilter}.
         *
         * @return new instance of {@code CompositeEventFilter}
         */
        public CompositeEventFilter build() {
            return new CompositeEventFilter(this);
        }
    }
}
