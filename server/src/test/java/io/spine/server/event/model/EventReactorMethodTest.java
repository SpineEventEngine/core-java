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

import com.google.common.truth.IterableSubject;
import io.spine.core.EventContext;
import io.spine.server.event.EventReactor;
import io.spine.server.event.model.given.reactor.RcIterableReturn;
import io.spine.server.event.model.given.reactor.RcOneParam;
import io.spine.server.event.model.given.reactor.RcReturnOptional;
import io.spine.server.event.model.given.reactor.RcTwoParams;
import io.spine.server.event.model.given.reactor.RcWrongAnnotation;
import io.spine.server.event.model.given.reactor.RcWrongFirstParam;
import io.spine.server.event.model.given.reactor.RcWrongNoAnnotation;
import io.spine.server.event.model.given.reactor.RcWrongNoParam;
import io.spine.server.event.model.given.reactor.RcWrongSecondParam;
import io.spine.server.event.model.given.reactor.TestEventReactor;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.ReactorMethodResult;
import io.spine.test.reflect.ProjectId;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.test.reflect.event.RefProjectStarted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.TestValues.randomString;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("EventReactorMethod should")
class EventReactorMethodTest {

    private static final MethodFactory<EventReactorMethod> factory = EventReactorMethod.factory();
    private static final Predicate<Method> predicate = factory.getPredicate();

    private static void assertValid(Method rawMethod, boolean isReactor) {
        assertThat(predicate.test(rawMethod)).isEqualTo(isReactor);
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
    @DisplayName("support Message return type")
    class MessageReturn {

        @Test
        @DisplayName("in predicate")
        void predicate() {
            Method method = new RcOneParam().getMethod();
            assertValid(method, true);
        }
    }

    @Nested
    @DisplayName("support Iterable return type")
    class ReturnValues {

        @Test
        @DisplayName("in predicate")
        void predicate() {
            Method method = new RcIterableReturn().getMethod();
            assertValid(method, true);
        }
    }

    @Nested
    @DisplayName("support Optional return type")
    class OptionalReturn {

        private EventReactor target;
        private Method rawMethod;
        private EventReactorMethod method;

        @BeforeEach
        void setUp() {
            target = new RcReturnOptional();
            rawMethod = ((TestEventReactor) target).getMethod();
            method = factory.create(rawMethod);
        }

        @Test
        @DisplayName("in predicate")
        void inPredicate() {
            assertValid(rawMethod, true);
        }

        @Test
        @DisplayName("in factory")
        void inFactory() {
            assertThat(method).isNotNull();
        }

        @Test
        @DisplayName("when returning value")
        void returnValue() {
            ProjectId id = ProjectId
                    .newBuilder()
                    .setId(randomString())
                    .build();
            RefProjectCreated event = RefProjectCreated
                    .newBuilder()
                    .setProjectId(id)
                    .build();

            ReactorMethodResult result =
                    method.invoke(target, event, EventContext.getDefaultInstance());

            IterableSubject assertThat = assertThat(result.asMessages());
            assertThat.hasSize(1);
            assertThat.containsExactly(
                    RefProjectStarted.newBuilder()
                                     .setProjectId(id)
                                     .build()
            );
        }

        @Test
        @DisplayName("when returning Optional.empty()")
        void returnEmpty() {
            // Passing event without projectId should return `Optional.empty()`.
            RefProjectCreated event = RefProjectCreated
                    .newBuilder()
                    .build();

            ReactorMethodResult result =
                    method.invoke(target, event, EventContext.getDefaultInstance());

            assertThat(result.asMessages()).isEmpty();
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

        @Test
        @DisplayName("the first parameter is not Message")
        void notMessageParam() {
            Method method = new RcWrongFirstParam().getMethod();
            assertValid(method, false);
        }

        @Test
        @DisplayName("the second parameter is not EventContext")
        void notContextParam() {
            Method method = new RcWrongSecondParam().getMethod();
            assertValid(method, false);
        }
    }
}
