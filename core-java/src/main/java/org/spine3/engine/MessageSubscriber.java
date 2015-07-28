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
package org.spine3.engine;

import com.google.protobuf.Message;
import org.spine3.util.Methods;

import javax.annotation.Nullable;
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
    private static final String MSG_UNABLE_TO_ACCESS = "Unable to call a method of the message: ";

    /**
     * Object sporting the subscriber method.
     */
    private final Object target;
    /**
     * Subscriber method.
     */
    private final Method method;

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
            throw new Error(MSG_UNABLE_TO_ACCESS + message, e);
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
            throw new Error(MSG_UNABLE_TO_ACCESS + message, e);
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
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MessageSubscriber other = (MessageSubscriber) obj;
        return Objects.equals(this.target, other.target)
                && Objects.equals(this.method, other.method);
    }

}

