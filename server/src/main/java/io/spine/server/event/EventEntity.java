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

package io.spine.server.event;

import com.google.protobuf.Timestamp;
import io.spine.base.Event;
import io.spine.base.EventId;
import io.spine.base.Events;
import io.spine.annotation.Internal;
import io.spine.server.entity.AbstractEntity;
import io.spine.type.TypeName;

import java.util.Comparator;

/**
 * Stores an event.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class EventEntity extends AbstractEntity<EventId, Event> {

    public static final String CREATED_TIME_COLUMN = "created";

    public static final String TYPE_COLUMN = "type";

    /**
     * Compares event entities by timestamps of events.
     */
    private static final Comparator<EventEntity> comparator = new Comparator<EventEntity>() {
        @Override
        public int compare(EventEntity e1, EventEntity e2) {
            final Event event1 = e1.getState();
            final Event event2 = e2.getState();
            final int result = Events.eventComparator()
                                     .compare(event1, event2);
            return result;
        }
    };

    EventEntity(EventId id) {
        super(id);
    }

    EventEntity(Event event) {
        this(event.getId());
        updateState(event);
    }

    public Timestamp getCreated() {
        return getState().getContext()
                         .getTimestamp();
    }

    public String getType() {
        return TypeName.ofEvent(getState())
                       .value();
    }

    /**
     * Returns comparator which sorts event entities chronologically.
     */
    static Comparator<EventEntity> comparator() {
        return comparator;
    }
}
