/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.string.Diags;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.spine.server.model.MethodExceptionCheck.check;
import static io.spine.server.model.SignatureMismatch.Severity.ERROR;
import static io.spine.server.model.SignatureMismatch.Severity.WARN;
import static io.spine.string.Diags.toEnumerationBackticked;
import static java.lang.String.format;

/**
 * The criteria of {@linkplain Method method} matching to a certain {@linkplain ReceptorSignature
 * set of requirements}, applied to the receptors.
 *
 * <p>Each criterion defines the {@linkplain #severity() severity} of its violation.
 * Depending on it, the callees may refuse working with the tested methods.
 *
 * <p>Additionally, upon testing the criteria provide a number of {@linkplain SignatureMismatch
 * signature mismatches}, that may later be used for diagnostic purposes.
 */
@Internal
public enum MatchCriterion {

    /**
     * The criterion, which checks that the method return type is among the
     * {@linkplain ReceptorSignature#returnTypes() expected}.
     */
    RETURN_TYPE(ERROR,
                "The return type of `%s` method does not match the constraints"
                        + " set for `%s`-annotated method.") {
        @Override
        Optional<SignatureMismatch> test(Method method, ReceptorSignature<?, ?> signature) {
            if (signature.returnTypeMatches(method)) {
                return Optional.empty();
            }
            return createMismatch(method, signature.annotation());
        }
    },
    /**
     * The criterion for the method parameters to conform the
     * {@linkplain ReceptorSignature#params() requirements}.
     *
     * @see AllowedParams#findMatching(Method)
     */
    PARAMETERS(ERROR, "The method `%s` has invalid parameters.%n"
             + "       Please refer to the documentation of `@%s` for allowed parameter types.") {
        @Override
        Optional<SignatureMismatch> test(Method method, ReceptorSignature<?, ?> signature) {
            var matching = signature.params().findMatching(method);
            if (matching.isPresent()) {
                return Optional.empty();
            }
            return createMismatch(method, signature.annotation());
        }
    },

    /**
     * The criterion, which ensures that the method access modifier is among the
     * {@linkplain ReceptorSignature#modifier() expected}.
     */
    ACCESS_MODIFIER(WARN, "The access modifier of `%s` method is `%s`.%n"
               + "         We recommend it to be one of: %s.%n"
               + "         Please refer to the `%s` annotation docs for details.") {
        @Override
        Optional<SignatureMismatch> test(Method method, ReceptorSignature<?, ?> signature) {
            var recommended = signature.modifier();
            var hasMatch = recommended.stream().anyMatch(m -> m.test(method));
            if (hasMatch) {
                return Optional.empty();
            }
            return createMismatch(method, signature, recommended);
        }

        private Optional<SignatureMismatch>
        createMismatch(Method method,
                       ReceptorSignature<?, ?> signature,
                       ImmutableSet<AccessModifier> recommended) {
            var methodReference = methodAsString(method);
            var annotationName = signature.annotation().getSimpleName();
            var currentModifier = AccessModifier.fromMethod(method);
            return SignatureMismatch.create(
                    this, methodReference, currentModifier, recommended, annotationName);
        }
    },

    /**
     * The criterion checking that the tested method throws only
     * {@linkplain ReceptorSignature#allowedThrowable() allowed exceptions}.
     */
    PROHIBITED_EXCEPTION(ERROR, "%s") {

        @Override
        Optional<SignatureMismatch> test(Method method, ReceptorSignature<?, ?> signature) {
            @Nullable Class<? extends Throwable> allowed =
                    signature.allowedThrowable().orElse(null);
            var checker = check(method, allowed);
            var prohibited = checker.findProhibited();
            if (prohibited.isEmpty()) {
                return Optional.empty();
            }
            var errorMessage = new ProhibitedExceptionMessage(method, prohibited, allowed);
            return SignatureMismatch.create(this, errorMessage);
        }
    };

    private final SignatureMismatch.Severity severity;
    private final String format;

    /**
     * Creates an instance with the given severity and the template of the signature
     * mismatch message.
     */
    MatchCriterion(SignatureMismatch.Severity severity, String format) {
        this.severity = severity;
        this.format = format;
    }

    /**
     * Tests the method against the rules defined by the signature.
     *
     * @param method
     *         the method to test
     * @param signature
     *         the signature to use in testing
     * @return {@link Optional#empty() Optional.empty()} if there was no mismatch,
     *         or the {@code Optional<SignatureMismatch>} of the mismatch detected.
     */
    abstract Optional<SignatureMismatch> test(Method method, ReceptorSignature<?, ?> signature);

    protected final SignatureMismatch.Severity severity() {
        return severity;
    }

    protected final String formatMsg(Object... args) {
        var message = format(Locale.ROOT, format, args);
        return message;
    }

    /**
     * Creates a mismatch with the passed method and the name as parameters.
     */
    protected final Optional<SignatureMismatch>
    createMismatch(Method method, Class<? extends Annotation> annotation) {
        var methodReference = methodAsString(method);
        var annotationName = annotation.getSimpleName();
        return SignatureMismatch.create(this, methodReference, annotationName);
    }

    private static String methodAsString(Method method) {
        var declaringClassName = method.getDeclaringClass().getName();
        var paramTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.<String>toUnmodifiableList());
        var parameterTypes = Diags.join(paramTypes);
        var result = format("%s.%s(%s)", declaringClassName, method.getName(), parameterTypes);
        return result;
    }

    /**
     * Helper class for {@link #PROHIBITED_EXCEPTION} for composing an error message.
     */
    static final class ProhibitedExceptionMessage {

        private static final String METHOD_THROWS = "The method `%s.%s` throws `%s`.%n";

        private final Method method;
        private final ImmutableList<Class<? extends Throwable>> declared;
        private final @Nullable Class<? extends Throwable> allowed;

        ProhibitedExceptionMessage(Method method,
                                   ImmutableList<Class<? extends Throwable>> declared,
                                   @Nullable Class<? extends Throwable> allowed) {
            this.method = method;
            this.declared = declared;
            this.allowed = allowed;
        }

        @Override
        public String toString() {
            if (allowed == null) {
                return format(
                        METHOD_THROWS + "Throwing is not allowed for this kind of methods.",
                        method.getDeclaringClass().getCanonicalName(),
                        method.getName(),
                        enumerateThrown()
                );
            }
            return format(
                    METHOD_THROWS + "Only `%s` is allowed for this kind of methods.",
                    method.getDeclaringClass().getCanonicalName(),
                    method.getName(),
                    enumerateThrown(),
                    allowed.getName()
            );
        }

        /**
         * Prints {@code Iterable} to {@code String}, separating elements with comma.
         */
        private String enumerateThrown() {
            return declared.stream()
                    .collect(toEnumerationBackticked());
        }
    }
}

