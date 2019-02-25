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

package io.spine.server.event;

import io.spine.server.bus.MulticastDispatcher;
import io.spine.server.integration.ExternalDispatcherFactory;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Set;

/**
 * {@code EventDispatcher} delivers events to {@linkplain EventReceiver receiving} objects.
 *
 * @param <I> the type of entity IDs
 */
public interface EventDispatcher<I>
        extends MulticastDispatcher<EventClass, EventEnvelope, I>, ExternalDispatcherFactory<I> {

    /**
     * Obtains classes of domestic events processed by this dispatcher.
     */
    default Set<EventClass> eventClasses() {
        return messageClasses();
    }

    /**
     * Obtains classes of external events processed by this dispatcher.
     */
    Set<EventClass> externalEventClasses();

    /**
     * Verifies if this instance dispatches at least one domestic event.
     */
    default boolean dispatchesEvents() {
        return !eventClasses().isEmpty();
    }

    /**
     * Verifies if this instance dispatches at least one external event.
     */
    default boolean dispatchesExternalEvents() {
        return !externalEventClasses().isEmpty();
    }
}
