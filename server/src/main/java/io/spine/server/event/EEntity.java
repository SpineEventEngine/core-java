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
import io.spine.annotation.Internal;
import io.spine.base.Event;
import io.spine.base.EventId;
import io.spine.base.Events;
import io.spine.server.entity.AbstractEntity;
import io.spine.type.TypeName;

import java.util.Comparator;

import static io.spine.base.Events.typeNameOf;

/**
 * Stores an event.
 *
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
@Internal
public class EEntity extends AbstractEntity<EventId, Event> {

    /**
     * The name of the Entity Column representing the time, when the event was fired.
     *
     * @see #getCreated()
     */
    static final String CREATED_TIME_COLUMN = "created";

    /**
     * The name of the Entity Column representing the Protobuf type name of the event.
     *
     * <p>For example, an Event of type {@code io.spine.test.TaskAdded} whose definition is enclosed
     * in the {@code spine.test} Protobuf package would have this Column equal to
     * {@code "spine.test.TaskAdded"}.
     *
     * @see #getType()
     */
    static final String TYPE_COLUMN = "type";

    /**
     * Compares event entities by timestamps of events.
     */
    private static final Comparator<EEntity> comparator = new Comparator<EEntity>() {
        @Override
        public int compare(EEntity e1, EEntity e2) {
            final Event event1 = e1.getState();
            final Event event2 = e2.getState();
            final int result = Events.eventComparator()
                                     .compare(event1, event2);
            return result;
        }
    };

    EEntity(EventId id) {
        super(id);
    }

    EEntity(Event event) {
        this(event.getId());
        updateState(event);
    }

    /**
     * Returns comparator which sorts event entities chronologically.
     */
    static Comparator<EEntity> comparator() {
        return comparator;
    }

    /**
     * Retrieves the time of the event occurrence.
     *
     * <p>This method represents an Entity Column {@code created}.
     *
     * @return the time when the underlying event was fired
     * @see #CREATED_TIME_COLUMN
     */
    public Timestamp getCreated() {
        return getState().getContext()
                         .getTimestamp();
    }

    /**
     * Retrieves the Protobuf type name of the enclosed event.
     *
     * <p>This method represents an Entity Column {@code type}.
     *
     * @return the {@link TypeName} value of the event represented by this Entity
     * @see #TYPE_COLUMN
     */
    public String getType() {
        final TypeName typeName = typeNameOf(getState());
        return typeName.value();
    }
}
