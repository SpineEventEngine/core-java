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
import io.spine.core.MessageEnvelope;
import io.spine.server.model.HandlerMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.declare.MethodParams.findMatching;
import static io.spine.server.model.declare.SignatureMismatch.Severity.ERROR;
import static java.util.stream.Collectors.toList;

/**
 * @author Alex Tymchenko
 */
public abstract class MethodSignature<H extends HandlerMethod<?, ?, E, ?>,
                                      E extends MessageEnvelope<?, ?, ?>> {

    private final Class<? extends Annotation> annotation;

    protected MethodSignature(Class<? extends Annotation> annotation) {
        this.annotation = checkNotNull(annotation);
    }

    public abstract Class<? extends ParameterSpec<E>> getParamSpecClass();

    protected abstract ImmutableSet<AccessModifier> getAllowedModifiers();

    protected abstract ImmutableSet<Class<?>> getValidReturnTypes();

    /**
     * Checks whether the passed {@code method} matches the constraints set by this
     * {@code MethodSignature} instance.
     *
     * @param method
     *         the method to check
     * @return true if there was no {@link SignatureMismatch.Severity#ERROR ERROR}-level mismatches
     * @throws SignatureMismatchException
     *         in case of any {@link SignatureMismatch.Severity#ERROR ERROR}-level mismatches
     * @implNote This method never returns {@code false} (rather throwing an exception),
     *         sincein future the extended diagnostic, based upon {@linkplain SignatureMismatch
     *         signature mismatches} found is going to be implemented.
     */
    public boolean matches(Method method) throws SignatureMismatchException {
        if (!shouldInspect(method)) {
            return false;
        }
        Collection<SignatureMismatch> mismatches = match(method);
        boolean hasErrors = mismatches.stream()
                                      .anyMatch(mismatch -> ERROR == mismatch.getSeverity());
        if (hasErrors) {
            throw new SignatureMismatchException(mismatches);
        }
        return true;
    }

    /**
     * Determines, if the given raw {@code method} is eligible to be inspected.
     *
     * <p>Such an approach allows to improve performance by skipping the methods, that a priori
     * cannot be qualified as message handler methods, such as methods with no
     * {@linkplain #getAnnotation() required annotation}.
     *
     * @param method
     *         the method to determine if it should be inspected at all
     * @return {@code true} if this method should be walked through further examination, {@code
     *         false} otherwise
     */
    protected boolean shouldInspect(Method method) {
        return method.isAnnotationPresent(annotation);
    }

    public abstract H doCreate(Method method, ParameterSpec<E> parameterSpec);

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    protected ImmutableSet<Class<? extends Throwable>> getAllowedExceptions() {
        return ImmutableSet.of();
    }

    /**
     * Creates a {@linkplain HandlerMethod handler method} from a raw method.
     *
     * <p>Performs various checks before wrapper creation, e.g. method access modifier or
     * whether method throws any prohibited exceptions.
     *
     * @param method
     *         the method to create wrapper from
     * @return a wrapper object created from the method
     * @throws IllegalStateException
     *         in case some of the method checks fail
     */
    public Optional<H> create(Method method) {
        boolean matches = matches(method);
        if (!matches) {
            return Optional.empty();
        }
        Optional<? extends ParameterSpec<E>> matchingSpec = findMatching(method,
                                                                         getParamSpecClass());
        return matchingSpec.map(spec -> doCreate(method, spec));

    }

    public Collection<SignatureMismatch> match(Method method) {
        Collection<SignatureMismatch> result =
                Stream.of(MatchCriterion.values())
                      .map(criterion -> criterion.test(method, this))
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .collect(toList());
        return result;
    }
}
