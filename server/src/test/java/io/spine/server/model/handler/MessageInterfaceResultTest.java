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

package io.spine.server.model.handler;

import io.spine.server.DefaultRepository;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.model.handler.given.RoverBot;
import io.spine.server.model.handler.given.command.Start;
import io.spine.server.model.handler.given.event.MovedEast;
import io.spine.server.model.handler.given.event.MovedNorth;
import io.spine.server.model.handler.given.event.MovedSouth;
import io.spine.server.model.handler.given.event.MovedWest;
import io.spine.server.type.EventClass;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.aggregate.model.AggregateClass.asAggregateClass;
import static io.spine.testing.TestValues.random;

@DisplayName("An aggregate class with a handler returning events via common interface should")
class MessageInterfaceResultTest {

    private final AggregateClass<?> aggregateClass = asAggregateClass(RoverBot.class);
    private BlackBoxBoundedContext<?> context;

    @BeforeEach
    void createContext() {
        context = BlackBoxBoundedContext.singleTenant()
                                         .with(DefaultRepository.of(RoverBot.class));
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    @DisplayName("provide state events")
    void generatedEvents() {
        assertThat(aggregateClass.stateEvents())
                .containsExactlyElementsIn(EventClass.setOf(
                        MovedNorth.class, MovedEast.class, MovedSouth.class, MovedWest.class
                ));
    }

    @Test
    @DisplayName("dispatch the command")
    void dispatching() {
        int numberOfMoves = random(1, 5);
        Start command = Start
                .newBuilder()
                .setBotId(random(7))
                .setNumberOfMoves(numberOfMoves)
                .build();
        context.receivesCommand(command);
        context.assertEvents()
               .hasSize(numberOfMoves);
    }
}
