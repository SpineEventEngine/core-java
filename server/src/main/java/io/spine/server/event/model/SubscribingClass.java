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

package io.spine.server.event.model;

import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * An interface common for model classes that subscribe to events.
 *
 * @author Alexander Yevsyukov
 */
public interface SubscribingClass {

    /**
     * Obtains a method that handles the passed class of events.
     *
     * @param event
     *         the event to obtain a method for
     */
    default SubscriberMethod getSubscriber(EventEnvelope event) {
        Collection<SubscriberMethod> subscribers =
                getSubscribers(event.getMessageClass(), event.getOriginClass());
        SubscriberMethod matchingMethod = subscribers
                .stream()
                .filter(s -> s.canHandle(event))
                .findFirst()
                .orElseThrow(() -> newIllegalStateException(
                        "None of the subscriber methods could handle %s event." +
                                "%n  Methods: %s" +
                                "%n  Event message: %s.",
                        event.getMessageClass(), subscribers, event.getMessage()
                ));
        return matchingMethod;
    }

    Collection<SubscriberMethod> getSubscribers(EventClass eventClass, MessageClass originClass);
}
