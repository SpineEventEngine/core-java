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

package io.spine.server.event;

import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.given.bus.ProjectAggregate;
import io.spine.server.stand.Stand;
import io.spine.server.type.EventClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("`EventBus` should")
class EventBusStandIntegrationTest {

    @Test
    @DisplayName("do not have `Stand` as the dispatcher if no repositories registered with the context")
    void notRegistered() {
        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .build();

        EventBus eventBus = context.eventBus();
        Stand stand = context.stand();

        eventClasses().forEach(eventClass ->
                assertThat(eventBus.dispatchersOf(eventClass))
                        .doesNotContain(stand)
        );
    }

    @Test
    @DisplayName("have `Stand` as the dispatcher of events of the registered repository")
    void reRegisterStand() {
        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .add(ProjectAggregate.class)
                .build();

        EventBus eventBus = context.eventBus();
        Stand stand = context.stand();

        eventClasses().forEach(eventClass ->
             assertThat(eventBus.dispatchersOf(eventClass))
                     .contains(stand)
        );
    }

    private static Set<EventClass> eventClasses() {
        return ProjectAggregate.outgoingEvents();
    }
}
