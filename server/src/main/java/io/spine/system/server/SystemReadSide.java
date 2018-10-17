/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.system.server;

import io.spine.annotation.SPI;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A message bus for system events.
 *
 * <p>A domain bounded context may register dispatchers of system events in the {@code SystemReadSide}.
 * All the events of a system context are broadcast by this bus.
 *
 * <p>Only the system events are allowed in the {@code SystemReadSide}. This class does not extend
 * the {@link io.spine.server.bus.Bus Bus} base class in order to restrict users from posting events
 * into the system bus.
 *
 * @implNote
 * A system bus is a delegate for the system event bus. When registering a dispatcher in
 * the system bus, the dispatcher gets registered in the system event bus. This way, all
 * the messages posted into the system event bus can be accessed via the system bus.
 */
@SPI
public final class SystemReadSide
        implements DispatcherRegistry<EventClass, EventEnvelope, EventDispatcher<?>> {

    private final EventBus systemEventBus;

    private SystemReadSide(EventBus systemEventBus) {
        this.systemEventBus = systemEventBus;
    }

    /**
     * Creates a new instance of {@code SystemReadSide} for the given system context.
     *
     * @param context
     *         the system context to broadcast the events of
     * @return a new instance of {@code SystemReadSide}
     */
    public static SystemReadSide newInstance(SystemContext context) {
        checkNotNull(context);
        EventBus delegate = context.getEventBus();
        return new SystemReadSide(delegate);
    }

    @Override
    public void register(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        systemEventBus.register(dispatcher);
    }

    @Override
    public void unregister(EventDispatcher<?> dispatcher) {
        checkNotNull(dispatcher);
        systemEventBus.unregister(dispatcher);
    }
}
