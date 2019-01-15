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

package io.spine.server.event.store;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.FieldFilter;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.server.event.EventFilter;
import io.spine.server.reflect.Field;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static io.spine.protobuf.AnyPacker.unpackFunc;
import static java.util.stream.Collectors.toList;

/**
 * The predicate for filtering events by {@link EventFilter}.
 */
final class MatchFilter implements Predicate<Event> {

    /**
     * The type URL of events to accept.
     *
     * <p>If null, all events are accepted.
     */
    private final @Nullable TypeUrl eventTypeUrl;

    /**
     * The list of aggregate IDs of which events to accept.
     *
     * <p>If null, all IDs are accepted.
     */
    private final @Nullable List<Any> aggregateIds;

    private final Collection<FieldFilter> eventFieldFilters;
    private final Collection<FieldFilter> contextFieldFilters;

    MatchFilter(EventFilter filter) {
        this.eventTypeUrl = getEventTypeUrl(filter);
        this.aggregateIds = getAggregateIdentifiers(filter);
        this.eventFieldFilters = filter.getEventFieldFilterList();
        this.contextFieldFilters = filter.getContextFieldFilterList();
    }

    private static @Nullable TypeUrl getEventTypeUrl(EventFilter filter) {
        String eventType = filter.getEventType();
        TypeUrl result = eventType.isEmpty()
                         ? null
                         : TypeName.of(eventType)
                                   .toUrl();
        return result;
    }

    private static @Nullable List<Any> getAggregateIdentifiers(EventFilter filter) {
        List<Any> aggregateIdList = filter.getAggregateIdList();
        List<Any> result = aggregateIdList.isEmpty()
                           ? null
                           : aggregateIdList;
        return result;
    }

    @Override
    public boolean test(@Nullable Event event) {
        if (event == null) {
            return false;
        }

        Message message = Events.getMessage(event);
        EventContext context = event.getContext();

        if (!checkEventType(message)) {
            return false;
        }

        if (!checkAggregateIds(context)) {
            return false;
        }

        if (!checkEventFields(message)) {
            return false;
        }

        boolean result = checkContextFields(context);
        return result;
    }

    private boolean checkAggregateIds(EventContext context) {
        if (aggregateIds == null) {
            return true;
        }
        Any aggregateId = context.getProducerId();
        boolean result = aggregateIds.contains(aggregateId);
        return result;
    }

    private boolean checkEventType(Message message) {
        TypeUrl actualTypeUrl = TypeUrl.of(message);
        if (eventTypeUrl == null) {
            return true;
        }
        boolean result = actualTypeUrl.equals(eventTypeUrl);
        return result;
    }

    private boolean checkContextFields(EventContext context) {
        for (FieldFilter filter : contextFieldFilters) {
            boolean matchesFilter = checkFields(context, filter);
            if (!matchesFilter) {
                return false;
            }
        }
        return true;
    }

    private boolean checkEventFields(Message message) {
        for (FieldFilter filter : eventFieldFilters) {
            boolean matchesFilter = checkFields(message, filter);
            if (!matchesFilter) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkFields(Message object, FieldFilter filter) {
        Optional<Field> fieldOptional = Field.forFilter(object.getClass(), filter);
        if (!fieldOptional.isPresent()) {
            return false;
        }

        Field field = fieldOptional.get();
        Optional<Message> value;

        try {
            value = field.getValue(object);
        } catch (IllegalStateException ignored) {
            // Wrong Message class -> does not satisfy the criteria.
            return false;
        }
        Collection<Message> expectedValues = filter.getValueList()
                                                   .stream()
                                                   .map(unpackFunc())
                                                   .collect(toList());
        if (!value.isPresent()) {
            /* If there is no value in the field return `true`
               if the list of required values is also empty. */
            boolean nothingIsExpected = expectedValues.isEmpty();
            return nothingIsExpected;
        }

        boolean result = expectedValues.contains(value.get());
        return result;
    }
}
