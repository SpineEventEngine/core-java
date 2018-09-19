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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.Events;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.FieldMasks.applyMask;
import static io.spine.validate.Validate.isDefault;

/**
 * An {@link EventFilter} which allows all the events but trims some of their message fields.
 *
 * <p>By default, the input event is returned unchanged. Specify a {@link FieldMask} for an event
 * type in order to trim certain fields off the event messages (the fields in the mask are
 * the retained, all the others are cleared).
 *
 * <p>Note that the mask should contain all the {@code (required) = true} fields. Otherwise,
 * the event will not be acknowledged by the bus.
 *
 * @author Dmytro Dashenkov
 */
public final class EventFieldFilter implements EventFilter {

    private final ImmutableMap<EventClass, FieldMask> fieldMasks;

    private EventFieldFilter(Builder builder) {
        this.fieldMasks = copyOf(builder.masks);
    }

    @Override
    public Optional<? extends EventMessage> filter(EventMessage event) {
        EventMessage masked = mask(event);
        return Optional.of(masked);
    }

    @Override
    public ImmutableCollection<Event> filter(Collection<Event> events) {
        return events.stream()
                     .map(this::maskEvent)
                     .collect(toImmutableList());
    }

    private Event maskEvent(Event event) {
        EventMessage message = Events.getMessage(event);
        EventMessage masked = mask(message);
        return event.toBuilder()
                    .setMessage(pack(masked))
                    .build();
    }

    private EventMessage mask(EventMessage event) {
        EventClass eventClass = EventClass.of(event);
        FieldMask mask = fieldMasks.get(eventClass);
        if (mask == null || isDefault(mask)) {
            return event;
        } else {
            TypeUrl typeUrl = eventClass.getTypeName().toUrl();
            EventMessage maskedEvent = applyMask(mask, event, typeUrl);
            return maskedEvent;
        }
    }

    /**
     * Creates a new instance of {@code Builder} for {@code EventFieldFilter} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code EventFieldFilter} instances.
     */
    public static final class Builder {

        private final Map<EventClass, FieldMask> masks = newHashMap();

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        /**
         * Specifies the {@link FieldMask} for the given event type.
         *
         * @param eventClass
         *         the type of the event to mask
         * @param mask
         *         the fields to <b>retain</b> in the event message
         * @return self for method chaining
         */
        public Builder putMask(Class<? extends Message> eventClass, FieldMask mask) {
            checkNotNull(eventClass);
            checkNotNull(mask);
            EventClass eventType = EventClass.from(eventClass);
            masks.put(eventType, mask);
            return this;
        }

        /**
         * Creates a new instance of {@code EventFieldFilter}.
         *
         * @return new instance of {@code EventFieldFilter}
         */
        public EventFieldFilter build() {
            return new EventFieldFilter(this);
        }
    }
}
