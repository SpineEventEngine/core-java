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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import org.spine3.EventClass;
import org.spine3.base.EventContext;
import org.spine3.error.AccessLevelException;
import org.spine3.eventbus.Subscribe;
import org.spine3.util.MethodMap;
import org.spine3.util.Methods;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alexander Yevsyukov
 */
public class EventHandlerMethod extends MessageHandlerMethod<Object, EventContext> {

    public static final Predicate<Method> isEventHandlerPredicate = new Predicate<Method>() {
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
     * <p>
     * An event handler must accept a type derived from {@link Message} as the first parameter,
     * have {@link EventContext} value as the second parameter, and return {@code void}.
     *
     * @param method a method to check
     * @return {@code true} if the method matches event handler conventions, {@code false} otherwise
     */
    public static boolean isEventHandler(Method method) {

        boolean isAnnotated = method.isAnnotationPresent(Subscribe.class);

        Class<?>[] parameterTypes = method.getParameterTypes();

        //noinspection LocalVariableNamingConvention
        boolean acceptsMessageAndEventContext =
                parameterTypes.length == 2
                        && Message.class.isAssignableFrom(parameterTypes[0])
                        && EventContext.class.equals(parameterTypes[1]);

        boolean returnsNothing = Void.TYPE.equals(method.getReturnType());

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
    public static Map<EventClass, EventHandlerMethod> scan(Object target) {
        MethodMap handlers = new MethodMap(target.getClass(), isEventHandlerPredicate);

        final ImmutableMap.Builder<EventClass, EventHandlerMethod> builder = ImmutableMap.builder();
        for (ImmutableMap.Entry<Class<? extends Message>, Method> entry : handlers.entrySet()) {
            final EventHandlerMethod handler = new EventHandlerMethod(target, entry.getValue());
            handler.checkModifier();
            builder.put(EventClass.of(entry.getKey()), handler);
        }
        return builder.build();
    }

    @Override
    protected void checkModifier() {
        if (!isPublic()) {
            Object target = getTarget();
            Method method = getMethod();

            throw new AccessLevelException(String.format(
                    "Event handler %s must be declared 'public'", Methods.getFullMethodName(target, method)));
        }
    }

    @Override
    public <R> R invoke(Message message, EventContext context) throws InvocationTargetException {
        return super.invoke(message, context);
    }

}
