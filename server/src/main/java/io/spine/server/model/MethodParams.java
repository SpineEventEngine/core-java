/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.server.event.model.SubscriberMethod;
import io.spine.server.type.MessageEnvelope;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides information about parameters of a method.
 */
@Immutable
public final class MethodParams {

    private final ImmutableList<Class<?>> params;
    private final @Nullable ArgumentFilter filter;

    /**
     * Obtains parameters of the subscriber method.
     *
     * <p>Parameters may include an argument filter.
     */
    public static MethodParams of(SubscriberMethod method) {
        checkNotNull(method);
        ArgumentFilter filter = method.filter();
        Class<?>[] paramTypes = method.rawMethod()
                                      .getParameterTypes();
        return new MethodParams(filter, paramTypes);
    }

    /**
     * Obtains parameters from the passed method.
     */
    public static MethodParams of(Method method) {
        checkNotNull(method);
        return new MethodParams(null, method.getParameterTypes());
    }

    /**
     * Creates the instance with single parameter of the passed type.
     */
    static MethodParams ofType(Class<?> type) {
        checkNotNull(type);
        return new MethodParams(null, type);
    }

    private MethodParams(@Nullable ArgumentFilter filter, Class<?>... types) {
        this(filter, ImmutableList.copyOf(types));
    }

    private MethodParams(@Nullable ArgumentFilter filter, ImmutableList<Class<?>> params) {
        this.filter = filter;
        this.params = params;
    }

    /** Obtains the number of parameters passed to the method. */
    public int size() {
        return params.size();
    }

    /**
     * Obtains the argument filter if it is specified for the first argument of
     * this instance of method parameters.
     */
    public Optional<ArgumentFilter> filter() {
        return Optional.ofNullable(filter);
    }

    /**
     * Obtains the type of the method parameter.
     *
     * @param index the zero-based index of the type.
     */
    public Class<?> type(int index) {
        Class<?> type = params.get(index);
        return type;
    }

    /**
     * Verifies if the method has only one parameter of the passed type.
     */
    public boolean is(Class<?> type) {
        if (size() != 1) {
            return false;
        }
        Class<?> firstParam = type(0);
        return type.isAssignableFrom(firstParam);
    }

    /**
     * Verifies if the method has only two parameters and they match the passed types.
     */
    public boolean are(Class<?> type1, Class<?> type2) {
        if (size() != 2) {
            return false;
        }
        boolean firstMatches = type1.isAssignableFrom(type(0));
        boolean secondMatches = type2.isAssignableFrom(type(1));
        return firstMatches && secondMatches;
    }

    /**
     * Verifies if these parameters match the passed types.
     */
    public boolean are(Class<?>... types) {
        return match(ImmutableList.copyOf(types));
    }

    /**
     * Verifies if these parameters match the passed types.
     */
    public boolean match(List<Class<?>> types) {
        if (size() != types.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            Class<?> actual = type(i);
            Class<?> expected = types.get(i);
            if (!expected.isAssignableFrom(actual)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds matching parameter specification based on the parameters of the given method and
     * the class, describing the parameter specification.
     *
     * @param method
     *         the method, which parameter list is used to match the parameter specification
     * @param paramSpecs
     *         the class of parameter specification
     * @param <E>
     *         the type of message envelope, which the parameter specification class handles
     * @param <S>
     *         the type of the parameter specification
     * @return the matching parameter spec,
     *         or {@link Optional#empty() Optional.empty()} if no matching specification is found
     */
    static <E extends MessageEnvelope<?, ?, ?>, S extends ParameterSpec<E>>
    Optional<S> findMatching(Method method, Collection<S> paramSpecs) {
        MethodParams params = of(method);
        Optional<S> result =
                paramSpecs.stream()
                          .filter(spec -> spec.matches(params))
                          .findFirst();
        return result;
    }

    /**
     * Finds out if the first method parameter is a {@linkplain CommandMessage}.
     *
     * @param method
     *         the method to inspect
     * @return {@code true} if the first parameter is a {@code Command} message,
     *         {@code false} otherwise
     */
    public static boolean firstIsCommand(Method method) {
        checkNotNull(method);
        MethodParams params = of(method);
        if (params.size() == 0) {
            return false;
        }
        boolean result = CommandMessage.class.isAssignableFrom(params.type(0));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodParams other = (MethodParams) o;
        return params.equals(other.params) &&
                Objects.equals(filter, other.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, filter);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // others also have the `filter` field.
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("params", params)
                          .add("filter", filter)
                          .toString();
    }
}
