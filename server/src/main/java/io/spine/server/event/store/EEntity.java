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

import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.EventVBuilder;
import io.spine.core.Events;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.Column;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.core.Events.clearEnrichments;

/**
 * An entity for storing an event.
 *
 * <p>An underlying event doesn't contain {@linkplain Events#clearEnrichments(Event) enrichments}.
 *
 * @apiNote This class is public so that {@link io.spine.server.entity.storage.EntityColumn
 * EntityColumn} can access its column declarations.
 */
@Internal
public final class EEntity extends TransactionalEntity<EventId, Event, EventVBuilder> {

    /** Cached value of the event message type name. */
    private @Nullable TypeName typeName;

    /**
     * Creates a new entity stripping enrichments from the passed event.
     */
    static EEntity create(Event event) {
        Factory factory = new Factory(event);
        EEntity result = factory.create();
        return result;
    }

    private EEntity(EventId id) {
        super(id);
    }

    /**
     * Retrieves the time of the event occurrence.
     *
     * <p>This method represents an entity column {@code created}.
     *
     * @return the time when the underlying event was fired
     * @see ColumnName#created
     */
    @Column
    public Timestamp getCreated() {
        return state().context()
                      .getTimestamp();
    }

    /**
     * Retrieves the Protobuf type name of the enclosed event.
     *
     * <p>This method represents an entity column {@link TypeName}.
     *
     * @return the {@link TypeName} value of the event represented by this entity
     * @see ColumnName#type
     */
    @Column
    public String getType() {
        if (typeName == null) {
            typeName = EventEnvelope.of(state())
                                    .messageTypeName();
        }
        return typeName.value();
    }

    /**
     * Creates a new entity stripping enrichments from the passed event.
     */
    private static class Factory extends Transaction<EventId, EEntity, Event, EventVBuilder> {

        private final Event event;

        private Factory(Event event) {
            super(new EEntity(event.getId()));
            this.event = event;
        }

        private EEntity create() {
            Event eventWithoutEnrichments = clearEnrichments(event);
            EEntity entity = entity();
            entity.builder()
                  .mergeFrom(eventWithoutEnrichments);
            commit();
            return entity;
        }
    }
}
