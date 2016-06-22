/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.Given;
import org.spine3.server.event.Subscribe;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectId;
import org.spine3.testdata.BoundedContextTestStubs;

import javax.annotation.Nullable;

import static org.junit.Assert.*;
import static org.spine3.base.Events.getEnrichment;
import static org.spine3.base.Events.getMessage;

public class EventEnricherShould {

    private BoundedContext boundedContext;
    private EventBus eventBus;
    private TestEventSubscriber subscriber;
    private final Function<ProjectId, String> projectNameLookup = new ProjectNameLookup();

    @Before
    public void setUp() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(ProjectCreated.class, ProjectCreated.Enrichment.class)
                .addFieldEnrichment(ProjectId.class, String.class, projectNameLookup)
                .build();
        this.boundedContext = BoundedContextTestStubs.create(enricher);
        this.eventBus = boundedContext.getEventBus();
        this.subscriber = new TestEventSubscriber();
        eventBus.subscribe(subscriber);
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    public void have_builder() {
        assertNotNull(EventEnricher.newBuilder());
    }

    @Test
    public void enrich_event() {
        final Event event = Given.Event.projectCreated();

        eventBus.post(event);

        assertTrue(subscriber.enrichment.isPresent());
        final ProjectCreated.Enrichment enrichment = subscriber.enrichment.get();
        final ProjectCreated msg = getMessage(event);
        assertEquals(projectNameLookup.apply(msg.getProjectId()), enrichment.getProjectName());
    }

    private static class TestEventSubscriber extends EventSubscriber {

        private Optional<ProjectCreated.Enrichment> enrichment;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.enrichment = getEnrichment(ProjectCreated.Enrichment.class, context);
        }
    }

    private static class ProjectNameLookup implements Function<ProjectId, String> {

        @Nullable
        @Override
        public String apply(@Nullable ProjectId id) {
            if (id == null) {
                return null;
            }
            final String name = "Project " + id.getId();
            return name;
        }
    }
}
