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

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.MethodExceptionChecker.forMethod;

/**
 * The base class for factory objects that can filter {@link Method} objects
 * that represent handler methods and create corresponding {@code HandlerMethod} instances
 * that wrap those methods.
 *
 * @param <H> the type of the handler method objects to create
 * @author Alexander Yevsyukov
 */
public abstract class MethodFactory<H extends HandlerMethod> {

    private final Class<H> methodClass;
    private final Predicate<Method> predicate;

    protected MethodFactory(Class<H> methodClass, Predicate<Method> predicate) {
        this.methodClass = checkNotNull(methodClass);
        this.predicate = checkNotNull(predicate);
    }

    /**
     * Returns the class of the method wrapper.
     */
    public final Class<H> getMethodClass() {
        return this.methodClass;
    }

    /**
     * Returns a predicate for filtering methods.
     */
    public final Predicate<Method> getPredicate() {
        return this.predicate;
    }

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
    public H create(Method method) {
        checkAccessModifier(method);
        checkThrownExceptions(method);
        return doCreate(method);
    }

    /**
     * Creates a model method object from a raw method.
     */
    protected abstract H doCreate(Method method);

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
}
