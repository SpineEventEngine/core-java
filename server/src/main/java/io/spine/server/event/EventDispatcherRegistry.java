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

import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The registry of objects that dispatch event to handlers.
 *
 * <p>There can be multiple dispatchers per event class.
 */
class EventDispatcherRegistry
        extends DispatcherRegistry<EventClass, EventEnvelope, EventDispatcher<?>> {

    @Override
    public void register(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        Set<EventClass> eventClasses = dispatcher.getMessageClasses();
        checkNotEmpty(dispatcher, eventClasses);

        super.register(dispatcher);
    }

    @Override
    public void unregister(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        Set<EventClass> eventClasses = dispatcher.getMessageClasses();
        checkNotEmpty(dispatcher, eventClasses);

        super.unregister(dispatcher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides in order to expose itself to
     * {@linkplain EventBus#getDispatchers(EventClass)}) EventBus}.
     */
    @Override
    protected Set<EventDispatcher<?>> getDispatchersForType(EventClass messageClass) {
        return super.getDispatchersForType(messageClass);
    }

    /**
     * Ensures that the dispatcher forwards at least one event.
     *
     * @throws IllegalArgumentException if the dispatcher returns empty set of event classes
     * @throws NullPointerException     if the dispatcher returns null set
     */
    private void checkNotEmpty(EventDispatcher<?> dispatcher, Set<EventClass> messageClasses) {
        checkArgument(!messageClasses.isEmpty(),
                      "%s: No message types are forwarded by this dispatcher: %s",
                      getClass().getName(),  dispatcher);
    }
}
