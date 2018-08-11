/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.model;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An abstract base for wrappers over methods handling messages.
 *
 * <p>Two message handlers are equivalent when they refer to the same method on the
 * same object (not class).
 *
 * @param <T> the type of the target object
 * @param <M> the type of the message class
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@Immutable
public abstract class AbstractHandlerMethod<T,
                                            M extends MessageClass,
                                            E extends MessageEnvelope<?, ?, ?>,
                                            R extends MethodResult>
        implements HandlerMethod<T, M, E, R> {

    /** The method to be called. */
    @SuppressWarnings("Immutable")
    private final Method method;

    /** The class of the first parameter. */
    private final Class<? extends Message> messageClass;

    /**
     * The set of the metadata attributes set via method annotations.
     *
     * @implNote Even though that {@code MethodAttribute} is parameterized with {@code Object},
     * which is mutable, the {@code @Immutable} annotation of {@code MethodAttribute} class
     * ensures that we don't have mutable types passed as generic parameters.
     */
    @SuppressWarnings("Immutable")
    private final ImmutableSet<MethodAttribute<?>> attributes;

    private final MessageAcceptor<E> acceptor;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    protected AbstractHandlerMethod(Method method, MessageAcceptor<E> acceptor) {
        this.method = checkNotNull(method);
        this.messageClass = getFirstParamType(method);
        this.attributes = discoverAttributes(method);
        this.acceptor = checkNotNull(acceptor);
        method.setAccessible(true);
    }

    protected final Class<? extends Message> rawMessageClass() {
        return messageClass;
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
        @SuppressWarnings("unchecked") /* We always expect first param as `Message`. */
        Class<? extends Message> result =
                (Class<? extends Message>) handler.getParameterTypes()[0];
        return result;
    }

    /**
     * Returns the handling method.
     */
    @Override
    public Method getRawMethod() {
        return method;
    }

    private int getModifiers() {
        return method.getModifiers();
    }

    /** Returns {@code true} if the method is declared {@code public}, {@code false} otherwise. */
    protected boolean isPublic() {
        boolean result = Modifier.isPublic(getModifiers());
        return result;
    }

    /** Returns {@code true} if the method is declared {@code private}, {@code false} otherwise. */
    protected boolean isPrivate() {
        boolean result = Modifier.isPrivate(getModifiers());
        return result;
    }

    /** Returns the set of method attributes configured for this method. */
    @Override
    public Set<MethodAttribute<?>> getAttributes() {
        return attributes;
    }

    private static ImmutableSet<MethodAttribute<?>> discoverAttributes(Method method) {
        checkNotNull(method);
        ExternalAttribute externalAttribute = ExternalAttribute.of(method);
        return ImmutableSet.of(externalAttribute);
    }

    @CanIgnoreReturnValue
    @Override
    public final R invoke(T target, E envelope) {
        checkNotNull(target);
        checkNotNull(envelope);
        Message message = envelope.getMessage();
        Message context = envelope.getMessageContext();
        try {
            Object rawOutput = acceptor.invoke(target, method, envelope);
            R result = toResult(target, rawOutput);
            return result;
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new HandlerMethodFailedException(target, message, context, e);
        }
    }

    /**
     * Converts the output of the raw method call to the result object.
     */
    protected abstract R toResult(T target, Object rawMethodOutput);

    /**
     * Returns a full name of the handler method.
     *
     * <p>The full name consists of a fully qualified class name of the target object and
     * the method name separated with a dot character.
     *
     * @return full name of the subscriber
     */
    public String getFullName() {
        String template = "%s.%s()";
        String className = method.getDeclaringClass()
                                 .getName();
        String methodName = method.getName();
        String result = format(template, className, methodName);
        return result;
    }

    @Override
    public HandlerKey key() {
        HandlerKey result = HandlerKey.of(getMessageClass());
        return result;
    }

    @Override
    public int hashCode() {
        int prime = 31;
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
        AbstractHandlerMethod other = (AbstractHandlerMethod) obj;

        return Objects.equals(this.method, other.method);
    }

    /**
     * @return full name of the handler method
     * @see #getFullName()
     */
    @Override
    public String toString() {
        return getFullName();
    }
}
