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

import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SystemBus
        implements DispatcherRegistry<EventClass, EventEnvelope, EventDispatcher<?>> {

    private final EventBus systemEventBus;

    private SystemBus(EventBus systemEventBus) {
        this.systemEventBus = checkNotNull(systemEventBus);
    }

    public static SystemBus newInstance(SystemContext context) {
        checkNotNull(context);
        EventBus delegate = context.getEventBus();
        return new SystemBus(delegate);
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
