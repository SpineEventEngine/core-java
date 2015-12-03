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

package org.spine3.internal;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.EventClass;
import org.spine3.base.EventContext;
import org.spine3.eventbus.Subscribe;
import org.spine3.server.MultiHandler;
import org.spine3.util.MethodMap;
import org.spine3.util.Methods;

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
     *
     * <p>An event handler must accept a type derived from {@link Message} as the first parameter,
     * have {@link EventContext} value as the second parameter, and return {@code void}.
     *
     * @param method a method to check
     * @return {@code true} if the method matches event handler conventions, {@code false} otherwise
     */
    @CheckReturnValue
    public static boolean isEventHandler(Method method) {

        final boolean isAnnotated = method.isAnnotationPresent(Subscribe.class);

        final Class<?>[] parameterTypes = method.getParameterTypes();

        //noinspection LocalVariableNamingConvention
        final boolean acceptsMessageAndEventContext =
                parameterTypes.length == 2
                        && Message.class.isAssignableFrom(parameterTypes[0])
                        && EventContext.class.equals(parameterTypes[1]);

        final boolean returnsNothing = Void.TYPE.equals(method.getReturnType());

        return isAnnotated
                && acceptsMessageAndEventContext
                && returnsNothing;
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
            final Map<EventClass, EventHandlerMethod> map = createMap(multiHandler);

            checkModifiers(toMethods(map.values()));

            result.putAll(map);
        }

        return result.build();
    }

    private static Map<EventClass, EventHandlerMethod> createMap(MultiHandler obj) {
        final Multimap<Method, Class<? extends Message>> methodsToClasses = obj.getEventHandlers();

        // Add entries exposed by the object as MultiHandler.
        final ImmutableMap.Builder<EventClass, EventHandlerMethod> builder = ImmutableMap.builder();
        for (Method method : methodsToClasses.keySet()) {
            final Collection<Class<? extends Message>> classes = methodsToClasses.get(method);
            builder.putAll(createMap(obj, method, classes));
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
     *
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
