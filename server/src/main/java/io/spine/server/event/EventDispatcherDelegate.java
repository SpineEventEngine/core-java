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

package io.spine.server.event;

import io.spine.annotation.Internal;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;

import java.util.Set;

/**
 * A common interface for objects which need to dispatch {@linkplain io.spine.core.Event events},
 * but are unable to implement {@link io.spine.server.event.EventDispatcher EventDispatcher}.
 *
 * <p>This interface defines own contract (instead of extending {@linkplain
 * io.spine.server.bus.MessageDispatcher MessageDispatcher} to allow classes that can dispatch
 * messages other than events (by implementing {@linkplain io.spine.server.bus.MessageDispatcher
 * MessageDispatcher}) and dispatch events by implementing this interface.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public interface EventDispatcherDelegate {

    Set<EventClass> getEventClasses();

    void dispatchEvent(EventEnvelope envelope);
}
