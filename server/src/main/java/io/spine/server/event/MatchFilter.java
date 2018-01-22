/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.FieldFilter;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.server.reflect.Field;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static io.spine.protobuf.AnyPacker.unpackFunc;

/**
 * The predicate for filtering events by {@link EventFilter}.
 *
 * @author Dmytro Dashenkov
 * @author Alexander Yevsyukov
 */
class MatchFilter implements Predicate<Event> {

    /**
     * The type URL of events to accept.
     *
     * <p>If null, all events are accepted.
     */
    @Nullable
    private final TypeUrl eventTypeUrl;

    /**
     * The list of aggregate IDs of which events to accept.
     *
     * <p>If null, all IDs are accepted.
     */
    @Nullable
    private final List<Any> aggregateIds;

    private final Collection<FieldFilter> eventFieldFilters;
    private final Collection<FieldFilter> contextFieldFilters;

    MatchFilter(EventFilter filter) {
        this.eventTypeUrl = getEventTypeUrl(filter);
        this.aggregateIds = getAggregateIdentifiers(filter);
        this.eventFieldFilters = filter.getEventFieldFilterList();
        this.contextFieldFilters = filter.getContextFieldFilterList();
    }

    @Nullable
    private static TypeUrl getEventTypeUrl(EventFilter filter) {
        final String eventType = filter.getEventType();
        final TypeUrl result = eventType.isEmpty()
                               ? null
                               : TypeName.of(eventType)
                                         .toUrl();
        return result;
    }

    @Nullable
    private static List<Any> getAggregateIdentifiers(EventFilter filter) {
        final List<Any> aggregateIdList = filter.getAggregateIdList();
        final List<Any> result = aggregateIdList.isEmpty()
                            ? null
                            : aggregateIdList;
        return result;
    }

    @SuppressWarnings("MethodWithMoreThanThreeNegations") // OK as we want traceability of exits.
    @Override
    public boolean apply(@Nullable Event event) {
        if (event == null) {
            return false;
        }

        final Message message = Events.getMessage(event);
        final EventContext context = event.getContext();

        if (!checkEventType(message)) {
            return false;
        }

        if (!checkAggregateIds(context)) {
            return false;
        }

        if (!checkEventFields(message)) {
            return false;
        }

        final boolean result = checkContextFields(context);
        return result;
    }

    private boolean checkAggregateIds(EventContext context) {
        if (aggregateIds == null) {
            return true;
        }
        final Any aggregateId = context.getProducerId();
        final boolean result = aggregateIds.contains(aggregateId);
        return result;
    }

    private boolean checkEventType(Message message) {
        final TypeUrl actualTypeUrl = TypeUrl.of(message);
        if (eventTypeUrl == null) {
            return true;
        }
        final boolean result = actualTypeUrl.equals(eventTypeUrl);
        return result;
    }

    private boolean checkContextFields(EventContext context) {
        for (FieldFilter filter : contextFieldFilters) {
            final boolean matchesFilter = checkFields(context, filter);
            if (!matchesFilter) {
                return false;
            }
        }
        return true;
    }

    private boolean checkEventFields(Message message) {
        for (FieldFilter filter : eventFieldFilters) {
            final boolean matchesFilter = checkFields(message, filter);
            if (!matchesFilter) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkFields(Message object, FieldFilter filter) {
        final Optional<Field> fieldOptional = Field.forFilter(object.getClass(), filter);
        if (!fieldOptional.isPresent()) {
            return false;
        }

        final Field field = fieldOptional.get();
        final Optional<Message> value;

        try {
            value = field.getValue(object);
        } catch (IllegalStateException ignored) {
            // Wrong Message class -> does not satisfy the criteria.
            return false;
        }

        final Collection<Any> expectedAnys = filter.getValueList();
        final Collection<Message> expectedValues =
                Collections2.transform(expectedAnys, unpackFunc());

        if (!value.isPresent()) {
            /* If there is no value in the field return `true`
               if the list of required values is also empty. */
            final boolean nothingIsExpected = expectedValues.isEmpty();
            return nothingIsExpected;
        }

        final boolean result = expectedValues.contains(value.get());
        return result;
    }
}
