/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.reflect;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.core.Subscribe;

import javax.annotation.CheckReturnValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Rejections.isRejection;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.Collections.unmodifiableSet;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 * @see Subscribe
 */
public final class EventSubscriberMethod extends HandlerMethod<EventContext> {

    /** The instance of the predicate to filter non-external event subscriber methods of a class. */
    private static final MethodPredicate DOMESTIC_SUBSCRIBERS = new FilterPredicate();

    /** The instance of the predicate to filter external event subscriber methods of a class. */
    private static final MethodPredicate EXTERNAL_SUBSCRIBERS = new FilterPredicate(true);


    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    private EventSubscriberMethod(Method method) {
        super(method);
    }

    /**
     * Invokes the subscriber method in the passed object.
     */
    public static void invokeFor(Object target, Message eventMessage, EventContext context) {
        checkNotNull(target);
        checkNotNull(eventMessage);
        checkNotNull(context);

        final EventSubscriberMethod method = getMethod(target.getClass(), eventMessage, context);
        method.invoke(target, eventMessage, context);
    }

    /**
     * Obtains the method for handling the event in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an event handling method
     *                               for the class of the passed message
     */
    //TODO:2017-07-21:alex.tymchenko: eliminate code duplication
    public static EventSubscriberMethod getMethod(Class<?> cls, Message eventMessage) {
        checkNotNull(cls);
        checkNotNull(eventMessage);

        final Class<? extends Message> eventClass = eventMessage.getClass();
        final EventSubscriberMethod method = MethodRegistry.getInstance()
                                                           .get(cls, eventClass, factory());
        if (method == null) {
            throw newIllegalStateException("The class %s is not subscribed to events of %s.",
                                           cls.getName(), eventClass.getName());
        }
        return method;
    }

    /**
     * Obtains the method for handling the event in the passed class.
     *
     * @throws IllegalStateException if the passed class does not have an event handling method
     *                               for the class of the passed message
     */
    public static EventSubscriberMethod getMethod(Class<?> cls,
                                                  Message eventMessage,
                                                  EventContext eventContext) {
        checkNotNull(cls);
        checkNotNull(eventMessage);
        checkNotNull(eventContext);

        final Class<? extends Message> eventClass = eventMessage.getClass();
        final MethodRegistry registry = MethodRegistry.getInstance();

        final Set<MethodAttribute<?>> attributes = attributesFor(eventContext);
        final EventSubscriberMethod method = registry.get(cls,
                                                          eventClass, attributes,
                                                          factory());
        if (method == null) {
            throw newIllegalStateException("The class %s is not subscribed to events of %s.",
                                           cls.getName(), eventClass.getName());
        }
        return method;
    }

    private static Set<MethodAttribute<?>> attributesFor(EventContext eventContext) {
        final ImmutableSet.Builder<MethodAttribute<?>> builder = ImmutableSet.builder();

        if(Events.isExternal(eventContext)) {
            builder.add(new ExternalAttribute(true));
        }
        return builder.build();
    }

    static EventSubscriberMethod from(Method method) {
        return new EventSubscriberMethod(method);
    }

    @CheckReturnValue
    public static Set<EventClass> inspect(Class<?> cls) {
        checkNotNull(cls);
        final Set<EventClass> result =
                EventClass.setOf(inspect(cls, domesticSubscribers()));
        return result;
    }

    @CheckReturnValue
    public static Set<EventClass> inspectExternal(Class<?> cls) {
        checkNotNull(cls);
        checkNotNull(cls);
        final Set<EventClass> result =
                EventClass.setOf(inspect(cls, externalSubscribers()));
        return result;
    }

    @Override
    protected Set<MethodAttribute<?>> getAttributes() {
        final Set<MethodAttribute<?>> attributes = Sets.newHashSet();

        final Subscribe annotation = getMethod().getAnnotation(Subscribe.class);
        if (annotation != null && annotation.external()) {
            attributes.add(new ExternalAttribute(true));
        }

        return unmodifiableSet(attributes);
    }

    /** Returns the factory for filtering and creating event subscriber methods. */
    private static HandlerMethod.Factory<EventSubscriberMethod> factory() {
        return Factory.getInstance();
    }

    static MethodPredicate domesticSubscribers() {
        return DOMESTIC_SUBSCRIBERS;
    }

    //TODO:2017-07-21:alex.tymchenko: cover external subscribers with tests as well.
    static MethodPredicate externalSubscribers() {
        return EXTERNAL_SUBSCRIBERS;
    }

    /**
     * The factory for creating {@linkplain EventSubscriberMethod event subscriber} methods.
     */
    private static class Factory implements HandlerMethod.Factory<EventSubscriberMethod> {

        private static final Factory INSTANCE = new Factory();

        private static Factory getInstance() {
            return INSTANCE;
        }

        @Override
        public Class<EventSubscriberMethod> getMethodClass() {
            return EventSubscriberMethod.class;
        }

        @Override
        public EventSubscriberMethod create(Method method) {
            return from(method);
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Predicates.or(domesticSubscribers(), externalSubscribers());
        }

        @Override
        public void checkAccessModifier(Method method) {
            if (!Modifier.isPublic(method.getModifiers())) {
                warnOnWrongModifier("Event subscriber {} must be declared 'public'", method);
            }
        }
    }

    /**
     * The predicate class allowing to filter event subscriber methods.
     *
     * <p>Please see {@link Subscribe} annotation for more information.
     */
    private static class FilterPredicate extends HandlerMethodPredicate<EventContext> {

        private final boolean externalOnly;

        private FilterPredicate() {
            this(false);
        }

        private FilterPredicate(boolean externalOnly) {
            super(Subscribe.class, EventContext.class);
            this.externalOnly = externalOnly;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Filters out methods that accept rejection messages as the first parameter.
         */
        @Override
        protected boolean verifyParams(Method method) {
            if (super.verifyParams(method)) {
                @SuppressWarnings("unchecked") // The case is safe since super returned `true`.
                final Class<? extends Message> firstParameter =
                        (Class<? extends Message>) method.getParameterTypes()[0];
                final boolean isRejection = isRejection(firstParameter);
                return !isRejection;
            }
            return false;
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            final boolean isVoid = Void.TYPE.equals(method.getReturnType());
            return isVoid;
        }

        @Override
        protected boolean verifyAnnotation(Method method) {
            return super.verifyAnnotation(method) && matchesExternal(externalOnly, method);
        }

        private boolean matchesExternal(boolean externalOnly, Method method) {
            final Annotation annotation = method.getAnnotation(getAnnotationClass());
            final Subscribe subscribeAnnotation = (Subscribe) annotation;
            final boolean result = externalOnly == subscribeAnnotation.external();
            return result;
        }
    }

    private static class ExternalAttribute implements MethodAttribute<Boolean> {

        private final boolean value;

        private ExternalAttribute(boolean value) {
            this.value = value;
        }

        @Override
        public String getName() {
            return "external";
        }

        @Override
        public Boolean getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExternalAttribute that = (ExternalAttribute) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
