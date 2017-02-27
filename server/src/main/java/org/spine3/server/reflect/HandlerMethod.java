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
package org.spine3.server.reflect;

import com.google.common.base.Predicate;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;

/**
 * An abstract base for wrappers over methods handling messages.
 *
 * <p>Two message handlers are equivalent when they refer to the same method on the
 * same object (not class).
 *
 * @param <C> the type of the message context or {@link com.google.protobuf.Empty Empty} if
 *            a context parameter is never used
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public abstract class HandlerMethod<C extends Message> {

    /** The method to be called. */
    private final Method method;

    /** The number of parameters the method has. */
    private final int paramCount;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    protected HandlerMethod(Method method) {
        this.method = checkNotNull(method);
        this.paramCount = method.getParameterTypes().length;
        method.setAccessible(true);
    }

    /** Returns the handling method. */
    protected Method getMethod() {
        return method;
    }

    private int getModifiers() {
        return method.getModifiers();
    }

    /** Returns {@code true} if the method is declared {@code public}, {@code false} otherwise. */
    protected boolean isPublic() {
        final boolean result = Modifier.isPublic(getModifiers());
        return result;
    }

    /** Returns {@code true} if the method is declared {@code private}, {@code false} otherwise. */
    protected boolean isPrivate() {
        final boolean result = Modifier.isPrivate(getModifiers());
        return result;
    }

    /** Returns the count of the method parameters. */
    protected int getParamCount() {
        return paramCount;
    }

    /**
     * Invokes the wrapped subscriber method to handle {@code message} with the {@code context}.
     *
     * @param <R>     the type of the expected handler invocation result
     * @param target  the target object on which call the method
     * @param message the message to handle   @return the result of message handling
     * @param context the context of the message
     * @throws InvocationTargetException if the wrapped method throws any {@link Throwable} that
     *                                   is not an {@link Error}.
     *                                   {@code Error} instances are propagated as-is.
     */
    public <R> R invoke(Object target, Message message, C context)
            throws InvocationTargetException {
        checkNotNull(message);
        checkNotNull(context);
        try {
            final int paramCount = getParamCount();
            if (paramCount == 1) {
                @SuppressWarnings("unchecked") // it is assumed that the method returns the result of this type
                final R result = (R) method.invoke(target, message);
                return result;
            } else {
                @SuppressWarnings("unchecked") // it is assumed that the method returns the result of this type
                final R result = (R) method.invoke(target, message, context);
                return result;
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throwIfUnchecked(e);
            throw new IllegalStateException(e);
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
        return getFullMethodName(method);
    }

    /**
     * Logs a message at the WARN level according to the specified format and method.
     */
    protected static void warnOnWrongModifier(String messageFormat, Method method) {
        log().warn(messageFormat, getFullMethodName(method));
    }

    /**
     * Returns a full method name without parameters.
     *
     * @param method a method to get name for
     * @return full method name
     */
    private static String getFullMethodName(Method method) {
        return method.getDeclaringClass().getName() + '.' + method.getName() + "()";
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
        return (prime + method.hashCode());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final HandlerMethod other = (HandlerMethod) obj;

        return Objects.equals(this.method, other.method);
    }

    /**
     * Returns the class of the first parameter of the passed handler method object.
     *
     * <p>It is expected that the first parameter of the passed method is always of
     * a class implementing {@link Message}.
     *
     * @param handler the method object to take first parameter type from
     * @return the class of the first method parameter
     * @throws ClassCastException if the first parameter isn't a class implementing {@link Message}
     */
    static Class<? extends Message> getFirstParamType(Method handler) {
        @SuppressWarnings("unchecked") /* we always expect first param as {@link Message} */
        final Class<? extends Message> result =
                (Class<? extends Message>) handler.getParameterTypes()[0];
        return result;
    }

    /**
     * The interface for factory objects that can filter {@link Method} objects
     * that represent handler methods and create corresponding {@code HandlerMethod} instances
     * that wrap those methods.
     *
     * @param <H> the type of the handler method objects to create
     */
    public interface Factory<H extends HandlerMethod> {

        /** Returns the class of the method wrapper. */
        Class<H> getMethodClass();

        /** Creates a wrapper for a method. */
        H create(Method method);

        /** Returns a predicate for filtering methods. */
        Predicate<Method> getPredicate();

        /**
         * Checks an access modifier of the method and logs a warning if it is invalid.
         *
         * @param method the method to check
         * @see HandlerMethod#warnOnWrongModifier(String, Method)
         */
        void checkAccessModifier(Method method);
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(HandlerMethod.class);
    }

    /** The common logger used by message handling method classes. */
    protected static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
