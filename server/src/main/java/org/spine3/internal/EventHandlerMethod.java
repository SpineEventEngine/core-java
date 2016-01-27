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

package org.spine3.internal;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.EventContext;
import org.spine3.server.MultiHandler;
import org.spine3.server.Subscribe;
import org.spine3.server.util.MethodMap;
import org.spine3.server.util.Methods;
import org.spine3.type.EventClass;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alexander Yevsyukov
 */
public class EventHandlerMethod extends MessageHandlerMethod<Object, EventContext> {

    public static final Predicate<Method> IS_EVENT_HANDLER = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventHandler(method);
        }
    };

    private static final int MESSAGE_PARAM_INDEX = 0;
    private static final int EVENT_CONTEXT_PARAM_INDEX = 1;

    private static final int EVENT_HANDLER_PARAM_COUNT = 2;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    public EventHandlerMethod(Object target, Method method) {
        super(target, method);
    }

    /**
     * Checks if the passed method is an event handlers.
     * <p/>
     * <p>An event handler must accept a type derived from {@link Message} as the first parameter,
     * have {@link EventContext} value as the second parameter, and return {@code void}.
     *
     * @param method a method to check
     * @return {@code true} if the method matches event handler conventions, {@code false} otherwise
     */
    @CheckReturnValue
    public static boolean isEventHandler(Method method) {
        if (!isAnnotatedCorrectly(method)) {
            return false;
        }
        if (!acceptsCorrectParams(method)) {
            return false;
        }
        final boolean returnsNothing = Void.TYPE.equals(method.getReturnType());
        return returnsNothing;
    }

    private static boolean acceptsCorrectParams(Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != EVENT_HANDLER_PARAM_COUNT) {
            return false;
        }
        final boolean acceptsCorrectParams =
                Message.class.isAssignableFrom(parameterTypes[MESSAGE_PARAM_INDEX]) &&
                        EventContext.class.equals(parameterTypes[EVENT_CONTEXT_PARAM_INDEX]);
        return acceptsCorrectParams;
    }

    private static boolean isAnnotatedCorrectly(Method method) {
        final boolean result = method.isAnnotationPresent(Subscribe.class);
        return result;
    }

    /**
     * Scans for event handlers the passed target.
     *
     * @param target the target to scan
     * @return immutable map of event handling methods
     */
    @CheckReturnValue
    public static Map<EventClass, EventHandlerMethod> scan(Object target) {
        final ImmutableMap.Builder<EventClass, EventHandlerMethod> result = ImmutableMap.builder();

        // Scan for declared event handler methods.
        final MethodMap handlers = new MethodMap(target.getClass(), IS_EVENT_HANDLER);
        checkModifiers(handlers.values());

        for (ImmutableMap.Entry<Class<? extends Message>, Method> entry : handlers.entrySet()) {
            final EventClass eventClass = EventClass.of(entry.getKey());
            final EventHandlerMethod handler = new EventHandlerMethod(target, entry.getValue());
            result.put(eventClass, handler);
        }
        // If the passed object is MultiHandler add its methods too.
        if (target instanceof MultiHandler) {
            final MultiHandler multiHandler = (MultiHandler) target;
            final Map<EventClass, EventHandlerMethod> multiHandlersMap = createMap(multiHandler);
            checkModifiers(toMethods(multiHandlersMap.values()));
            result.putAll(multiHandlersMap);
        }
        return result.build();
    }

    private static Map<EventClass, EventHandlerMethod> createMap(MultiHandler obj) {
        final Multimap<Method, Class<? extends Message>> methodsToClasses = obj.getHandlers();
        // Add entries exposed by the object as MultiHandler.
        final ImmutableMap.Builder<EventClass, EventHandlerMethod> builder = ImmutableMap.builder();
        for (Method method : methodsToClasses.keySet()) {
            // check if the method accepts an event context (and is not a command handler)
            if (acceptsCorrectParams(method)) {
                final Collection<Class<? extends Message>> classes = methodsToClasses.get(method);
                builder.putAll(createMap(obj, method, classes));
            }
        }
        return builder.build();
    }

    private static Iterable<Method> toMethods(Iterable<EventHandlerMethod> handlerMethods) {
        return Iterables.transform(handlerMethods, new Function<EventHandlerMethod, Method>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public Method apply(@Nullable EventHandlerMethod eventHandlerMethod) {
                if (eventHandlerMethod == null) {
                    return null;
                }
                return eventHandlerMethod.getMethod();
            }
        });
    }

    private static Map<EventClass, EventHandlerMethod> createMap(Object target,
                                                                 Method method,
                                                                 Iterable<Class<? extends Message>> classes) {
        final ImmutableMap.Builder<EventClass, EventHandlerMethod> builder = ImmutableMap.builder();
        for (Class<? extends Message> messageClass : classes) {
            final EventClass key = EventClass.of(messageClass);
            final EventHandlerMethod value = new EventHandlerMethod(target, method);
            builder.put(key, value);
        }
        return builder.build();
    }

    @Override
    public <R> R invoke(Message message, EventContext context) throws InvocationTargetException {
        return super.invoke(message, context);
    }

    /**
     * Verifiers modifiers in the methods in the passed map to be 'public'.
     * <p/>
     * <p>Logs warning for the methods with a non-public modifier.
     *
     * @param methods the map of methods to check
     */
    public static void checkModifiers(Iterable<Method> methods) {
        for (Method method : methods) {
            final boolean isPublic = Modifier.isPublic(method.getModifiers());
            if (!isPublic) {
                log().warn(String.format("Event handler %s must be declared 'public'",
                        Methods.getFullMethodName(method)));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override // Promote to public to make it visible to routines inspecting EventBus.
    public Object getTarget() {
        return super.getTarget();
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EventHandlerMethod.class);
    }

    @CheckReturnValue
    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
