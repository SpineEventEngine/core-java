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

package io.spine.server.event.model;

import io.spine.logging.Logging;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import static java.util.Comparator.comparing;

/**
 * An interface common for model classes that subscribe to events.
 */
public interface SubscribingClass extends Logging {

    /**
     * Obtains a method that handles the passed class of events.
     *
     * @param event
     *         the event to obtain a method for
     */
    default Optional<SubscriberMethod> getSubscriber(EventEnvelope event) {
        Collection<SubscriberMethod> subscribers =
                getSubscribers(event.getMessageClass(), event.getOriginClass());
        Comparator<SubscriberMethod> methodOrder = comparing(
                (SubscriberMethod subscriber) -> subscriber.filter().getField().getFieldNameCount()
        ).reversed();
        Optional<SubscriberMethod> foundSubscriber = subscribers
                .stream()
                .sorted(methodOrder)
                .filter(s -> s.canHandle(event))
                .findFirst();
        if (foundSubscriber.isPresent()) {
            return foundSubscriber;
        } else {
            _debug("None of the subscriber methods could handle %s event." +
                           "%n  Methods: %s" +
                           "%n  Event message: %s.",
                   event.getMessageClass(), subscribers, event.getMessage());
            return Optional.empty();
        }
    }

    Collection<SubscriberMethod> getSubscribers(EventClass eventClass, MessageClass originClass);
}
