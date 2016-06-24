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
import com.google.protobuf.StringValue;
import org.junit.After;
import org.junit.Test;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.client.UserUtil;
import org.spine3.server.BoundedContext;
import org.spine3.server.event.EventBus;
import org.spine3.server.event.EventSubscriber;
import org.spine3.server.event.Given;
import org.spine3.server.event.Subscribe;
import org.spine3.test.event.EnrichmentWithInvalidField;
import org.spine3.test.event.ProjectCreated;
import org.spine3.test.event.ProjectCreatedSeparateEnrichment;
import org.spine3.test.event.ProjectId;
import org.spine3.test.event.ProjectStarted;
import org.spine3.testdata.BoundedContextTestStubs;
import org.spine3.users.UserId;

import javax.annotation.Nullable;

import static org.junit.Assert.*;
import static org.spine3.base.Events.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.testdata.TestEventContextFactory.createEventContext;

public class EventEnricherShould {

    private BoundedContext boundedContext;
    private EventBus eventBus;
    private TestEventSubscriber subscriber;
    private final Function<ProjectId, String> getProjectName = new GetProjectName();
    private final Function<ProjectId, UserId> getProjectOwnerId = new GetProjectOwnerId();

    @After
    public void tearDown() throws Exception {
        if (boundedContext != null) {
            boundedContext.close();
        }
    }

    @Test
    public void have_builder() {
        assertNotNull(EventEnricher.newBuilder());
    }

    @Test
    public void enrich_event() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(ProjectStarted.class, ProjectStarted.Enrichment.class)
                .addFieldEnrichment(ProjectId.class, String.class, getProjectName)
                .build();
        setUp(enricher);
        final Event event = Given.Event.projectStarted();

        eventBus.post(event);

        final ProjectStarted msg = getMessage(event);
        assertEquals(getProjectName.apply(msg.getProjectId()), subscriber.projectStartedEnrichment.getProjectName());
    }

    @Test
    public void enrich_event_with_several_fields_by_same_source_id() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(ProjectCreated.class, ProjectCreated.Enrichment.class)
                .addFieldEnrichment(ProjectId.class, String.class, getProjectName)
                .addFieldEnrichment(ProjectId.class, UserId.class, getProjectOwnerId)
                .build();
        setUp(enricher);
        final Event event = Given.Event.projectCreated();

        eventBus.post(event);

        final ProjectCreated msg = getMessage(event);
        assertEquals(getProjectName.apply(msg.getProjectId()), subscriber.projectCreatedEnrichment.getProjectName());
        assertEquals(getProjectOwnerId.apply(msg.getProjectId()), subscriber.projectCreatedEnrichment.getOwnerId());
    }

    @Test
    public void enrich_event_if_enrichment_definition_is_not_enclosed_to_event() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(ProjectCreated.class, ProjectCreatedSeparateEnrichment.class)
                .addFieldEnrichment(ProjectId.class, String.class, getProjectName)
                .build();
        setUp(enricher);
        final Event event = Given.Event.projectCreated();

        eventBus.post(event);

        final ProjectCreated msg = getMessage(event);
        assertEquals(getProjectName.apply(msg.getProjectId()), subscriber.projectCreatedSeparateEnrichment.getProjectName());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_exception_if_enrichment_has_invalid_options() {
        EventEnricher.newBuilder()
                     .addEventEnrichment(ProjectCreated.class, EnrichmentWithInvalidField.class)
                     .addFieldEnrichment(ProjectId.class, String.class, getProjectName)
                     .build();
    }

    @Test
    public void confirm_that_event_can_be_enriched_if_enrichments_registered() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(ProjectStarted.class, ProjectStarted.Enrichment.class)
                .addFieldEnrichment(ProjectId.class, String.class, getProjectName)
                .build();

        assertTrue(enricher.canBeEnriched(Given.Event.projectStarted()));
    }

    @Test
    public void confirm_that_event_can_not_be_enriched_if_no_enrichment_registered() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .build();

        assertFalse(enricher.canBeEnriched(Given.Event.projectStarted()));
    }

    @Test
    public void confirm_that_event_can_not_be_enriched_if_enrichment_disabled() {
        final EventEnricher enricher = EventEnricher
                .newBuilder()
                .addEventEnrichment(ProjectStarted.class, ProjectStarted.Enrichment.class)
                .addFieldEnrichment(ProjectId.class, String.class, getProjectName)
                .build();

        final Event event = createEvent(newStringValue(newUuid()), createEventContext(/*doNotEnrich=*/true));

        assertFalse(enricher.canBeEnriched(event));
    }

    @Test
    public void return_false_if_pass_null_to_function_checking_predicate() {
        final boolean result = EventEnricher.SupportsFieldConversion.of(StringValue.class, String.class)
                                                                    .apply(null);
        assertFalse(result);
    }

    private void setUp(EventEnricher enricher) {
        boundedContext = BoundedContextTestStubs.create(enricher);
        eventBus = boundedContext.getEventBus();
        subscriber = new TestEventSubscriber();
        eventBus.subscribe(subscriber);
    }

    private static class TestEventSubscriber extends EventSubscriber {

        private ProjectCreated.Enrichment projectCreatedEnrichment;
        private ProjectCreatedSeparateEnrichment projectCreatedSeparateEnrichment;
        private ProjectStarted.Enrichment projectStartedEnrichment;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            final Optional<ProjectCreated.Enrichment> enrichment = getEnrichment(ProjectCreated.Enrichment.class, context);
            if (enrichment.isPresent()) {
                this.projectCreatedEnrichment =  enrichment.get();
            }
            final Optional<ProjectCreatedSeparateEnrichment> enrichmentSeparate =
                    getEnrichment(ProjectCreatedSeparateEnrichment.class, context);
            if (enrichmentSeparate.isPresent()) {
                this.projectCreatedSeparateEnrichment = enrichmentSeparate.get();
            }
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
            this.projectStartedEnrichment = getEnrichment(ProjectStarted.Enrichment.class, context).get();
        }
    }

    /* package */ static class GetProjectName implements Function<ProjectId, String> {

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

    private static class GetProjectOwnerId implements Function<ProjectId, UserId> {
        @Nullable
        @Override
        public UserId apply(@Nullable ProjectId id) {
            if (id == null) {
                return null;
            }
            return UserUtil.newUserId("Project owner " + id.getId());
        }
    }
}
