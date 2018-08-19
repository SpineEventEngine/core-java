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

package io.spine.server.aggregate.model;

import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.aggregate.given.klasse.EngineAggregate;
import io.spine.server.aggregate.given.klasse.command.EmissionTestStarted;
import io.spine.server.aggregate.given.klasse.command.EmissionTestStopped;
import io.spine.server.aggregate.given.klasse.command.StartEngine;
import io.spine.server.aggregate.given.klasse.command.StopEngine;
import io.spine.server.aggregate.given.klasse.command.TankEmpty;
import io.spine.server.aggregate.given.klasse.rejection.Rejections.CannotStartEmissionTest;
import io.spine.server.aggregate.given.klasse.rejection.Rejections.EngineAlreadyStarted;
import io.spine.server.aggregate.given.klasse.rejection.Rejections.EngineAlreadyStopped;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("AggregateClass should")
class AggregateClassTest {

    private final AggregateClass<?> aggregateClass = asAggregateClass(EngineAggregate.class);

    @Test
    @DisplayName("provide handled command classes")
    void commandClasses() {
        assertThat(aggregateClass.getCommands())
                .containsExactlyElementsIn(CommandClass.setOf(
                        StartEngine.class, StopEngine.class
                ));
    }

    @Test
    @DisplayName("provide event classes on which the aggregate reacts")
    void eventClasses() {
        assertThat(aggregateClass.getEventClasses())
                .containsExactlyElementsIn(EventClass.setOf(
                        TankEmpty.class
                ));
    }

    @Test
    @DisplayName("provide classes of external events on which the aggregate reacts")
    void externalEventClasses() {
        assertThat(aggregateClass.getExternalEventClasses())
                .containsExactlyElementsIn(EventClass.setOf(
                        EmissionTestStarted.class,
                        EmissionTestStopped.class
                ));
    }

    @Test
    @DisplayName("provide rejection classes on which the aggregate reacts")
    void rejectionClasses() {
        assertThat(aggregateClass.getRejectionClasses())
                .containsExactlyElementsIn(RejectionClass.setOf(
                        EngineAlreadyStarted.class,
                        EngineAlreadyStopped.class
                ));
    }

    @Test
    @DisplayName("provide external rejection classes on which the aggregate reacts")
    void externalRejectionClasses() {
        assertThat(aggregateClass.getExternalRejectionClasses())
                .containsExactlyElementsIn(RejectionClass.setOf(
                        CannotStartEmissionTest.class
                ));
    }
}
