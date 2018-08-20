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

import com.google.common.collect.ImmutableSet;
import io.spine.server.model.MethodExceptionChecker;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import static com.google.common.base.Joiner.on;
import static io.spine.server.model.MethodExceptionChecker.forMethod;
import static io.spine.server.model.declare.SignatureMismatch.Severity.ERROR;
import static io.spine.server.model.declare.SignatureMismatch.Severity.WARN;
import static io.spine.server.model.declare.SignatureMismatch.create;
import static java.lang.String.format;

/**
 * @author Alex Tymchenko
 */
public enum MatchCriterion {

    RETURN_TYPE(ERROR,
                "The return type of `%s` method does not match the constraints " +
                        "set for `%s`-annotated method.") {
        @Override
        Optional<SignatureMismatch> test(Method method, MethodSignature<?, ?> signature) {
            Class<?> returnType = method.getReturnType();
            boolean conforms = signature.getValidReturnTypes()
                                        .stream()
                                        .anyMatch(type -> type.isAssignableFrom(returnType));
            if (!conforms) {
                SignatureMismatch mismatch =
                        create(this,
                               methodAsString(method),
                               signature.getAnnotation()
                                        .getSimpleName());
                return Optional.of(mismatch);
            }
            return Optional.empty();
        }
    },

    ACCESS_MODIFIER(WARN,
                    "The access modifier of `%s` method must be `%s`, but it is `%s`.") {
        @Override
        Optional<SignatureMismatch> test(Method method, MethodSignature<?, ?> signature) {

            ImmutableSet<AccessModifier> allowedModifiers = signature.getAllowedModifiers();
            boolean hasMatch = allowedModifiers
                    .stream()
                    .anyMatch(m -> m.test(method));
            if (!hasMatch) {
                SignatureMismatch mismatch =
                        create(this,
                               methodAsString(method),
                               AccessModifier.asString(allowedModifiers),
                               Modifier.toString(method.getModifiers()));
                return Optional.of(mismatch);

            }
            return Optional.empty();
        }
    },

    PROHIBITED_EXCEPTION(ERROR, "%s") {
        @Override
        Optional<SignatureMismatch> test(Method method, MethodSignature<?, ?> signature) {
            //TODO:2018-08-15:alex.tymchenko: add non-throwing behavior to `MethodExceptionChecker`.
            try {
                MethodExceptionChecker checker = forMethod(method);
                Collection<Class<? extends Throwable>> allowed = signature.getAllowedExceptions();
                checker.checkThrowsNoExceptionsBut(allowed);
                return Optional.empty();
            } catch (IllegalStateException e) {
                SignatureMismatch mismatch = create(this,
                                                    e.getMessage());
                return Optional.of(mismatch);
            }
        }
    },

    PARAMETER_LIST(ERROR,
                   "`%s` method has an invalid parameter list. " +
                           "Please refer to `%s` annotation docs for allowed parameters.") {
        @Override
        Optional<SignatureMismatch> test(Method method, MethodSignature<?, ?> signature) {
            Optional<? extends ParameterSpec<?>> matching =
                    MethodParams.findMatching(method, signature.getParamSpecClass());
            if (!matching.isPresent()) {
                SignatureMismatch mismatch =
                        create(this,
                               methodAsString(method),
                               signature.getAnnotation()
                                        .getSimpleName());
                return Optional.of(mismatch);
            }
            return Optional.empty();
        }
    };

    private final SignatureMismatch.Severity severity;
    private final String format;

    MatchCriterion(SignatureMismatch.Severity severity, String format) {
        this.severity = severity;
        this.format = format;
    }

    SignatureMismatch.Severity getSeverity() {
        return severity;
    }

    String formatMsg(Object... args) {
        String message = format(Locale.ROOT, format, args);
        return message;
    }

    abstract Optional<SignatureMismatch> test(Method method, MethodSignature<?, ?> signature);

    private static String methodAsString(Method method) {
        String result =
                on(".").join(method.getDeclaringClass()
                                   .getCanonicalName(),
                             method.getName()) + '(' +
                        on(", ")
                                .join(method.getParameterTypes()) +
                        ')';
        return result;
    }
}
