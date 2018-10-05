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
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import io.spine.core.MessageEnvelope;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An abstract base for wrappers over methods handling messages.
 *
 * <p>Two message handlers are equivalent when they refer to the same method on the
 * same object (not class).
 *
 * @param <T> the type of the target object
 * @param <M> the type of the message handled by this method
 * @param <C> the type of the message class
 * @param <E> the type of message envelopes, in which the messages to handle are wrapped
 * @param <R> the type of the method invocation result
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@Immutable
public abstract class AbstractHandlerMethod<T,
                                            M extends Message,
                                            C extends MessageClass<M>,
                                            E extends MessageEnvelope<?, ?, ?>,
                                            R extends MethodResult>
        implements HandlerMethod<T, C, E, R> {

    /** The method to be called. */
    @SuppressWarnings("Immutable")
    private final Method method;

    /** The class of the first parameter. */
    private final Class<M> messageClass;

    /**
     * The set of the metadata attributes set via method annotations.
     *
     * @implNote Even though that {@code MethodAttribute} is parameterized with {@code Object},
     * which is mutable, the {@code @Immutable} annotation of {@code MethodAttribute} class
     * ensures that we don't have mutable types passed as generic parameters.
     *
     * <p>This field is initialized in {@link #discoverAttributes()} to allow derived classes to
     * {@linkplain #attributeSuppliers() customize} the set of supported method attributes.
     */
    @SuppressWarnings("Immutable")
    private ImmutableSet<MethodAttribute<?>> attributes;

    /**
     * The specification of parameters for this method.
     *
     * @implNote It serves to extract the argument values from the {@linkplain E envelope} used
     * as a source for the method call.
     */
    private final ParameterSpec<E> parameterSpec;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method
     *         subscriber method
     * @param parameterSpec
     *         the specification of method parameters
     */
    protected AbstractHandlerMethod(Method method,
                                    ParameterSpec<E> parameterSpec) {
        this.method = checkNotNull(method);
        this.messageClass = getFirstParamType(method);
        this.attributes = discoverAttributes(method);
        this.parameterSpec = parameterSpec;

        method.setAccessible(true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes attributes by obtaining values via functions provided by
     * {@link #attributeSuppliers()}.
     *
     * @see #attributeSuppliers()
     */
    @Override
    @PostConstruct
    public final void discoverAttributes() {
        ImmutableSet.Builder<MethodAttribute<?>> builder = ImmutableSet.builder();
        for (Function<Method, MethodAttribute<?>> fn : attributeSuppliers()) {
            MethodAttribute<?> attr = fn.apply(method);
            builder.add(attr);
        }
        attributes = builder.build();
    }

    /**
     * Obtains a set of functions for getting {@linkplain MethodAttribute method attributes}
     * by a {@linkplain Method raw method} value.
     *
     * <p>Default implementation returns a one-element set for obtaining {@link ExternalAttribute}.
     *
     * <p>Overriding classes must return a set which is a
     * {@linkplain com.google.common.collect.Sets#union(Set, Set) union} of the
     * set provided by this method and the one needed by the overriding class.
     */
    @OverridingMethodsMustInvokeSuper
    protected Set<Function<Method, MethodAttribute<?>>> attributeSuppliers() {
        return ImmutableSet.of(ExternalAttribute::of);
    }

    protected final Class<M> rawMessageClass() {
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
    static <M extends Message> Class<M> getFirstParamType(Method handler) {
        @SuppressWarnings("unchecked")
            // We always expect first param as a Message of required type.
        Class<M> result = (Class<M>) handler.getParameterTypes()[0];
        return result;
    }

    @Override
    public Method getRawMethod() {
        return method;
    }

    private int getModifiers() {
        return method.getModifiers();
    }

    protected ParameterSpec<E> getParameterSpec() {
        return parameterSpec;
    }

    /**
     * Returns {@code true} if the method is declared {@code public},
     * {@code false} otherwise.
     */
    protected boolean isPublic() {
        boolean result = Modifier.isPublic(getModifiers());
        return result;
    }

    /**
     * Returns {@code true} if the method is declared {@code private},
     * {@code false} otherwise.
     */
    protected boolean isPrivate() {
        boolean result = Modifier.isPrivate(getModifiers());
        return result;
    }

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
    public R invoke(T target, E envelope) {
        checkNotNull(target);
        checkNotNull(envelope);
        checkAttributesMatch(envelope);
        try {
            Object[] arguments = parameterSpec.extractArguments(envelope);
            Object rawOutput = method.invoke(target, arguments);
            R result = toResult(target, rawOutput);
            return result;
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            Message message = envelope.getMessage();
            Message context = envelope.getMessageContext();
            throw new HandlerMethodFailedException(target, message, context, e);
        }
    }

    /**
     * Allows to make sure that the passed envelope matches the annotation attributes of a method.
     *
     * <p>Default implementation does nothing. Descending classes may override for checking
     * the match.
     *
     * @throws IllegalArgumentException
     *         the default implementation does not throw ever. Descending classes would throw
     *         if the annotation arguments do not match the message of the passed envelope.
     * @param envelope the envelope with the massed to handle
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // Optional for descendants.
    protected void checkAttributesMatch(E envelope) throws IllegalArgumentException {
        // Do nothing by default.
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
    public HandlerToken token() {
        TypeUrl messageType = getMessageClass().getTypeName()
                                               .toUrl();
        return HandlerToken
                .newBuilder()
                .setMessageType(messageType.value())
                .build();
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
     * Obtains the full name of the handler method.
     *
     * @see #getFullName()
     */
    @Override
    public String toString() {
        return getFullName();
    }
}
