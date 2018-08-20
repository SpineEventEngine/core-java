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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.ARejectionSubscriber;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.ExternalSubscriber;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidNoAnnotation;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidNoParams;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidNotVoid;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidOneNotMsgParam;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidTooManyParams;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidTwoParamsFirstInvalid;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.InvalidTwoParamsSecondInvalid;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.TestEventSubscriber;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.ValidButPrivate;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.ValidOneParam;
import io.spine.server.event.given.EventSubscriberMethodTestEnv.ValidTwoParams;
import io.spine.server.model.given.Given;
import io.spine.test.reflect.event.RefProjectCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", /* Common test display names. */
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */})
@DisplayName("EventSubscriberMethod should")
class EventSubscriberMethodTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() throws NoSuchMethodException {
        Method defaultMethod =
                ValidOneParam.class.getDeclaredMethod("handle", RefProjectCreated.class);
        new NullPointerTester()
                .setDefault(Any.class, Any.getDefaultInstance())
                .setDefault(EventContext.class, EventContext.getDefaultInstance())
                .setDefault(Method.class, defaultMethod)
                .testAllPublicStaticMethods(EventSubscriberMethod.class);
    }

    @Test
    @DisplayName("invoke subscriber method")
    void invokeSubscriberMethod() {
        ValidTwoParams subscriberObject;
        subscriberObject = spy(new ValidTwoParams());
        Optional<EventSubscriberMethod> createdMethod =
                new EventSubscriberSignature().create(subscriberObject.getMethod());
        assertTrue(createdMethod.isPresent());
        EventSubscriberMethod subscriber = createdMethod.get();
        RefProjectCreated msg = Given.EventMessage.projectCreated();

        Event event = Event
                .newBuilder()
                .setMessage(pack(msg))
                .build();

        EventEnvelope envelope = EventEnvelope.of(event);
        subscriber.invoke(subscriberObject, envelope);

        verify(subscriberObject, times(1))
                .handle(msg, EventContext.getDefaultInstance());
    }

    @Nested
    @DisplayName("consider subscriber valid with")
    class ConsiderSubscriberValidWith {

        @Test
        @DisplayName("one Message parameter")
        void oneMessageParam() {
            Method subscriber = new ValidOneParam().getMethod();

            assertIsEventSubscriber(subscriber, true);
        }

        @Test
        @DisplayName("Message and Context parameters")
        void messageAndContextParams() {
            Method subscriber = new ValidTwoParams().getMethod();

            assertIsEventSubscriber(subscriber, true);
        }

        @Test
        @DisplayName("non-public access")
        void nonPublicAccess() {
            Method method = new ValidButPrivate().getMethod();

            assertIsEventSubscriber(method, true);
        }

        @Test
        @DisplayName("rejection event type")
        void rejectionClassType() {
            Method rejectionSubscriber = new ARejectionSubscriber().getMethod();

            assertIsEventSubscriber(rejectionSubscriber, true);
        }
    }

    @Nested
    @DisplayName("consider subscriber invalid with")
    class ConsiderSubscriberInvalidWith {

        @Test
        @DisplayName("no annotation")
        void noAnnotation() {
            Method subscriber = new InvalidNoAnnotation().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("no params")
        void noParams() {
            Method subscriber = new InvalidNoParams().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("too many params")
        void tooManyParams() {
            Method subscriber = new InvalidTooManyParams().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("one invalid param")
        void oneInvalidParam() {
            Method subscriber = new InvalidOneNotMsgParam().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("first non-Message param")
        void firstNonMessageParam() {
            Method subscriber = new InvalidTwoParamsFirstInvalid().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("second non-Context param")
        void secondNonContextParam() {
            Method subscriber = new InvalidTwoParamsSecondInvalid().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }

        @Test
        @DisplayName("non-void return type")
        void nonVoidReturnType() {
            Method subscriber = new InvalidNotVoid().getMethod();

            assertIsEventSubscriber(subscriber, false);
        }
    }

    @Nested
    @DisplayName("reject event if")
    class ExternalMatch {

        @Test
        @DisplayName("it is external")
        void external() {
            TestEventSubscriber subscriber = new ValidOneParam();
            check(subscriber, true);
        }

        @Test
        @DisplayName("it is not external")
        void notExternal() {
            TestEventSubscriber subscriber = new ExternalSubscriber();
            check(subscriber, false);
        }

        private void check(TestEventSubscriber subscriber, boolean external) {
            Method method = subscriber.getMethod();
            Optional<EventSubscriberMethod> created = new EventSubscriberSignature().create(method);
            assertTrue(created.isPresent());
            EventSubscriberMethod modelMethod = created.get();
            EventContext context = EventContext
                    .newBuilder()
                    .setExternal(external)
                    .build();
            Event event = Event
                    .newBuilder()
                    .setMessage(pack(RefProjectCreated.getDefaultInstance()))
                    .setContext(context)
                    .build();
            EventEnvelope envelope = EventEnvelope.of(event);
            assertThrows(IllegalArgumentException.class,
                         () -> modelMethod.invoke(subscriber, envelope));
        }
    }

    private static void assertIsEventSubscriber(Method subscriber, boolean isSubscriber) {
        assertEquals(isSubscriber, new EventSubscriberSignature().match(subscriber)
                                                                 .isEmpty());
    }
}
