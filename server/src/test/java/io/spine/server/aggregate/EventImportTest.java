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

package io.spine.server.aggregate;

import io.spine.base.EventMessage;
import io.spine.server.aggregate.given.klasse.EngineId;
import io.spine.server.aggregate.given.klasse.EngineRepository;
import io.spine.server.aggregate.given.klasse.event.EngineStopped;
import io.spine.server.aggregate.given.klasse.event.SettingsAdjusted;
import io.spine.server.aggregate.given.klasse.event.UnsupportedEngineEvent;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.TestEventFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.ackedWithErrors;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;

/**
 * Test support of event import in {@link AggregateRepository}.
 */
@DisplayName("For event import AggregateRepository should")
class EventImportTest {

    private SingleTenantBlackBoxContext boundedContext;
    private EngineRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EngineRepository();
        boundedContext = BlackBoxBoundedContext
                .singleTenant()
                .with(repository);
    }

    @AfterEach
    void tearDown() {
        boundedContext.close();
    }

    @Test
    @DisplayName("Obtain importable event classes")
    void importableEventClasses() {
        Set<EventClass> importableEventClasses = repository.importableEventClasses();
        Set<EventClass> exposedByAggregateClass = repository.aggregateClass()
                                                            .importableEventClasses();
        assertThat(importableEventClasses).isEqualTo(exposedByAggregateClass);
    }

    @Nested
    @DisplayName("route imported events")
    class Routing {

        private final EngineId engineId = engineId("AFB");
        private final SettingsAdjusted eventMessage = SettingsAdjusted
                .newBuilder()
                .setId(engineId)
                .build();

        @Test
        @DisplayName("route imported events by Producer ID by default")
        void routeById() {
            // Create event with the producer ID of the target aggregate.
            EventEnvelope event = createEvent(eventMessage, engineId);

            // Apply routing to the generated event.
            assertRouted(event);
        }

        @Test
        @DisplayName("route imported event by first message field, if configured")
        void routeByFirstMessageField() {
            repository.routeImportByFirstMessageField();

            // Create event with the producer ID, which is NOT the target aggregate ID.
            EventEnvelope event = createEvent(eventMessage, null);

            assertRouted(event);
        }

        /**
         * Asserts that the import routing resulted in {@link #engineId}.
         */
        private void assertRouted(EventEnvelope event) {
            Set<EngineId> targets =
                    repository.eventImportRouting()
                              .apply(event.message(), event.context());

            assertThat(targets).hasSize(1);
            assertThat(targets).containsExactly(engineId);
        }
    }

    @Nested
    @DisplayName("dispatch imported events")
    class Dispatching {

        private final EngineId engineId = engineId("AEL");
        private final EngineStopped eventMessage = EngineStopped
                .newBuilder()
                .setId(engineId)
                .build();


        @Test
        @DisplayName("by producer ID")
        void producerId() {
            // Create event with producer ID, which is the target aggregate ID.
            EventEnvelope event = createEvent(eventMessage, engineId);

            assertImports(event);
        }

        @Test
        @DisplayName("by first message field")
        void firstMessageField() {
            repository.routeImportByFirstMessageField();

            // Create event with producer ID, which is NOT the target aggregate ID.
            EventEnvelope event = createEvent(eventMessage, null);

            assertImports(event);
        }

        private void assertImports(EventEnvelope event) {
            boundedContext.importsEvent(event.outerObject())
                          .assertThat(emittedEvent(EngineStopped.class, once()));
        }
    }

    @Test
    @DisplayName("fail with exception when importing unsupported event")
    void importUnsupported() {
        EngineId id = engineId("AGR");
        UnsupportedEngineEvent eventMessage = UnsupportedEngineEvent
                .newBuilder()
                .setId(id)
                .build();
        EventEnvelope unsupported = createEvent(eventMessage, id);

        boundedContext.importsEvent(unsupported.outerObject())
                      .assertThat(ackedWithErrors());
    }

    /**
     * Creates a new event for the passed message.
     *
     * @param eventMessage
     *        the event message
     * @param producerId
     *        the producer for the event. If {@code null}, a test class name will be the ID
     *        of the producer.
     * @return generated event wrapped into the envelope
     */
    private EventEnvelope createEvent(EventMessage eventMessage, @Nullable EngineId producerId) {
        TestEventFactory eventFactory = producerId == null
                                        ? TestEventFactory.newInstance(getClass())
                                        : TestEventFactory.newInstance(producerId, getClass());
        EventEnvelope result = EventEnvelope.of(eventFactory.createEvent(eventMessage));
        return result;
    }

    private static EngineId engineId(String value) {
        return EngineId
                .newBuilder()
                .setValue(value)
                .build();
    }
}
