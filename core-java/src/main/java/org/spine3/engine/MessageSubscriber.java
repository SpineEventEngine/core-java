/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.engine;

import com.google.protobuf.Message;
import org.spine3.AggregateRoot;
import org.spine3.CommandHandler;
import org.spine3.Repository;
import org.spine3.base.CommandContext;
import org.spine3.util.Messages;
import org.spine3.util.Methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps a subscriber method on a specific object.
 * <p>
 * <p>This class only verifies the suitability of the method and event type if
 * something fails.  Callers are expected to verify their uses of this class.
 * <p>
 * <p>Two EventSubscribers are equivalent when they refer to the same method on the
 * same object (not class).   This property is used to ensure that no subscriber
 * method is registered more than once.
 *
 * @author Mikhail Melnik
 */
public class MessageSubscriber {

    /**
     * This code bases on Guava {@link com.google.common.eventbus.EventSubscriber}
     */

    private static final String ARGUMENT_REJECTED = "Method rejected target/argument: ";

    /**
     * Object sporting the subscriber method.
     */
    private final Object target;
    /**
     * Subscriber method.
     */
    private final Method method;

    public static MessageSubscriber fromCommandHandler(CommandHandler handler, Method method) throws NoSuchMethodException {
        checkNotNull(handler);
        checkNotNull(method);
        return new MessageSubscriber(handler, method);
    }

    public static MessageSubscriber fromRepository(Repository repository) {
        checkNotNull(repository);

        try {
            Method method = repository.getClass().getMethod("dispatch", Message.class, CommandContext.class);
            return new MessageSubscriber(repository, method);
        } catch (NoSuchMethodException e) {
            //noinspection ProhibitedExceptionThrown // this exception cannot occur, otherwise it is a fatal error
            throw new Error(e);
        }
    }

    public static MessageSubscriber fromAggregateRoot(AggregateRoot aggregateRoot, Method method) {
        checkNotNull(aggregateRoot);
        checkNotNull(method);
        return new MessageSubscriber(aggregateRoot, method);
    }

    /**
     * Creates a new MessageSubscriber to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    public MessageSubscriber(Object target, Method method) {
        checkNotNull(target, "target cannot be null.");
        checkNotNull(method, "method cannot be null.");

        this.target = target;
        this.method = method;
        method.setAccessible(true);
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code protoMessage}.
     *
     * @param <T>     the type of the expected handler invocation result
     * @param message protoMessage to handle
     * @param context context of the protoMessage
     * @return the result of message handling
     * @throws java.lang.reflect.InvocationTargetException if the wrapped method
     *                                                     throws any {@link Throwable} that is not an {@link Error} ({@code Error}
     *                                                     instances are propagated as-is).
     */
    @SuppressWarnings("ProhibitedExceptionThrown")
    public <T> T handle(Message message, Message context) throws InvocationTargetException {

        checkNotNull(message);
        checkNotNull(context);
        try {
            @SuppressWarnings("unchecked")
            final T result = (T) method.invoke(target, message, context);
            return result;
        } catch (IllegalArgumentException e) {
            throw new Error(ARGUMENT_REJECTED + message, e);
        } catch (IllegalAccessException e) {
            throw new Error(Messages.INACCESSIBLE_METHOD + message, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code protoMessage}.
     *
     * @param <T>     the type of the expected handler invocation result
     * @param message protoMessage to handle
     * @return the result of message handling
     * @throws java.lang.reflect.InvocationTargetException if the wrapped method
     *                                                     throws any {@link Throwable} that is not an {@link Error} ({@code Error}
     *                                                     instances are propagated as-is).
     */
    @SuppressWarnings("ProhibitedExceptionThrown")
    public <T> T handle(Message message) throws InvocationTargetException {
        checkNotNull(message);
        try {
            @SuppressWarnings("unchecked")
            T result = (T) method.invoke(target, message);
            return result;
        } catch (IllegalArgumentException e) {
            throw new Error(ARGUMENT_REJECTED + message, e);
        } catch (IllegalAccessException e) {
            throw new Error(Messages.INACCESSIBLE_METHOD + message, e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Returns a full name of the subscriber.
     * <p>
     * The full name consists of a fully qualified class name of the target object and
     * the method name separated with a dot character.
     *
     * @return full name of the subscriber
     */
    public String getFullName() {
        return Methods.getFullMethodName(target, method);
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, method);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        //noinspection ConstantConditions
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MessageSubscriber other = (MessageSubscriber) obj;
        return Objects.equals(this.target, other.target)
                && Objects.equals(this.method, other.method);
    }

}

