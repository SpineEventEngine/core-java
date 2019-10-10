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

import com.google.common.collect.ImmutableSet;
import io.spine.testing.logging.MuteLogging;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public abstract class MethodSignatureTest<S extends MethodSignature<?, ?>> {

    protected abstract Stream<Method> validMethods();

    protected abstract Stream<Method> invalidMethods();

    protected abstract S signature();

    @MuteLogging /* Signature mismatch warnings are expected. */
    @DisplayName("create handlers from valid methods")
    @ParameterizedTest
    @MethodSource("validMethods")
    protected final void testValid(Method method) {
        wrap(method).orElseGet(Assertions::fail);
    }

    /**
     * Tests that the handlers are not created from the methods, which signatures do not satisfy
     * the requirements implied by the method annotation, such as {@literal @Assign}.
     *
     * @implNote The classification API of {@link MethodSignature} declares
     *         a {@link SignatureMismatchException} thrown. In most of the cases, the exception is
     *         thrown if the given method does not satisfy the requirements.
     *         However, {@link io.spine.server.command.Command Command}-ing methods are used in two
     *         different scenarios by handling either a {@code Command} or {@code Event} message.
     *         Their behavior is to return {@code Optional.empty()} if no match is found.
     *         All these cases are covered by the implementation of this test.
     */
    @DisplayName("not create handlers from invalid methods")
    @ParameterizedTest
    @MethodSource("invalidMethods")
    protected final void testInvalid(Method method) {
        try {
            Optional<? extends HandlerMethod> result = wrap(method);
            if (result.isPresent()) {
                fail(String.format(
                        "Handler method `%s` should have had an invalid signature.", method
                ));
            }
        } catch (SignatureMismatchException ignored) {
            assertThrows(SignatureMismatchException.class, () -> wrap(method));
        }
    }

    private Optional<? extends HandlerMethod> wrap(Method method) {
        Optional<? extends HandlerMethod> result = signature().classify(method);
        return result;
    }

    /**
     * Finds the method by its name.
     *
     * <p>Throws {@link org.opentest4j.AssertionFailedError assertion error} if no such method
     * found.
     *
     * @param declaringClass
     *         the class in which to search for the method
     * @param name
     *         the name of the method;
     *         expected to be without the class name, visibility level or parameters
     * @return the method
     */
    public static Method findMethod(Class<?> declaringClass, String name) {

        Method result = Stream.of(declaringClass.getDeclaredMethods())
                              .filter(method -> method.getName()
                                                      .equals(name))
                              .findAny()
                              .orElseGet(() -> fail(format("Method %s not found.", name)));
        return result;
    }

    /**
     * Returns all methods of the class which are annotated by the specified annotation.
     */
    protected static ImmutableSet<Method>
    methodsAnnotatedWith(Class<? extends Annotation> annotationCls, Class<?> declaringCls) {
        ImmutableSet<Method> result =
                Stream.of(declaringCls.getDeclaredMethods())
                      .filter(m -> m.getDeclaredAnnotation(annotationCls) != null)
                      .collect(toImmutableSet());
        return result;
    }
}
