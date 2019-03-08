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

package io.spine.server.model.declare;

import com.google.common.collect.ImmutableSet;
import io.spine.logging.Logging;
import io.spine.server.model.HandlerMethod;
import io.spine.server.type.MessageEnvelope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.declare.AccessModifier.PACKAGE_PRIVATE;
import static io.spine.server.model.declare.MethodParams.findMatching;
import static java.util.stream.Collectors.toList;

/**
 * An abstract base of signatures of a {@linkplain HandlerMethod handler method}s.
 *
 * <p>Sets the requirements to meet for the {@linkplain Method java.lang.reflect.Method}
 * in order to be qualified as a {@code Message} handler method.
 *
 * <p>By extending this base class, descendants define the number of requirements:
 * <ul>
 *     <li>{@linkplain #MethodSignature(Class) the method annotation},
 *     <li>{@linkplain #paramSpecs() the specification of method parameters},
 *     <li>{@linkplain #allowedModifiers() the set of allowed access modifiers},
 *     <li>{@linkplain #validReturnTypes() the set of valid return types},
 *     <li>{@linkplain #allowedExceptions() the set of allowed exceptions}, that the method
 * declares to throw (empty by default),
 * </ul>
 *
 * @param <H>
 *         the type of the handler method
 * @param <E>
 *         the type of envelope, which is used to invoke the handler method
 */
public abstract class MethodSignature<H extends HandlerMethod<?, ?, E, ?, ?>,
                                      E extends MessageEnvelope<?, ?, ?>> implements Logging {

    private final Class<? extends Annotation> annotation;

    /**
     * Creates an instance of signature, defining the required annotation to be present
     * in the methods, that are matched against this signature.
     */
    protected MethodSignature(Class<? extends Annotation> annotation) {
        this.annotation = checkNotNull(annotation);
    }

    /**
     * Obtains the specification of handler parameters to meet.
     */
    public abstract ImmutableSet<? extends ParameterSpec<E>> paramSpecs();

    /**
     * Obtains the set of allowed access modifiers for the method.
     *
     * <p>By default, obtains a set of single value {@link AccessModifier#PACKAGE_PRIVATE}. Most of
     * the implementations should be fine with this behaviour. Override this method to change
     * the allowed access modifiers.
     */
    protected ImmutableSet<AccessModifier> allowedModifiers() {
        return ImmutableSet.of(PACKAGE_PRIVATE);
    }

    /**
     * Obtains the set of valid return types.
     */
    protected abstract ImmutableSet<Class<?>> validReturnTypes();

    /**
     * Obtains the set of allowed exceptions that method may declare to throw.
     */
    protected ImmutableSet<Class<? extends Throwable>> allowedExceptions() {
        return ImmutableSet.of();
    }

    /**
     * Checks whether the passed {@code method} matches the constraints set by this
     * {@code MethodSignature} instance.
     *
     * <p>{@link SignatureMismatch.Severity#WARN WARN}-level mismatches are silently ignored
     * by this method. To obtain a detailed information callees should use
     * {@linkplain #match(Method) match(Method)}.
     *
     * @param method
     *         the method to check
     * @return true if there was no {@link SignatureMismatch.Severity#ERROR ERROR}-level mismatches
     * @throws SignatureMismatchException
     *         in case of any {@link SignatureMismatch.Severity#ERROR ERROR}-level mismatches
     * @implNote This method never returns {@code false} (rather throwing an exception),
     *         because in future the extended diagnostic, based upon {@linkplain SignatureMismatch
     *         signature mismatches} found is going to be implemented.
     */
    public boolean matches(Method method) throws SignatureMismatchException {
        if (skipMethod(method)) {
            return false;
        }
        Collection<SignatureMismatch> mismatches = match(method);
        boolean hasErrors = mismatches.stream()
                                      .anyMatch(SignatureMismatch::isError);
        List<SignatureMismatch> warnings =  mismatches.stream()
                                                      .filter(SignatureMismatch::isWarning)
                                                      .collect(toList());
        if (hasErrors) {
            throw new SignatureMismatchException(mismatches);
        }
        if (!warnings.isEmpty()) {
            warnings.stream()
                    .map(SignatureMismatch::toString)
                    .forEach(this::_warn);
        }
        return true;
    }

    /**
     * Determines, if the given raw {@code method} should be skipped as non-matching.
     *
     * <p>Such an approach allows to improve performance by skipping the methods, that a priori
     * cannot be qualified as message handler methods, such as methods with no
     * {@linkplain #annotation() required annotation}.
     *
     * @param method
     *         the method to determine if it should be inspected at all
     * @return {@code true} if this method should be walked through further examination,
     *         {@code false} otherwise
     */
    protected boolean skipMethod(Method method) {
        return !method.isAnnotationPresent(annotation);
    }

    /**
     * Creates the {@linkplain HandlerMethod HandlerMethod} instance according to the passed
     * raw method and the parameter specification.
     *
     * <p>By implementing this method descendants define how the parameter spec is used to fit
     * the {@code Message} envelope onto the parameter list during the method invocation.
     *
     * <p>This method is designed to NOT perform any matching, but rather create a specific
     * instance of {@code HandlerMethod}.
     *
     * @param method the raw method to wrap into a {@code HandlerMethod} instance being created
     * @param parameterSpec the specification of method parameters
     * @return new instance of {@code HandlerMethod}
     */
    public abstract H doCreate(Method method, ParameterSpec<E> parameterSpec);

    /**
     * Obtains the annotation, which is required to be declared for the matched raw method.
     */
    public Class<? extends Annotation> annotation() {
        return annotation;
    }

    /**
     * Creates a {@linkplain HandlerMethod handler method} from a raw method.
     *
     * <p>Before creation performs {@linkplain #matches(Method) matching } against the signature.
     *
     * @param method
     *         the method to create wrapper from
     * @return a wrapper object created from the method
     * @throws SignatureMismatchException
     *         in case there are
     *         {@link io.spine.server.model.declare.SignatureMismatch.Severity#ERROR ERROR}-level
     *         mismatches
     */
    public Optional<H> create(Method method) {
        boolean matches = matches(method);
        if (!matches) {
            return Optional.empty();
        }
        Optional<? extends ParameterSpec<E>> matchingSpec = findMatching(method, paramSpecs());
        return matchingSpec.map(spec -> {
            H handler = doCreate(method, spec);
            handler.discoverAttributes();
            return handler;
        });
    }

    /**
     * Match the method against the {@linkplain MatchCriterion criteria} and obtain a collection
     * of mismatches, if any.
     *
     * <p>NOTE: this method does not test the presence of annotation.
     *
     * @param method
     *         the method to match.
     * @return the collection of signature mismatches, if any
     */
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
