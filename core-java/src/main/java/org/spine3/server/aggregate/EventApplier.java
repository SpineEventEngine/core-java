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

package org.spine3.server.aggregate;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import org.spine3.EventClass;
import org.spine3.base.EventContext;
import org.spine3.error.AccessLevelException;
import org.spine3.internal.MessageHandlerMethod;
import org.spine3.server.aggregate.error.MissingEventApplierException;
import org.spine3.util.Methods;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper for event applier method.
 *
 * @author Alexander Yevsyukov
 */
class EventApplier extends MessageHandlerMethod<AggregateRoot, Void> {

    static final Predicate<Method> isEventApplierPredicate = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventApplier(method);
        }
    };

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    protected EventApplier(AggregateRoot target, Method method) {
        super(target, method);
    }

    /**
     * Checks if a method is an event applier.
     *
     * @param method to check
     * @return {@code true} if the method is an event applier, {@code false} otherwise
     */
    @SuppressWarnings("LocalVariableNamingConvention") // -- we want longer names here for clarity.
    public static boolean isEventApplier(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        boolean isAnnotated = method.isAnnotationPresent(Apply.class);
        boolean acceptsMessage = parameterTypes.length == 1 && Message.class.isAssignableFrom(parameterTypes[0]);
        //noinspection LocalVariableNamingConvention
        boolean acceptsMessageAndEventContext =
                parameterTypes.length == 2
                        && Message.class.isAssignableFrom(parameterTypes[0])
                        && EventContext.class.equals(parameterTypes[1]);
        boolean returnsNothing = Void.TYPE.equals(method.getReturnType());

        //noinspection OverlyComplexBooleanExpression
        return isAnnotated && (acceptsMessage || acceptsMessageAndEventContext) && returnsNothing;
    }

    /**
     * Scans for event applier methods the passed aggregate root object.
     *
     * @param aggregateRoot the object that keeps event applier methods
     * @return immutable map of event appliers
     */
    public static java.util.Map<EventClass, EventApplier> scan(AggregateRoot aggregateRoot) {
        java.util.Map<Class<? extends Message>, Method> appliers = scan(aggregateRoot, isEventApplierPredicate);

        final ImmutableMap.Builder<EventClass, EventApplier> builder = ImmutableMap.builder();
        for (java.util.Map.Entry<Class<? extends Message>, Method> entry : appliers.entrySet()) {
            final EventApplier applier = new EventApplier(aggregateRoot, entry.getValue());
            applier.checkModifier();
            builder.put(EventClass.of(entry.getKey()), applier);
        }
        return builder.build();
    }

    public static AccessLevelException forEventApplier(AggregateRoot aggregate, Method method) {
        return new AccessLevelException(messageForEventApplier(aggregate, method));
    }

    private static String messageForEventApplier(AggregateRoot aggregate, Method method) {
        return "Event applier method of the aggregate " + Methods.getFullMethodName(aggregate, method) +
                " must be declared 'private'. It is not supposed to be called from outside the aggregate.";
    }

    @Override
    protected void checkModifier() {
        boolean methodIsPrivate = Modifier.isPrivate(getMethod().getModifiers());

        if (!methodIsPrivate) {
            throw forEventApplier(getTarget(), getMethod());
        }
    }

    /**
     * Dispatches the incoming events to the corresponding applier method of an aggregate root.
     *
     * @author Alexander Yevsyukov
     * @author Mikhail Melnik
     */
    @SuppressWarnings("ClassNamingConvention")
    static class Map {

        private final java.util.Map<EventClass, EventApplier> subscribersByType;

        /**
         * Constructs a new instance for the passed aggregated root.
         *
         * @param aggregateRoot the aggregate root object
         */
        Map(AggregateRoot aggregateRoot) {
            checkNotNull(aggregateRoot);

            java.util.Map<EventClass, EventApplier> appliers = scan(aggregateRoot);
            this.subscribersByType = ImmutableMap.<EventClass, EventApplier>builder().putAll(appliers).build();
        }

        /**
         * Directs the event to the corresponding applier.
         *
         * @param event the event to be applied
         * @throws InvocationTargetException if an exception occurs during event applying
         */
        void apply(Message event) throws InvocationTargetException {
            checkNotNull(event);

            EventClass eventClass = EventClass.of(event);
            if (!subscriberRegistered(eventClass)) {
                throw new MissingEventApplierException(event);
            }

            EventApplier applier = findApplier(eventClass);
            applier.invoke(event);
        }

        private EventApplier findApplier(EventClass eventClass) {
            return subscribersByType.get(eventClass);
        }

        private boolean subscriberRegistered(EventClass eventClass) {
            return subscribersByType.containsKey(eventClass);
        }

    }
}
