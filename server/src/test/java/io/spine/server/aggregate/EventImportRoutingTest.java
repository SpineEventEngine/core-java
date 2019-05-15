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
import io.spine.server.aggregate.given.klasse.event.SettingsAdjusted;
import io.spine.server.type.EventEnvelope;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.blackbox.SingleTenantBlackBoxContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("For event import AggregateRepository should")
class EventImportRoutingTest extends EventImportTestBase {

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
}
