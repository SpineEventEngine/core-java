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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.base.ThrowableMessage;
import io.spine.core.Event;
import io.spine.server.event.EventBus;
import io.spine.server.event.RejectionEnvelope;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.SignalEnvelope;

import java.util.Collection;

/**
 * Operations common for repositories that can post to {@link #eventBus() EventBus}.
 */
@Internal
public interface EventProducingRepository {

    /**
     * Obtains the {@code EventBus} to which the repository posts.
     */
    EventBus eventBus();

    /**
     * Filters events before they are posted to the bus.
     */
    Iterable<Event> filter(Collection<Event> events);

    /**
     * Filters the passed events and posts the result to the EventBus.
     */
    default void postEvents(Collection<Event> events) {
        Iterable<Event> filtered = filter(events);
        eventBus().post(filtered);
    }

    /**
     * If the passed signal is command and the error occurred is a rejection, posts
     * it to the associated {@link #eventBus() EventBus}.
     */
    default void postRejectionIfCommand(SignalEnvelope<?, ?, ?> signal, Throwable cause) {
        if (signal instanceof CommandEnvelope && cause instanceof ThrowableMessage) {
            CommandEnvelope command = (CommandEnvelope) signal;
            ThrowableMessage rejection = (ThrowableMessage) cause;
            RejectionEnvelope re = RejectionEnvelope.from(command, rejection);
            postEvents(ImmutableSet.of(re.outerObject()));
        }
    }
}
