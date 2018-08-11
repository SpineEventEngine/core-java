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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.MethodExceptionChecker.forMethod;

/**
 * The base class for factory objects that can filter {@link Method} objects
 * that represent handler methods and create corresponding {@code HandlerMethod} instances
 * that wrap those methods.
 *
 * @param <H> the type of the handler method objects to create
 */
public abstract class MethodFactory<H extends HandlerMethod, A extends MessageAcceptor> {

    private final Class<? extends Annotation> annotation;
    private final ImmutableSet<Class<?>> validReturnTypes;

    protected MethodFactory(Class<? extends Annotation> annotation,
                            Set<Class<?>> types) {
        this.annotation = checkNotNull(annotation);
        this.validReturnTypes = ImmutableSet.copyOf(types);
    }

    /** Returns the class of the method wrapper. */
    public abstract Class<H> getMethodClass();

    /**
     * Checks an access modifier of the method and logs a warning if it is invalid.
     *
     * @param method the method to check
     * @see MethodAccessChecker
     */
    public abstract void checkAccessModifier(Method method);

    /**
     * Creates a {@linkplain HandlerMethod handler method} from a raw method.
     *
     * <p>Performs various checks before wrapper creation, e.g. method access modifier or
     * whether method throws any prohibited exceptions.
     *
     * @param method the method to create wrapper from
     * @return a wrapper object created from the method
     * @throws IllegalStateException in case some of the method checks fail
     */
    public Optional<H> create(Method method) {
        boolean validMethod = annotationMatches(method) && returnTypeMatches(method);
        if (validMethod) {
            checkAccessModifier(method);
            checkThrownExceptions(method);
            Optional<? extends A> acceptor = findAcceptorFor(method);
            Optional<H> result = acceptor.map(
                    messageAcceptor -> doCreate(method, messageAcceptor)
            );
            return result;
        } else {
            return Optional.empty();
        }
    }

    /** Creates a wrapper object from a method. */
    protected abstract H doCreate(Method method, A acceptor);

    /**
     * Ensures method does not throw any prohibited exception types.
     *
     * <p>In case it does, the {@link IllegalStateException} containing diagnostics info is
     * thrown.
     *
     * @param method the method to check
     * @throws IllegalStateException if the method throws any prohibited exception types
     * @see MethodExceptionChecker
     */
    protected void checkThrownExceptions(Method method) {
        MethodExceptionChecker checker = forMethod(method);
        checker.checkDeclaresNoExceptionsThrown();
    }

    private boolean annotationMatches(Method method) {
        return method.isAnnotationPresent(annotation);
    }

    private boolean returnTypeMatches(Method method) {
        Class<?> returnType = method.getReturnType();
        return validReturnTypes.stream()
                               .anyMatch(type -> type.isAssignableFrom(returnType));
    }

    private Optional<? extends A> findAcceptorFor(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return findAcceptorForParameters(parameters);
    }

    protected abstract
    Optional<? extends A> findAcceptorForParameters(Class<?>[] parameterTypes);
}
