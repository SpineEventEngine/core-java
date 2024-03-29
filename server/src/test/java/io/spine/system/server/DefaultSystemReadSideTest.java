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

package io.spine.system.server;

import com.google.common.testing.NullPointerTester;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.base.EventMessage;
import io.spine.core.Subscribe;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.EventBus;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.base.Identifier.newUuid;
import static io.spine.system.server.SystemBoundedContexts.systemOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`SystemReadSide` should")
class DefaultSystemReadSideTest {

    private static final TestEventFactory events =
            TestEventFactory.newInstance(DefaultSystemReadSideTest.class);

    private BoundedContext domainContext;
    private SystemReadSide systemReadSide;

    @BeforeEach
    void setUp() {
        domainContext = BoundedContextBuilder.assumingTests().build();
        systemReadSide = domainContext.systemClient().readSide();
    }

    @AfterEach
    void tearDown() throws Exception {
        domainContext.close();
    }

    @Test
    @DisplayName("not allow `null`s on construction")
    void notAllowNullsOnConstruction() {
        new NullPointerTester()
                .setDefault(EventBus.class, EventBus.newBuilder().build())
                .testStaticMethods(DefaultSystemReadSide.class, PACKAGE);
    }

    @Test
    @DisplayName("not allow `null`s")
    void notAllowNulls() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(systemReadSide);
    }

    @Nested
    class RegisterSystemEventDispatchers {

        @Test
        @DisplayName("pass system events to the domain")
        void passSystemEvents() {
            var subscriber = new ProjectCreatedSubscriber();
            systemReadSide.register(subscriber);

            var systemEvent = postSystemEvent();
            var receivedEvent = subscriber.lastEvent();
            assertTrue(receivedEvent.isPresent());
            var actualEvent = receivedEvent.get();
            assertEquals(systemEvent, actualEvent);
        }

        @Test
        @DisplayName("unregister dispatchers")
        void unregisterDispatchers() {
            var subscriber = new ProjectCreatedSubscriber();
            systemReadSide.register(subscriber);
            systemReadSide.unregister(subscriber);

            postSystemEvent();
            var receivedEvent = subscriber.lastEvent();
            assertFalse(receivedEvent.isPresent());
        }

        @CanIgnoreReturnValue
        private EventMessage postSystemEvent() {
            var systemContext = systemOf(domainContext);
            EventMessage systemEvent = SMProjectCreated.newBuilder()
                    .setUuid(newUuid())
                    .setName("System Bus test project")
                    .build();
            var event = events.createEvent(systemEvent);
            systemContext.eventBus().post(event);
            return systemEvent;
        }
    }

    /**
     * A subscriber for {@link SMProjectCreated} events.
     *
     * <p>Memoizes the last received event and reports it on {@link #lastEvent()} calls.
     */
    private static final class ProjectCreatedSubscriber extends AbstractEventSubscriber {

        private EventMessage lastEvent;

        @Subscribe
        void on(SMProjectCreated event) {
            lastEvent = event;
        }

        private Optional<EventMessage> lastEvent() {
            return Optional.ofNullable(lastEvent);
        }
    }
}
