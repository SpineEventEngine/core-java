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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.EntityState;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

/**
 * Extends {@link SubscriptionUpdate} with useful methods.
 */
@GeneratedMixin
interface SubscriptionUpdateMixin {

    @SuppressWarnings("override") // for generated code
    EntityUpdates getEntityUpdates();

    @SuppressWarnings("override") // for generated code
    EventUpdates getEventUpdates();

    /**
     * Obtains the entity state at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of the range of entities returned by this update
     */
    default EntityState state(int index) {
        EntityStateUpdate stateUpdate =
                getEntityUpdates().getUpdateList()
                                  .get(index);
        EntityState result = (EntityState) AnyPacker.unpack(stateUpdate.getState());
        return result;
    }

    /**
     * Obtains an immutable list of stored entity states.
     */
    default List<EntityState> states() {
        ImmutableList<EntityState> result =
                getEntityUpdates().getUpdateList()
                                  .stream()
                                  .map(EntityStateUpdate::getState)
                                  .map(AnyPacker::unpack)
                                  .map(EntityState.class::cast)
                                  .collect(toImmutableList());
        return result;
    }

    /**
     * Obtains the event at the given index.
     *
     * @throws IndexOutOfBoundsException
     *         if the index is out of the range of events returned by this update
     */
    default Event event(int index) {
        Event result = getEventUpdates().getEvent(index);
        return result;
    }

    /**
     * Obtains an immutable list of stored events.
     */
    default List<Event> events() {
        List<Event> events = getEventUpdates().getEventList();
        ImmutableList<Event> result = ImmutableList.copyOf(events);
        return result;
    }

    /**
     * Obtains an immutable list of stored event messages.
     */
    default List<EventMessage> eventMessages() {
        ImmutableList<EventMessage> result =
                events().stream()
                        .map(Event::getMessage)
                        .map(AnyPacker::unpack)
                        .map(EventMessage.class::cast)
                        .collect(toImmutableList());
        return result;
    }
}
