/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.spine3.server.internal.EventHandlerMethod;
import org.spine3.type.EventClass;

import java.util.Collection;
import java.util.Map;

/**
 * The registry of event handling methods by event class.
 *
 * <p>There can be multiple handlers per event class.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class HandlerRegistry {

    private final Multimap<EventClass, EventHandlerMethod> handlersByClass = HashMultimap.create();

    /* package */ void subscribe(Map<EventClass, EventHandlerMethod> handlers) {
        for (Map.Entry<EventClass, EventHandlerMethod> entry : handlers.entrySet()) {
            handlersByClass.put(entry.getKey(), entry.getValue());
        }
    }

    /* package */ void unsubscribe(Map<EventClass, EventHandlerMethod> handlers) {
        for (Map.Entry<EventClass, EventHandlerMethod> entry : handlers.entrySet()) {

            final EventClass eventClass = entry.getKey();
            final EventHandlerMethod handler = entry.getValue();

            unsubscribe(eventClass, handler);
        }
    }

    private void unsubscribe(EventClass c, EventHandlerMethod handler) {
        final Collection<EventHandlerMethod> currentSubscribers = handlersByClass.get(c);
        if (!currentSubscribers.contains(handler)) {
            throw handlerMethodWasNotRegistered(handler);
        }
        currentSubscribers.remove(handler);
    }

    /* package */ void unsubscribeAll() {
        handlersByClass.clear();
        EventBus.log().info("All subscribers cleared.");
    }

    private static IllegalArgumentException handlerMethodWasNotRegistered(EventHandlerMethod handler) {
        return new IllegalArgumentException(
                "Cannot un-subscribe the event handler, which was not subscribed before:" + handler.getFullName());
    }

    /* package */ Collection<EventHandlerMethod> getSubscribers(EventClass c) {
        return ImmutableList.copyOf(handlersByClass.get(c));
    }

    /* package */ boolean hasSubscribers(EventClass eventClass) {
        final Collection<EventHandlerMethod> handlers = getSubscribers(eventClass);
        return !handlers.isEmpty();
    }
}
