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

package io.spine.server.aggregate.model;

import io.spine.server.aggregate.given.klasse.EngineAggregate;
import io.spine.server.aggregate.given.klasse.command.StartEngine;
import io.spine.server.aggregate.given.klasse.command.StopEngine;
import io.spine.server.aggregate.given.klasse.event.EmissionTestStarted;
import io.spine.server.aggregate.given.klasse.event.EmissionTestStopped;
import io.spine.server.aggregate.given.klasse.event.EngineStarted;
import io.spine.server.aggregate.given.klasse.event.EngineStopped;
import io.spine.server.aggregate.given.klasse.event.SettingsAdjusted;
import io.spine.server.aggregate.given.klasse.event.TankEmpty;
import io.spine.server.aggregate.given.klasse.rejection.Rejections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.ServerAssertions.assertCommandsExactly;
import static io.spine.server.ServerAssertions.assertExactly;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;

/**
 * Test obtaining dispatched message classes from {@link AggregateClass}.
 */
@DisplayName("`AggregateClass` should")
class AggregateClassTest {

    private final AggregateClass<?> aggregateClass = asAggregateClass(EngineAggregate.class);

    @Nested
    @DisplayName("provide classes of")
    class MessageClasses {

        @Test
        @DisplayName("commands handled by the aggregate")
        void commandClasses() {
            assertCommandsExactly(aggregateClass.commands(),
                                  StartEngine.class, StopEngine.class);
        }

        @Test
        @DisplayName("events (including rejections) on which the aggregate reacts")
        void eventClasses() {
            assertExactly(aggregateClass.events(),
                          TankEmpty.class,
                          Rejections.EngineAlreadyStopped.class,
                          Rejections.EngineAlreadyStarted.class,
                          EmissionTestStarted.class,
                          EmissionTestStopped.class,
                          Rejections.CannotStartEmissionTest.class);
        }

        @Test
        @DisplayName("external events (including rejections) on which the aggregate reacts")
        void externalEventClasses() {
            assertExactly(aggregateClass.externalEvents(),
                          EmissionTestStarted.class,
                          EmissionTestStopped.class,
                          Rejections.CannotStartEmissionTest.class);
        }

        @Test
        @DisplayName("events imported by the aggregate")
        void importedEvents() {
            assertExactly(aggregateClass.importableEvents(),
                          EngineStopped.class,
                          SettingsAdjusted.class);
        }

        @Test
        @DisplayName("events (including rejections and importable) produced by the aggregate")
        void producedEvents() {
            assertExactly(aggregateClass.outgoingEvents(),
                          EngineStarted.class,
                          EngineStopped.class,
                          SettingsAdjusted.class,
                          Rejections.EngineAlreadyStarted.class,
                          Rejections.EngineAlreadyStopped.class);
        }

        @Test
        @DisplayName("rejections produced by the aggregate")
        void producedRejections() {
            assertExactly(aggregateClass.rejections(),
                        Rejections.EngineAlreadyStarted.class,
                        Rejections.EngineAlreadyStopped.class);
        }
    }
}
