/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.internal.EventHandlerMethod;
import org.spine3.server.Entity;
import org.spine3.util.MethodMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates data projection from incoming events.
 *
 * @param <I> the type of the IDs of the stateful handlers
 * @param <M> the type of the state objects
 */
public abstract class Projection<I, M extends Message> extends Entity<I, M> {

    private MethodMap handlers;

    protected Projection(I id) {
        super(id);
    }

    protected void handle(Message event, EventContext ctx) throws InvocationTargetException {
        init();
        dispatch(event, ctx);
    }

    private void dispatch(Message event, EventContext ctx) throws InvocationTargetException {
        final Class<? extends Message> eventClass = event.getClass();
        Method method = handlers.get(eventClass);
        if (method == null) {
            throw new IllegalStateException(String.format("Missing event handler for event class %s in the projection class %s",
                    eventClass, this.getClass()));
        }
        EventHandlerMethod handler = new EventHandlerMethod(this, method);
        handler.invoke(event, ctx);
    }

    protected boolean isInitialized() {
        return handlers != null;
    }

    protected void init() {
        if (!isInitialized()) {
            final Registry registry = Registry.instance();
            final Class<? extends Projection> thisClass = getClass();

            if (!registry.contains(thisClass)) {
                registry.register(thisClass);
            }

            handlers = registry.getEventHandlers(thisClass);
        }
    }

    private static class Registry {
        private final MethodMap.Registry<Projection> eventHandlers = new MethodMap.Registry<>();

        boolean contains(Class<? extends Projection> clazz) {
            return eventHandlers.contains(clazz);
        }

        void register(Class<? extends Projection> clazz) {
            eventHandlers.register(clazz, EventHandlerMethod.isEventHandlerPredicate);
        }

        MethodMap getEventHandlers(Class<? extends Projection> clazz) {
            MethodMap result = eventHandlers.get(clazz);
            return result;
        }

        static Registry instance() {
            return RegistrySingleton.INSTANCE.value;
        }

        @SuppressWarnings("InnerClassTooDeeplyNested")
        private enum RegistrySingleton {
            INSTANCE;

            @SuppressWarnings("NonSerializableFieldInSerializableClass")
            private final Registry value = new Registry();
        }
    }
}