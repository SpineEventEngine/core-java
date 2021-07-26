/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.event;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

/**
 * A common interface for objects which need to dispatch {@linkplain io.spine.core.Event events},
 * but are unable to implement {@link io.spine.server.event.EventDispatcher EventDispatcher}.
 *
 * <p>This interface defines own contract (instead of extending {@link
 * io.spine.server.bus.MessageDispatcher MessageDispatcher} to allow classes that dispatch
 * messages other than events (by implementing {@link io.spine.server.bus.MessageDispatcher
 * MessageDispatcher}), and dispatch events by implementing this interface.
 *
 * @see DelegatingEventDispatcher
 */
@Internal
public interface EventDispatcherDelegate {

    /**
     * Obtains all event classes dispatched by this delegate.
     */
    ImmutableSet<EventClass> events();

    /**
     * Obtains domestic event classes dispatched by this delegate.
     */
    ImmutableSet<EventClass> domesticEvents();

    /**
     * Obtains external event classes dispatched by this delegate.
     */
    ImmutableSet<EventClass> externalEvents();

    /**
     * Dispatches the event.
     */
    void dispatchEvent(EventEnvelope event);

    /**
     * Returns immutable set with one element with the identity of the multicast dispatcher
     * that dispatches messages to itself.
     *
     * @implNote The identity obtained as the result of {@link Object#toString()
     * EventDispatcherDelegate.toString()}.
     *
     * @return immutable set with the dispatcher delegate identity
     */
    default ImmutableSet<String> identity() {
        return ImmutableSet.of(this.toString());
    }

    /**
     * Verifies if this instance dispatches at least one event.
     */
    default boolean dispatchesEvents() {
        return !events().isEmpty();
    }

    /**
     * Verifies if this instance dispatches at least one external event.
     */
    default boolean dispatchesExternalEvents() {
        return !externalEvents().isEmpty();
    }

    /**
     * Checks if this dispatcher can dispatch the given event.
     *
     * <p>By default, all events are permitted. Implementations may change this behaviour to reject
     * certain events as early as possible.
     *
     * @param envelope
     *         event to dispatch
     * @return {@code true} if this dispatcher can dispatch the given event, {@code false} otherwise
     */
    default boolean canDispatchEvent(EventEnvelope envelope) {
        return true;
    }
}
