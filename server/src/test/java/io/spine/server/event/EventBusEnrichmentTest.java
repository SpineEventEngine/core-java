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

package io.spine.server.event;

import io.spine.core.EventContext;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.event.given.bus.GivenEvent;
import io.spine.server.event.given.bus.ProjectAggregate;
import io.spine.server.event.given.bus.RememberingSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("EventBus should manage event enrichment")
class EventBusEnrichmentTest {

    private EventBus eventBus;
    private BoundedContext context;
    private RememberingSubscriber subscriber;

    @BeforeEach
    void setUp() {
        EventEnricher enricher = EventEnricher
                .newBuilder()
                .build();
        subscriber = new RememberingSubscriber();
        context = BoundedContextBuilder
                .assumingTests(true)
                .enrichEventsUsing(enricher)
                .addEventDispatcher(subscriber)
                .add(ProjectAggregate.class)
                .build();
        eventBus = context.eventBus();
    }

    @AfterEach
    void closeBoundedContext() throws Exception {
        context.close();
    }

    @Test
    @DisplayName("for event that cannot be enriched")
    void forNonEnrichable() {
        eventBus.post(GivenEvent.projectCreated());

        EventContext eventContext = subscriber.getEventContext();
        assertThat(eventContext.getEnrichment()
                               .getContainer()
                               .getItemsCount())
                .isEqualTo(0);
    }
}
