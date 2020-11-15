/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.server.type.MessageEnvelope;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Specifies allowed combinations of parameters that a handler method may accept.
 *
 * @param <E>
 *          the type of the message envelope passed to the handler methods
 */
@Immutable
public final class AllowedParams<E extends MessageEnvelope<?, ?, ?>> {

    private final ImmutableList<? extends ParameterSpec<E>> specs;

    /**
     * Constructs new instance using passed parameter specifications.
     *
     * @param spec
     *         specification of allowed parameters in the order of importance
     */
    @SafeVarargs
    public AllowedParams(ParameterSpec<E>... spec) {
        checkNotNull(spec);
        this.specs = ImmutableList.copyOf(spec);
    }

    /**
     * Finds matching parameter specification based on the parameters of the given method and
     * the class, describing the parameter specification.
     *
     * @param method
     *         the method, which parameter list is used to match the parameter specification
     * @return the matching parameter spec,
     *         or {@link Optional#empty() Optional.empty()} if no matching specification is found
     */
    Optional<? extends ParameterSpec<E>> findMatching(Method method) {
        MethodParams params = MethodParams.of(method);
        Optional<? extends ParameterSpec<E>> result =
                specs.stream()
                     .filter(spec -> spec.matches(params))
                     .findFirst();
        return result;
    }

    /**
     * Obtains specification of allowed parameters as a list.
     *
     * <p>The order of the parameters is the same as passed during
     * {@linkplain #AllowedParams(ParameterSpec[]) construction}.
     */
    public ImmutableList<? extends ParameterSpec<E>> asList() {
        return specs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AllowedParams<?> params = (AllowedParams<?>) o;
        return Objects.equal(specs, params.specs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(specs);
    }
}
