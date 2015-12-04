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

import com.google.protobuf.Message;
import org.spine3.util.Methods;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

/**
 * Wraps a handler method on a specific object.
 *
 * <p>This class only verifies the suitability of the method and message type if
 * something fails.  Callers are expected to verify their uses of this class.
 *
 * <p>Two message handlers are equivalent when they refer to the same method on the
 * same object (not class).   This property is used to ensure that no handler
 * method is registered more than once.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov

 * @param <T> the type of the target object
 * @param <C> the type of the message context or {@code Void} if context is not used
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public abstract class MessageHandlerMethod<T, C> {

    /**
     * Object sporting the handler method.
     */
    private final T target;

    /**
     * Handler method.
     */
    private final Method method;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param target object to which the method applies
     * @param method subscriber method
     */
    protected MessageHandlerMethod(T target, Method method) {
        checkNotNull(target, "target");
        checkNotNull(method, "method");

        this.target = target;
        this.method = method;
        method.setAccessible(true);
    }

    /**
     * @return the target object on which the method call is made
     */
    protected T getTarget() {
        return target;
    }

    /**
     * @return the handling method
     */
    protected Method getMethod() {
        return method;
    }

    /**
     * @return {@code true} if the method is declared {@code public}, {@code false} otherwise
     */
    protected boolean isPublic() {
        final boolean result = Modifier.isPublic(getMethod().getModifiers());
        return result;
    }

    /**
     * @return {@code true} if the method is declared {@code private}, {@code false} otherwise
     */
    protected boolean isPrivate() {
        final boolean result = Modifier.isPrivate(getMethod().getModifiers());
        return result;
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code message} with the {@code context}.
     *
     * @param <R>     the type of the expected handler invocation result
     * @param message the message to handle
     * @param context the context of the message
     * @return the result of message handling
     * @throws InvocationTargetException if the wrapped method throws any {@link Throwable} that is not an {@link Error}.
     *                                   {@code Error} instances are propagated as-is.
     */
    protected <R> R invoke(Message message, C context) throws InvocationTargetException {

        checkNotNull(message);
        checkNotNull(context);
        try {
            @SuppressWarnings("unchecked")
            final R result = (R) method.invoke(target, message, context);
            return result;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw propagate(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException,ProhibitedExceptionThrown
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code message}.
     *
     * @param <R>     the type of the expected handler invocation result
     * @param message a message to handle
     * @return the result of message handling
     * @throws InvocationTargetException if the wrapped method throws any {@link Throwable} that is not an {@link Error}.
     *                                   {@code Error} instances are propagated as-is.
     */
    protected <R> R invoke(Message message) throws InvocationTargetException {
        checkNotNull(message);
        try {
            @SuppressWarnings("unchecked") final
            R result = (R) method.invoke(target, message);
            return result;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw propagate(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException,ProhibitedExceptionThrown
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Returns a full name of the handler method.
     *
     * <p>The full name consists of a fully qualified class name of the target object and
     * the method name separated with a dot character.
     *
     * @return full name of the subscriber
     */
    public String getFullName() {
        return Methods.getFullMethodName(method);
    }

    /**
     * @return the class of the target object.
     */
    public Class<?> getTargetClass() {
        return target.getClass();
    }

    /**
     * @return full name of the handler method
     * @see #getFullName()
     */
    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        // We need to hash only by the target's identity.
        return (prime + method.hashCode()) * prime
                + System.identityHashCode(target);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MessageHandlerMethod other = (MessageHandlerMethod) obj;

        // Use == to verify that the instances of the target objects are the same.
        // This way we'd allow having handlers for target objects that are otherwise equal.
        return (this.target == other.target)
                && Objects.equals(this.method, other.method);
    }
}

