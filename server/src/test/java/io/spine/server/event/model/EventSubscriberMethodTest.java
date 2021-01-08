/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.event.model;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.event.model.given.subscriber.ExternalSubscriber;
import io.spine.server.event.model.given.subscriber.InvalidNoAnnotation;
import io.spine.server.event.model.given.subscriber.TestEventSubscriber;
import io.spine.server.event.model.given.subscriber.ValidOneParam;
import io.spine.server.model.SignalOriginMismatchError;
import io.spine.server.model.given.Given;
import io.spine.server.type.EventEnvelope;
import io.spine.test.reflect.event.RefProjectCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"DuplicateStringLiteralInspection", /* Common test display names. */
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */})
@DisplayName("EventSubscriberMethod should")
class EventSubscriberMethodTest {

    private static final SubscriberSignature signature = new SubscriberSignature();

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
        ValidTwoParams subscriberObject = new ValidTwoParams();
        Optional<SubscriberMethod> createdMethod = signature.classify(subscriberObject.getMethod());
        assertTrue(createdMethod.isPresent());
        SubscriberMethod subscriber = createdMethod.get();
        RefProjectCreated msg = Given.EventMessage.projectCreated();

        Event event = Event
                .newBuilder()
                .setMessage(pack(msg))
                .build();

        EventEnvelope envelope = EventEnvelope.of(event);
        subscriber.invoke(subscriberObject, envelope);

        assertThat(subscriberObject.handledMessages())
                .containsExactly(msg);
    }

    @Nested
    @DisplayName("consider subscriber invalid with")
    class ConsiderSubscriberInvalidWith {

        @Test
        @DisplayName("no annotation")
        void noAnnotation() {
            Method method = new InvalidNoAnnotation().getMethod();
            assertFalse(signature.matches(method));
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
            Optional<SubscriberMethod> created = signature.classify(method);
            assertTrue(created.isPresent());
            SubscriberMethod modelMethod = created.get();
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
            assertThrows(SignalOriginMismatchError.class,
                         () -> modelMethod.invoke(subscriber, envelope));
        }
    }
}
