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

package io.spine.server.model.declare;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import io.spine.core.MessageEnvelope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.declare.SignatureMismatch.Severity.ERROR;
import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.stream.Collectors.toList;

/**
 * @author Alex Tymchenko
 */
public abstract class MethodSignature<E extends MessageEnvelope<?, ?, ?>> {

    private final Class<? extends Annotation> annotation;

    public MethodSignature(Class<? extends Annotation> annotation) {
        this.annotation = checkNotNull(annotation);
    }

    boolean matches(Method method) {
        Collection<SignatureMismatch> mismatches = match(method);
        boolean hasErrors = mismatches.stream()
                              .anyMatch(mismatch -> ERROR == mismatch.getSeverity());
        if(hasErrors) {
            throw newIllegalStateException("Error declaring a method. Mismatches: %s",
                                                      Joiner.on(", ").join(mismatches));
        }
        return mismatches.isEmpty();
    }

    Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    protected abstract ImmutableSet<AccessModifier> getAllowedModifiers();

    protected abstract ImmutableSet<Class<? extends Throwable>> getAllowedExceptions();

    protected abstract ImmutableSet<Class<?>> getValidReturnTypes();

    protected abstract Class<? extends ParameterSpec<E>> getParamSpecClass();

    Collection<SignatureMismatch> match(Method method) {
        Collection<SignatureMismatch> result =
                Arrays.stream(MatchCriterion.values())
                      .map(criterion -> criterion.test(method, this))
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(toList());
        return result;
    }
}
