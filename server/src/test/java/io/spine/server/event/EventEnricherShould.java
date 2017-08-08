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

package io.spine.server.event;

import com.google.common.base.Function;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.event.given.EventEnricherTestEnv.GivenEvent;
import io.spine.server.event.given.EventEnricherTestEnv.GivenEventMessage;
import io.spine.test.event.ProjectCompleted;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectCreatedSeparateEnrichment;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarred;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.SeparateEnrichmentForMultipleProjectEvents;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static io.spine.core.Enrichments.getEnrichment;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.command.TestEventFactory.newInstance;
import static io.spine.server.event.given.EventEnricherTestEnv.Enrichment.GetProjectName;
import static io.spine.server.event.given.EventEnricherTestEnv.Enrichment.GetProjectOwnerId;
import static io.spine.server.event.given.EventEnricherTestEnv.Enrichment.newEventEnricher;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEvent.projectStarted;
import static io.spine.testdata.TestBoundedContextFactory.MultiTenant.newBoundedContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventEnricherShould {

    private BoundedContext boundedContext;
    private EventBus eventBus;
    private TestEventSubscriber subscriber;
    private EventEnricher enricher;
    private final Function<ProjectId, String> getProjectName = new GetProjectName();
    private final Function<ProjectId, UserId> getProjectOwnerId = new GetProjectOwnerId();

    private static Event createEvent(Message msg) {
        final TestEventFactory eventFactory = newInstance(EventEnricherShould.class);
        final Event event = eventFactory.createEvent(msg);
        return event;
    }

    @Before
    public void setUp() {
        enricher = newEventEnricher();
        boundedContext = newBoundedContext(enricher);
        eventBus = boundedContext.getEventBus();
        subscriber = new TestEventSubscriber();
        eventBus.register(subscriber);
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
        final ProjectStarted msg = GivenEventMessage.projectStarted();
        final Event event = createEvent(msg);

        eventBus.post(event);

        assertEquals(getProjectName.apply(msg.getProjectId()),
                     subscriber.projectStartedEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_if_enrichment_definition_is_not_enclosed_to_event_same_package() {
        final ProjectCreated msg = GivenEventMessage.projectCreated();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(msg.getProjectId()),
                     subscriber.projectCreatedSeparateEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_if_enrichment_definition_is_in_another_package() {
        final ProjectCreated msg = GivenEventMessage.projectCreated();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(msg.getProjectId()),
                     subscriber.projectCreatedAnotherPackEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_with_several_fields_by_same_source_id() {
        final ProjectCreated msg = GivenEventMessage.projectCreated();
        final ProjectId projectId = msg.getProjectId();

        eventBus.post(createEvent(msg));

        assertEquals(getProjectName.apply(projectId),
                     subscriber.projectCreatedEnrichment.getProjectName());
        assertEquals(getProjectOwnerId.apply(projectId),
                     subscriber.projectCreatedEnrichment.getOwnerId());
    }

    @Test
    public void enrich_several_events_with_same_enrichment_message_with_wildcard_by() {
        final ProjectCompleted completed = GivenEventMessage.projectCompleted();
        final ProjectStarred starred = GivenEventMessage.projectStarred();
        final ProjectId completedProjectId = completed.getProjectId();
        final ProjectId starredProjectId = starred.getProjectId();

        eventBus.post(createEvent(completed));
        eventBus.post(createEvent(starred));

        assertEquals(getProjectName.apply(completedProjectId),
                     subscriber.projectCompletedEnrichment.getProjectName());
        assertEquals(getProjectName.apply(starredProjectId),
                     subscriber.projectStarredEnrichment.getProjectName());
    }

    @Test
    public void enrich_several_events_bound_by_fields() {
        final EventEnvelope permissionGranted = EventEnvelope.of(GivenEvent.permissionGranted());
        final EventEnvelope permissionRevoked = EventEnvelope.of(GivenEvent.permissionRevoked());
        final EventEnvelope sharingRequestApproved = EventEnvelope.of(GivenEvent.sharingRequestApproved());

        assertTrue(enricher.canBeEnriched(permissionGranted));
        assertTrue(enricher.canBeEnriched(permissionRevoked));
        assertTrue(enricher.canBeEnriched(sharingRequestApproved));
    }

    @Test
    public void confirm_that_event_can_be_enriched_if_enrichment_registered() {
        assertTrue(enricher.canBeEnriched(EventEnvelope.of(projectStarted())));
    }

    @Test
    public void confirm_that_event_can_not_be_enriched_if_no_such_enrichment_registered() {
        final EventEnvelope dummyEvent = EventEnvelope.of(createEvent(toMessage(newUuid())));

        assertFalse(enricher.canBeEnriched(dummyEvent));
    }

    @Test
    public void confirm_that_event_can_not_be_enriched_if_enrichment_disabled() {
        final Event event = createEvent(toMessage(newUuid()));
        final EventEnvelope notEnrichableEvent = EventEnvelope.of(
                event.toBuilder()
                     .setContext(event.getContext()
                                      .toBuilder()
                                      .setEnrichment(event.getContext()
                                                          .getEnrichment()
                                                          .toBuilder()
                                                          .setDoNotEnrich(true)))
                     .build()
        );

        assertFalse(enricher.canBeEnriched(notEnrichableEvent));
    }

    /**
     * Event subscriber that remembers enrichments.
     *
     * <p>This class is a part of assert checking, and as such it is not placed under the test
     * {@linkplain io.spine.server.event.given.EventEnricherTestEnv environment class} .
     */
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "InstanceVariableNamingConvention",
                       "ConstantConditions"})
    private static class TestEventSubscriber extends EventSubscriber {

        private ProjectCreated.Enrichment projectCreatedEnrichment;
        private ProjectCreatedSeparateEnrichment projectCreatedSeparateEnrichment;
        private ProjectStarted.Enrichment projectStartedEnrichment;
        private SeparateEnrichmentForMultipleProjectEvents projectCompletedEnrichment;
        private SeparateEnrichmentForMultipleProjectEvents projectStarredEnrichment;
        private ProjectCreatedEnrichmentAnotherPackage projectCreatedAnotherPackEnrichment;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.projectCreatedEnrichment =
                    getEnrichment(ProjectCreated.Enrichment.class, context).get();
            this.projectCreatedSeparateEnrichment =
                    getEnrichment(ProjectCreatedSeparateEnrichment.class, context).get();
            this.projectCreatedAnotherPackEnrichment =
                    getEnrichment(ProjectCreatedEnrichmentAnotherPackage.class, context).get();
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
            this.projectStartedEnrichment =
                    getEnrichment(ProjectStarted.Enrichment.class, context).get();
        }

        @Subscribe
        public void on(ProjectCompleted event, EventContext context) {
            this.projectCompletedEnrichment =
                    getEnrichment(SeparateEnrichmentForMultipleProjectEvents.class, context).get();
        }

        @Subscribe
        public void on(ProjectStarred event, EventContext context) {
            this.projectStarredEnrichment =
                    getEnrichment(SeparateEnrichmentForMultipleProjectEvents.class, context).get();
        }
    }
}
