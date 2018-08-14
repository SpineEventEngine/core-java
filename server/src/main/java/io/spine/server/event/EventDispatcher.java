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

package io.spine.server.event;

import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.bus.MulticastDispatcher;
import io.spine.server.integration.ExternalMessageDispatcher;

import java.util.Set;

/**
 * {@code EventDispatcher} delivers events to {@linkplain EventReceiver receiving} objects.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 */
public interface EventDispatcher<I> extends MulticastDispatcher<EventClass, EventEnvelope, I> {

    default Set<EventClass> getEventClasses() {
        return getMessageClasses();
    }

    Set<EventClass> getExternalEventClasses();

    ExternalMessageDispatcher<I> createExternalDispatcher();

    enum Error {

        DISPATCHING_EXTERNAL_EVENT("Error dispatching external event (class: %s, id: %s)");

        private final String messageFormat;

        Error(String messageFormat) {
            this.messageFormat = messageFormat;
        }

        public String format(Object... args) {
            return String.format(messageFormat, args);
        }
    }
}
