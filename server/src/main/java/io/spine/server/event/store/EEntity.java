/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.SystemColumn;
import io.spine.type.TypeName;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * An entity for storing an event.
 *
 * <p>An underlying event doesn't contain {@linkplain Event#clearEnrichments() enrichments}.
 */
final class EEntity extends TransactionalEntity<EventId, Event, Event.Builder> {

    static final String CREATED_COLUMN = "created";
    /** Cached value of the event message type name. */
    private @MonotonicNonNull TypeName typeName;

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
    @SystemColumn(name = CREATED_COLUMN)
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
    @SystemColumn(name = "type")
    public String getType() {
        if (typeName == null) {
            typeName = state().enclosedTypeUrl()
                              .toTypeName();
        }
        return typeName.value();
    }

    /**
     * Creates a new entity stripping enrichments from the passed event.
     */
    private static class Factory extends Transaction<EventId, EEntity, Event, Event.Builder> {

        private final Event event;

        private Factory(Event event) {
            super(new EEntity(event.getId()));
            this.event = event;
        }

        private EEntity create() {
            Event eventWithoutEnrichments = event.clearEnrichments();
            EEntity entity = entity();
            entity.builder()
                  .mergeFrom(eventWithoutEnrichments);
            commit();
            return entity;
        }
    }
}
