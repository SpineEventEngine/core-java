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

import io.spine.server.aggregate.given.klasse.EngineId;
import io.spine.server.aggregate.given.klasse.EngineRepository;
import io.spine.server.aggregate.given.klasse.event.EngineStopped;
import io.spine.server.aggregate.given.klasse.event.UnsupportedEngineEvent;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.EventSubject;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.client.blackbox.VerifyAcknowledgements.ackedWithErrors;

/**
 * Test support of event import in {@link AggregateRepository}.
 */
@DisplayName("For event import AggregateRepository should")
class EventImportTest extends EventImportTestBase {

    private SingleTenantBlackBoxContext context;
    private EngineRepository repository;

    @BeforeEach
    void setUp() {
        repository = new EngineRepository();
        context = BlackBoxBoundedContext
                .singleTenant()
                .with(repository);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("Obtain importable event classes")
    void importableEventClasses() {
        Set<EventClass> importableEventClasses = repository.importableEvents();
        Set<EventClass> exposedByAggregateClass = repository.aggregateClass()
                                                            .importableEvents();
        assertThat(importableEventClasses)
                .isEqualTo(exposedByAggregateClass);
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
            EventSubject assertEvents =
                    context.importsEvent(event.outerObject())
                           .assertEvents()
                           .withType(EngineStopped.class);

            assertEvents.hasSize(1);
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

        context.importsEvent(unsupported.outerObject())
               .assertThat(ackedWithErrors());
    }
}
