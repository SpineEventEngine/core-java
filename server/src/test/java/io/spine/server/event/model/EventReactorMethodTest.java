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

package io.spine.server.event.model;

import io.spine.server.event.model.given.reactor.RcIterableReturn;
import io.spine.server.event.model.given.reactor.RcOneParam;
import io.spine.server.event.model.given.reactor.RcReturnOptional;
import io.spine.server.event.model.given.reactor.RcTwoParams;
import io.spine.server.event.model.given.reactor.RcWrongAnnotation;
import io.spine.server.event.model.given.reactor.RcWrongNoAnnotation;
import io.spine.server.event.model.given.reactor.RcWrongNoParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("EventReactorMethod should")
class EventReactorMethodTest {

    private static final Predicate<Method> predicate = EventReactorMethod.factory().getPredicate();

    private static void assertValid(Method reactor, boolean isReactor) {
        assertThat(predicate.test(reactor)).isEqualTo(isReactor);
    }

    @Nested
    @DisplayName("consider reactor method valid with")
    class MethodArguments {

        @Test
        @DisplayName("one event message parameter")
        void oneParam() {
            Method method = new RcOneParam().getMethod();
            assertValid(method, true);
        }

        @Test
        @DisplayName("event message and context")
        void twoParams() {
            Method method = new RcTwoParams().getMethod();
            assertValid(method, true);
        }
    }

    @Nested
    @DisplayName("allow return value of type")
    class ReturnValues {

        @Test
        @DisplayName("Message")
        void messageReturn() {
            Method method = new RcOneParam().getMethod();
            assertValid(method, true);
        }

        @Test
        @DisplayName("Iterable")
        void iterableReturn() {
            Method method = new RcIterableReturn().getMethod();
            assertValid(method, true);
        }

        @Test
        @DisplayName("Optional")
        void optionalReturn() {
            Method method = new RcReturnOptional().getMethod();
            assertValid(method, true);
        }
    }

    @Nested
    @DisplayName("consider a method invalid if")
    class NotReactor {

        @Test
        @DisplayName("no annotation is provided")
        void noAnnotation() {
            Method method = new RcWrongNoAnnotation().getMethod();
            assertValid(method, false);
        }

        @Test
        @DisplayName("wrong annotations provided")
        void wrongAnnotations() {
            Method method = new RcWrongAnnotation().getMethod();
            assertValid(method, false);
        }

        @Test
        @DisplayName("it has no parameters")
        void noParameters() {
            Method method = new RcWrongNoParam().getMethod();
            assertValid(method, false);
        }
    }
}
