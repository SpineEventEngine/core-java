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
import com.google.protobuf.Message;
import org.spine3.server.reflect.EventHandlerMethod;
import org.spine3.server.reflect.MethodMap;
import org.spine3.server.type.EventClass;

import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The registry of event handling methods by event class.
 *
 * <p>There can be multiple handlers per event class.
 *
 * @author Alexander Yevsyukov
 */
/* package */ class HandlerRegistry {

    private final Multimap<EventClass, EventHandler> handlersByEventClass = HashMultimap.create();

    /* package */ void subscribe(EventHandler object) {
        checkNotNull(object);
        final MethodMap<EventHandlerMethod> handlers = EventHandlerMethod.scan(object);
        final boolean handlersEmpty = handlers.isEmpty();
        checkHandlersNotEmpty(object, handlersEmpty);
        for (Map.Entry<Class<? extends Message>, EventHandlerMethod> entry : handlers.entrySet()) {
            handlersByEventClass.put(EventClass.of(entry.getKey()), object);
        }
    }

    /* package */ void usubscribe(EventHandler object) {
        final MethodMap<EventHandlerMethod> handlers = EventHandlerMethod.scan(object);
        final boolean handlersEmpty = handlers.isEmpty();
        checkHandlersNotEmpty(object, handlersEmpty);
        if (!handlersEmpty) {
            for (Class<? extends Message> eventClass : handlers.keySet()) {
                handlersByEventClass.remove(EventClass.of(eventClass), object);
            }
        }
    }

    /* package */ void unsubscribeAll() {
        handlersByEventClass.clear();
        EventBus.log().info("All subscribers cleared.");
    }

    /* package */ Collection<EventHandler> getSubscribers(EventClass c) {
        return ImmutableList.copyOf(handlersByEventClass.get(c));
    }

    /* package */ boolean hasSubscribers(EventClass eventClass) {
        final Collection<EventHandler> handlers = getSubscribers(eventClass);
        return !handlers.isEmpty();
    }

    private static void checkHandlersNotEmpty(Object object, boolean handlersEmpty) {
        checkArgument(!handlersEmpty, "No event subscriber methods found in %s", object);
    }
}
