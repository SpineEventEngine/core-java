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

package org.spine3.server.event;

import com.google.protobuf.Timestamp;
import org.spine3.annotation.Internal;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.server.entity.AbstractEntity;

import java.util.Comparator;

/**
 * Stores an event.
 *
 * <p>{@code EventEntity} is a Spine internal {@link org.spine3.server.entity.Entity Entity} type,
 * which is not designed for the direct usage. Both API and behaviour of this class may are subjects
 * of change.
 *
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
@Internal
public class EventEntity extends AbstractEntity<EventId, Event> {

    /**
     * The name of the Entity Column representing the time, when the event was fired.
     * @see #getCreated()
     */
    static final String CREATED_TIME_COLUMN = "created";

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

    /**
     * Retrieves the time of the event occurrence.
     *
     * <p>This method represents an Entity Column {@code time}.
     *
     * @return the time when the underlying event was fired
     * @see #CREATED_TIME_COLUMN
     */
    public Timestamp getCreated() {
        return getState().getContext()
                         .getTimestamp();
    }

    /**
     * Returns comparator which sorts event entities chronologically.
     */
    static Comparator<EventEntity> comparator() {
        return comparator;
    }
}
