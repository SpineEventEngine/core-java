/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.Given;
import org.spine3.server.event.Subscribe;
import org.spine3.test.event.ProjectCompleted;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectCreatedSeparateEnrichment;
import org.spine3.test.event.ProjectId;
import org.spine3.test.event.ProjectStarred;
import org.spine3.test.event.ProjectStarted;
import org.spine3.test.event.SeparateEnrichmentForMultipleProjectEvents;
import org.spine3.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import org.spine3.users.UserId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Events.getEnrichment;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.testdata.TestBoundedContextFactory.newBoundedContext;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

public class EventEnricherShould {

    private BoundedContext boundedContext;
    private EventBus eventBus;
    private TestEventSubscriber subscriber;
    private EventEnricher enricher;
    private final Function<ProjectId, String> getProjectName = new Given.Enrichment.GetProjectName();
    private final Function<ProjectId, UserId> getProjectOwnerId = new Given.Enrichment.GetProjectOwnerId();

    @Before
    public void setUp() {
        enricher = Given.Enrichment.newEventEnricher();
        boundedContext = newBoundedContext(enricher);
        eventBus = boundedContext.getEventBus();
        subscriber = new TestEventSubscriber();
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
    public void enrich_event_if_enrichment_definition_is_enclosed_to_event() {
        final ProjectStarted msg = Given.EventMessage.projectStarted();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(msg.getProjectId()),
                     subscriber.projectStartedEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_if_enrichment_definition_is_not_enclosed_to_event_same_package() {
        final ProjectCreated msg = Given.EventMessage.projectCreated();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(msg.getProjectId()),
                     subscriber.projectCreatedSeparateEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_if_enrichment_definition_is_in_another_package() {
        final ProjectCreated msg = Given.EventMessage.projectCreated();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(msg.getProjectId()),
                     subscriber.projectCreatedAnotherPackEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_with_several_fields_by_same_source_id() {
        final ProjectCreated msg = Given.EventMessage.projectCreated();
        final ProjectId projectId = msg.getProjectId();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(projectId), subscriber.projectCreatedEnrichment.getProjectName());
        assertEquals(getProjectOwnerId.apply(projectId), subscriber.projectCreatedEnrichment.getOwnerId());
    }

    @Test
    public void enrich_several_events_with_same_enrichment_message_with_wildcard_by() {
        final ProjectCompleted completed = Given.EventMessage.projectCompleted();
        final ProjectStarred starred = Given.EventMessage.projectStarred();
        final ProjectId completedProjectId = completed.getProjectId();
        final ProjectId starredProjectId = starred.getProjectId();

        eventBus.post(createEvent(completed));
        eventBus.post(createEvent(starred));

        assertEquals(getProjectName.apply(completedProjectId), subscriber.projectCompletedEnrichment.getProjectName());
        assertEquals(getProjectName.apply(starredProjectId), subscriber.projectStarredEnrichment.getProjectName());
    }

    @Test
    public void confirm_that_event_can_be_enriched_if_enrichment_registered() {
        assertTrue(enricher.canBeEnriched(Given.Event.projectStarted()));
    }

    @Test
    public void confirm_that_event_can_not_be_enriched_if_no_such_enrichment_registered() {
        final Event dummyEvent = createEvent(newStringValue(newUuid()));

        assertFalse(enricher.canBeEnriched(dummyEvent));
    }

    @Test
    public void confirm_that_event_can_not_be_enriched_if_enrichment_disabled() {
        final Event event = Events.createEvent(newStringValue(newUuid()), createEventContext(/*doNotEnrich=*/true));

        assertFalse(enricher.canBeEnriched(event));
    }

    @Test
    public void return_false_if_pass_null_to_function_checking_predicate() {
        final boolean result = EventEnricher.SupportsFieldConversion.of(StringValue.class, String.class)
                                                                    .apply(null);
        assertFalse(result);
    }

    private static Event createEvent(Message msg) {
        final Event event = Events.createEvent(msg, createEventContext());
        return event;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static class TestEventSubscriber extends EventSubscriber {

        private ProjectCreated.Enrichment projectCreatedEnrichment;
        private ProjectCreatedSeparateEnrichment projectCreatedSeparateEnrichment;
        private ProjectStarted.Enrichment projectStartedEnrichment;
        private SeparateEnrichmentForMultipleProjectEvents projectCompletedEnrichment;
        private SeparateEnrichmentForMultipleProjectEvents projectStarredEnrichment;

        @SuppressWarnings("InstanceVariableNamingConvention")
        private ProjectCreatedEnrichmentAnotherPackage projectCreatedAnotherPackEnrichment;

        @SuppressWarnings("UnusedParameters")
        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.projectCreatedEnrichment =
                    getEnrichment(ProjectCreated.Enrichment.class, context).get();
            this.projectCreatedSeparateEnrichment =
                    getEnrichment(ProjectCreatedSeparateEnrichment.class, context).get();
            this.projectCreatedAnotherPackEnrichment =
                    getEnrichment(ProjectCreatedEnrichmentAnotherPackage.class, context).get();
        }

        @SuppressWarnings("UnusedParameters")
        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
            this.projectStartedEnrichment = getEnrichment(ProjectStarted.Enrichment.class, context).get();
        }

        @SuppressWarnings("UnusedParameters")
        @Subscribe
        public void on(ProjectCompleted event, EventContext context) {
            this.projectCompletedEnrichment =
                    getEnrichment(SeparateEnrichmentForMultipleProjectEvents.class, context).get();
        }

        @SuppressWarnings("UnusedParameters")
        @Subscribe
        public void on(ProjectStarred event, EventContext context) {
            this.projectStarredEnrichment =
                    getEnrichment(SeparateEnrichmentForMultipleProjectEvents.class, context).get();
        }
    }
}
