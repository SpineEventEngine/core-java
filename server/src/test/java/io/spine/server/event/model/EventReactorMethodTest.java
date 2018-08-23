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
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
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
import io.spine.server.model.ReactorMethodResult;
import io.spine.server.model.declare.SignatureMismatchException;
import io.spine.test.reflect.ProjectId;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.test.reflect.event.RefProjectStarted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.TestValues.randomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("InnerClassMayBeStatic")
@DisplayName("EventReactorMethod should")
class EventReactorMethodTest {

    private static final EventReactorSignature signature = new EventReactorSignature();

    private static void assertValid(Method rawMethod) {
        assertTrue(signature.matches(rawMethod));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")  // It's fine as it throws an exception.
    private static void assertInvalid(Method method) {
        assertThrows(SignatureMismatchException.class, () -> signature.matches(method));
    }


    @Nested
    @DisplayName("consider reactor method valid with")
    class MethodArguments {

        @Test
        @DisplayName("one event message parameter")
        void oneParam() {
            Method method = new RcOneParam().getMethod();
            assertValid(method);
        }

        @Test
        @DisplayName("event message and context")
        void twoParams() {
            Method method = new RcTwoParams().getMethod();
            assertValid(method);
        }
    }

    @Nested
    @DisplayName("support Message return type")
    class MessageReturn {

        @Test
        @DisplayName("in predicate")
        void predicate() {
            Method method = new RcOneParam().getMethod();
            assertValid(method);
        }
    }

    @Nested
    @DisplayName("support Iterable return type")
    class ReturnValues {

        @Test
        @DisplayName("in predicate")
        void predicate() {
            Method method = new RcIterableReturn().getMethod();
            assertValid(method);
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
            method = createMethod(rawMethod);
        }

        @Test
        @DisplayName("in predicate")
        void inPredicate() {
            assertValid(rawMethod);
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
                    method.invoke(target, envelope(event));

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
                    method.invoke(target, envelope(event));

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
            assertFalse(signature.matches(method));
        }

        @Test
        @DisplayName("wrong annotations provided")
        void wrongAnnotations() {
            Method method = new RcWrongAnnotation().getMethod();
            assertFalse(signature.matches(method));
        }

        @Test
        @DisplayName("it has no parameters")
        void noParameters() {
            Method method = new RcWrongNoParam().getMethod();
            assertInvalid(method);
        }

        @Test
        @DisplayName("the first parameter is not Message")
        void notMessageParam() {
            Method method = new RcWrongFirstParam().getMethod();
            assertInvalid(method);
        }

        @Test
        @DisplayName("the second parameter is not EventContext")
        void notContextParam() {
            Method method = new RcWrongSecondParam().getMethod();
            assertInvalid(method);
        }
    }

    @SuppressWarnings("ConstantConditions") // It's OK for tests
    private static EventReactorMethod createMethod(Method method) {
        return signature.create(method).get();
    }


    private static EventEnvelope envelope(Message eventMessage) {
        Any cmd = pack(eventMessage);
        Event event = Event
                .newBuilder()
                .setMessage(cmd)
                .setContext(EventContext.getDefaultInstance())
                .build();
        EventEnvelope envelope = EventEnvelope.of(event);
        return envelope;
    }
}
