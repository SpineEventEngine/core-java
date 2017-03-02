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

import org.spine3.base.EventClass;
import org.spine3.server.bus.DispatcherRegistry;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The registry of objects that dispatch event to handlers.
 *
 * <p>There can be multiple dispatchers per event class.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
class EventDispatcherRegistry extends DispatcherRegistry<EventClass, EventDispatcher> {

    @Override
    protected void register(EventDispatcher dispatcher) {
        checkNotNull(dispatcher);
        final Set<EventClass> eventClasses = dispatcher.getMessageClasses();
        checkNotEmpty(dispatcher, eventClasses);

        super.register(dispatcher);
    }

    @Override
    protected void unregister(EventDispatcher dispatcher) {
        checkNotNull(dispatcher);
        final Set<EventClass> eventClasses = dispatcher.getMessageClasses();
        checkNotEmpty(dispatcher, eventClasses);

        super.unregister(dispatcher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to
     * {@link EventBus#close() EventBus}.
     */
    @Override
    protected Set<EventDispatcher> getDispatchers(EventClass messageClass) {
        return super.getDispatchers(messageClass);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to
     * {@link EventBus#isUnsupportedEvent(EventClass)} EventBus}.
     */
    @Override
    protected void unregisterAll() {
        super.unregisterAll();
    }

    boolean hasDispatchersFor(EventClass eventClass) {
        final Set<EventDispatcher> dispatchers = getDispatchers(eventClass);
        final boolean result = !dispatchers.isEmpty();
        return result;
    }

    /**
     * Ensures that the dispatcher forwards at least one event.
     *
     * @throws IllegalArgumentException if the dispatcher returns empty set of event classes
     * @throws NullPointerException     if the dispatcher returns null set
     */
    private static void checkNotEmpty(EventDispatcher dispatcher, Set<EventClass> eventClasses) {
        checkArgument(!eventClasses.isEmpty(),
                      "No event classes are forwarded by this dispatcher: %s",
                      dispatcher);
    }
}
