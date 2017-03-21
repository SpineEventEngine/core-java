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


import org.spine3.server.outbus.OutputDispatcherRegistry;
import org.spine3.type.EventClass;

import java.util.Set;

/**
 * The registry of objects that dispatch event to handlers.
 *
 * <p>There can be multiple dispatchers per event class.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
class EventDispatcherRegistry extends OutputDispatcherRegistry<EventClass, EventDispatcher> {

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose this method to
     * {@linkplain EventBus#isUnsupportedEvent(EventClass)}  EventBus}.
     */
    @Override
    protected boolean hasDispatchersFor(EventClass eventClass) {
        return super.hasDispatchersFor(eventClass);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides in order to expose itself to
     * {@linkplain EventBus#getDispatchers(EventClass)}) EventBus}.
     */
    @Override
    protected Set<EventDispatcher> getDispatchers(EventClass eventClass) {
        return super.getDispatchers(eventClass);
    }

}
