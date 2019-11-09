/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.base.Field;
import io.spine.core.Event;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Filters events by conditions on both message and context.
 */
final class EventFilter implements MessageFilter<Event> {

    /** The name of the {@code context} field in the {@code Event} type. */
    private static final String CONTEXT_FIELD =
            Field.nameOf(Event.CONTEXT_FIELD_NUMBER, Event.getDescriptor());

    private final Filter filter;

    EventFilter(Filter filter) {
        this.filter = checkNotNull(filter);
    }

    @Override
    public boolean test(Event event) {
        boolean byContext =
                filter.getFieldPath()
                      .getFieldName(0)
                      .equals(CONTEXT_FIELD);
        if (byContext) {
            // Since we reference the context field with `context` prefix, we need to pass
            // the whole `Event` instance.
            return filter.test(event);
        }
        return filter.test(event.enclosedMessage());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventFilter other = (EventFilter) o;
        return filter.equals(other.filter);
    }

    @Override
    public int hashCode() {
        return filter.hashCode();
    }
}
