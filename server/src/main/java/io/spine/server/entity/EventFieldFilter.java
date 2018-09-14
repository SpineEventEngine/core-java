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

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.type.TypeUrl;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.core.Events.getMessage;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.FieldMasks.applyMask;
import static io.spine.validate.Validate.isDefault;

/**
 * @author Dmytro Dashenkov
 */
public final class EventFieldFilter implements EventFilter {

    private final ImmutableMap<EventClass, FieldMask> fieldMasks;

    private EventFieldFilter(Builder builder) {
        this.fieldMasks = copyOf(builder.masks);
    }

    @Override
    public Optional<Event> filter(Event event) {
        EventClass eventClass = EventClass.of(event);
        FieldMask mask = fieldMasks.get(eventClass);
        if (mask == null || isDefault(mask)) {
            return Optional.of(event);
        } else {
            Event result = mask(event, eventClass, mask);
            return Optional.of(result);
        }
    }

    private static Event mask(Event event, EventClass eventClass, FieldMask mask) {
        Message eventMessage = getMessage(event);
        TypeUrl typeUrl = eventClass.getTypeName().toUrl();
        Message masked = applyMask(mask, eventMessage, typeUrl);
        Event result = event.toBuilder()
                            .setMessage(pack(masked))
                            .build();
        return result;
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
