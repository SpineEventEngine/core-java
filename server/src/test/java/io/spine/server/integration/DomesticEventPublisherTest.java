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

package io.spine.server.integration;

import com.google.common.testing.NullPointerTester;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.type.EventClass;
import io.spine.test.integration.event.ItgProjectCreated;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;

@DisplayName("DomesticEventPublisher should")
class DomesticEventPublisherTest {

    private static final EventClass TARGET_EVENT_CLASS = EventClass.from(ItgProjectCreated.class);

    private IntegrationBroker broker;
    private BoundedContext context;

    @BeforeEach
    void initBus() {
        context = BoundedContextBuilder
                .assumingTests()
                .build();
        broker = context.IntegrationBroker();
    }

    @AfterEach
    void closeContext() throws Exception {
        context.close();
    }

    @Test
    @DisplayName("not accept nulls on construction")
    void notAcceptNulls() {
        new NullPointerTester()
                .setDefault(IntegrationBroker.class, broker)
                .setDefault(EventClass.class, TARGET_EVENT_CLASS)
                .testConstructors(DomesticEventPublisher.class, PACKAGE);
    }

    @Test
    @DisplayName("dispatch only one event type")
    void dispatchSingleEvent() {
        DomesticEventPublisher publisher =
                new DomesticEventPublisher(broker, TARGET_EVENT_CLASS);
        Set<EventClass> classes = publisher.messageClasses();
        assertThat(classes).containsExactly(TARGET_EVENT_CLASS);
    }

    @Test
    @DisplayName("dispatch no external events")
    void dispatchNoExternalEvents() {
        DomesticEventPublisher publisher =
                new DomesticEventPublisher(broker, TARGET_EVENT_CLASS);
        Set<EventClass> classes = publisher.externalEventClasses();
        assertThat(classes).isEmpty();
    }
}
