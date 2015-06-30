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
package org.spine3.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.protobuf.Message;
import org.spine3.AggregateRoot;
import org.spine3.CommandHandler;
import org.spine3.Repository;
import org.spine3.base.CommandContext;
import org.spine3.base.EventContext;
import org.spine3.engine.MessageSubscriber;
import org.spine3.lang.AccessLevelException;
import org.spine3.lang.ClassHoldsSubscribersOfSameTypeException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with methods.
 *
 * @author Alexander Yevsyukov
 * @author Mikhail Melnik
 */
@SuppressWarnings("UtilityClass")
public class Methods {

    /**
     * Returns a full method name without parameters.
     *
     * @param obj    an object the method belongs to
     * @param method a method to get name for
     * @return full method name
     */
    @SuppressWarnings("TypeMayBeWeakened") // We keep the type to make the API specific.
    public static String getFullMethodName(Object obj, Method method) {
        return obj.getClass().getName() + '.' + method.getName() + "()";
    }

    /**
     * Checks if a method is an event applier.
     *
     * @param method to check
     * @return {@code true} if the method is an event applier, {@code false} otherwise
     */
    public static boolean isEventApplier(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        boolean isAnnotated = method.isAnnotationPresent(Subscribe.class);
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
     * Checks if a method is a command handler.
     *
     * @param method a method to check
     * @return {@code true} if the method is a command handler, {@code false} otherwise
     */
    public static boolean isCommandHandler(Method method) {
        boolean isAnnotated = method.isAnnotationPresent(Subscribe.class);

        Class<?>[] parameterTypes = method.getParameterTypes();

        //noinspection LocalVariableNamingConvention
        boolean acceptsMessageAndCommandContext =
                parameterTypes.length == 2
                        && Message.class.isAssignableFrom(parameterTypes[0])
                        && CommandContext.class.equals(parameterTypes[1]);

        boolean returnsMessageList = List.class.equals(method.getReturnType());
        boolean returnsMessage = Message.class.equals(method.getReturnType());

        //noinspection OverlyComplexBooleanExpression
        return isAnnotated
                && acceptsMessageAndCommandContext
                && (returnsMessageList || returnsMessage);
    }

    private static final Predicate<Method> isEventApplierPredicate = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isEventApplier(method);
        }
    };

    private static final Predicate<Method> isCommandHandlerPredicate = new Predicate<Method>() {
        @Override
        public boolean apply(@Nullable Method method) {
            checkNotNull(method);
            return isCommandHandler(method);
        }
    };

    /**
     * Returns set of the event types handled by a given aggregate root.
     *
     * @param aggregateRootClass {@link Class} of the aggregate root
     * @return event classes handled by the aggregate root
     */
    @CheckReturnValue
    public static Set<Class<? extends Message>> getEventClasses(Class<? extends AggregateRoot> aggregateRootClass) {
        Set<Class<? extends Message>> result = getHandledMessageClasses(aggregateRootClass, isEventApplierPredicate);
        return result;
    }

    /**
     * Returns set of the command types handled by a given aggregate root.
     *
     * @param clazz {@link Class} of the aggregate root
     * @return command types handled by aggregate root
     */
    @CheckReturnValue
    public static Set<Class<? extends Message>> getCommandClasses(Class<? extends AggregateRoot> clazz) {
        Set<Class<? extends Message>> result = getHandledMessageClasses(clazz, isCommandHandlerPredicate);
        return result;
    }

    /**
     * Returns event/command types handled by given AggregateRoot class.
     */
    @CheckReturnValue
    private static Set<Class<? extends Message>> getHandledMessageClasses(
            Class<? extends AggregateRoot> clazz, Predicate<Method> methodPredicate) {

        Set<Class<? extends Message>> result = Sets.newHashSet();

        for (Method method : clazz.getDeclaredMethods()) {

            boolean methodMatches = methodPredicate.apply(method);

            if (methodMatches) {
                Class<? extends Message> firstParamType = getFirstParamType(method);
                result.add(firstParamType);
            }
        }
        return result;
    }

    /**
     * Returns the first param type of the passed method object.
     *
     * @param method the method object to take first parameter type from
     * @return the {@link Class} of the first method parameter
     */
    public static Class<? extends Message> getFirstParamType(Method method) {

        @SuppressWarnings("unchecked") /** we always expect first param as {@link Message} */
                Class<? extends Message> result = (Class<? extends Message>) method.getParameterTypes()[0];
        return result;
    }

    /**
     * Returns {@link Class} object representing the aggregate id type of the given repository.
     *
     * @return the aggregate id {@link Class}
     */
    public static <I extends Message> Class<I> getRepositoryAggregateIdClass(Repository repository) {
        return getGenericParameterType(repository, 0);
    }

    /**
     * Returns {@link Class} object representing the aggregate root type of the given repository.
     *
     * @return the aggregate root {@link Class}
     */
    public static <R extends AggregateRoot> Class<R> getRepositoryAggregateRootClass(Repository repository) {
        return getGenericParameterType(repository, 1);
    }

    private static <T> Class<T> getGenericParameterType(Object object, int paramNumber) {
        try {
            Type genericSuperclass = object.getClass().getGenericSuperclass();
            Field actualTypeArguments = genericSuperclass.getClass().getDeclaredField("actualTypeArguments");

            actualTypeArguments.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<T> result = (Class<T>) ((Type[]) actualTypeArguments.get(genericSuperclass))[paramNumber];
            actualTypeArguments.setAccessible(false);

            return result;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            //noinspection ProhibitedExceptionThrown // these exceptions cannot occur, otherwise it is a fatal error
            throw new Error(e);
        }
    }

    /**
     * Returns a map of the {@link MessageSubscriber} objects to the corresponding command class.
     *
     * @param commandHandler the object that keeps command subscriber methods
     * @return the map of command subscribers
     */
    public static Map<Class<? extends Message>, MessageSubscriber> scanForCommandSubscribers(Object commandHandler) {
        Map<Class<? extends Message>, MessageSubscriber> result = scanForSubscribers(commandHandler, isCommandHandlerPredicate);
        return result;
    }

    /**
     * Returns a map of the {@link MessageSubscriber} objects to the corresponding event class.
     *
     * @param eventApplier the object that keeps event subscriber methods
     * @return the map of event subscribers
     */
    public static Map<Class<? extends Message>, MessageSubscriber> scanForEventSubscribers(Object eventApplier) {
        Map<Class<? extends Message>, MessageSubscriber> result = scanForSubscribers(eventApplier, isEventApplierPredicate);
        return result;
    }

    /**
     * Returns a map of the {@link MessageSubscriber} objects to the corresponding message class.
     *
     * @param subscribersHolder   the object that keeps subscribed methods
     * @param subscriberPredicate the predicate that defines rules for subscriber scanning
     * @return the map of message subscribers
     */
    private static Map<Class<? extends Message>, MessageSubscriber> scanForSubscribers(
            Object subscribersHolder, Predicate<Method> subscriberPredicate) {

        Map<Class<? extends Message>, MessageSubscriber> result = Maps.newHashMap();

        for (Method method : subscribersHolder.getClass().getDeclaredMethods()) {
            if (subscriberPredicate.apply(method)) {
                /*
                   This check must be performed after
                   subscriberPredicate.apply(method) is true,
                   otherwise it will be performed for the all methods from the subscribersHolder.
                 */
                checkModifier(subscribersHolder, method);

                MessageSubscriber subscriber = new MessageSubscriber(subscribersHolder, method);

                //noinspection unchecked as we always expect first param as Message
                Class<? extends Message> messageClass = (Class<? extends Message>) method.getParameterTypes()[0];
                if (result.containsKey(messageClass)) {
                    throw new ClassHoldsSubscribersOfSameTypeException(subscribersHolder, messageClass);
                }
                result.put(messageClass, subscriber);
            }
        }
        return result;
    }

    private static void checkModifier(Object handler, Method handlerMethod) {
        boolean methodIsPrivate = Modifier.isPrivate(handlerMethod.getModifiers());
        boolean isAggregateRoot = handler instanceof AggregateRoot;

        if (isAggregateRoot && !methodIsPrivate) {
            throw AccessLevelException.forAggregateCommandHandler((AggregateRoot) handler, handlerMethod);
        }

        boolean isRepository = handler instanceof Repository;
        boolean isCommandHandler = handler instanceof CommandHandler;
        boolean methodIsPublic = Modifier.isPublic(handlerMethod.getModifiers());

        if (isRepository && !methodIsPublic) {
            throw AccessLevelException.forRepositoryCommandHandler((Repository) handler, handlerMethod);
        }

        if (isCommandHandler && !methodIsPublic) {
            throw AccessLevelException.forCommandHandler(handler, handlerMethod);
        }
    }

    private Methods() {
    }

}
