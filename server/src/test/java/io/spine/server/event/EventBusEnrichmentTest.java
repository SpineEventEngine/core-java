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

import io.spine.core.EventContext;
import io.spine.server.BoundedContext;
import io.spine.server.event.given.bus.GivenEvent;
import io.spine.server.event.given.bus.ProjectRepository;
import io.spine.server.event.given.bus.RememberingSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.event.given.bus.EventBusTestEnv.eventBusBuilder;

@DisplayName("EventBus should manage event enrichment")
class EventBusEnrichmentTest {

    private EventBus eventBus;
    private BoundedContext bc;

    private void setUp(EventEnricher enricher) {
        EventBus.Builder eventBusBuilder = eventBusBuilder(enricher);
        bc = BoundedContext
                .newBuilder()
                .setEventBus(eventBusBuilder)
                .setMultitenant(true)
                .build();

        ProjectRepository projectRepository = new ProjectRepository();
        bc.register(projectRepository);
        eventBus = bc.eventBus();
    }

    @AfterEach
    void closeBoundedContext() throws Exception {
        bc.close();
    }

    @Test
    @DisplayName("for event that cannot be enriched")
    void forNonEnrichable() {
        EventEnricher enricher = EventEnricher
                .newBuilder()
                .build();

        setUp(enricher);
        RememberingSubscriber subscriber = new RememberingSubscriber();
        eventBus.register(subscriber);
        eventBus.post(GivenEvent.projectCreated());

        EventContext eventContext = subscriber.getEventContext();
        assertThat(eventContext.getEnrichment()
                               .getContainer()
                               .getItemsCount()).isEqualTo(0);
    }
}
