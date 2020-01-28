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

import io.spine.base.EventContextField;
import io.spine.base.EventMessageField;
import io.spine.core.Event;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.client.Filters.checkSupportedOrderingComparisonType;
import static io.spine.client.Filters.createContextFilter;
import static io.spine.client.Filters.createFilter;

/**
 * Filters events by conditions on both message and context.
 */
public final class EventFilter extends TypedFilter<Event> {

    private static final long serialVersionUID = 0L;

    private final boolean byContext;

    private EventFilter(Filter filter, boolean byContext) {
        super(filter);
        this.byContext = byContext;
    }

    private EventFilter(EventMessageField field, Object expected, Filter.Operator operator) {
        this(createFilter(field.getField(), expected, operator), false);
    }

    private EventFilter(EventContextField field, Object expected, Filter.Operator operator) {
        this(createContextFilter(field.getField(), expected, operator), true);
    }

    EventFilter(Filter filter) {
        this(filter, isContextFilter(filter));
    }

    public static EventFilter eq(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return new EventFilter(field, value, EQUAL);
    }

    public static EventFilter eq(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return new EventFilter(field, value, EQUAL);
    }

    public static EventFilter gt(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_THAN);
    }

    public static EventFilter gt(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_THAN);
    }

    public static EventFilter lt(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_THAN);
    }

    public static EventFilter lt(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_THAN);
    }

    public static EventFilter ge(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_OR_EQUAL);
    }

    public static EventFilter ge(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_OR_EQUAL);
    }

    public static EventFilter le(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_OR_EQUAL);
    }

    public static EventFilter le(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_OR_EQUAL);
    }

    @Override
    public boolean test(Event event) {
        if (byContext) {
            // Since we reference the context field with `context` prefix, we need to pass
            // the whole `Event` instance.
            return filter().test(event);
        }
        return filter().test(event.enclosedMessage());
    }

    private static boolean isContextFilter(Filter filter) {
        String contextFieldName = Event.Fields.context()
                                              .getField()
                                              .toString();
        String firstInPath = filter.getFieldPath()
                                   .getFieldName(0);
        boolean result = contextFieldName.equals(firstInPath);
        return result;
    }
}
