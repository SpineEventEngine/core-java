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

package io.spine.server.event.enrich;

import com.google.common.truth.BooleanSubject;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.server.event.EventBus;
import io.spine.server.event.given.EventEnricherTestEnv.GivenEvent;
import io.spine.server.event.given.EventEnricherTestEnv.GivenEventMessage;
import io.spine.test.event.ProjectCompleted;
import io.spine.test.event.ProjectCreated;
import io.spine.test.event.ProjectCreatedEnrichment;
import io.spine.test.event.ProjectCreatedSeparateEnrichment;
import io.spine.test.event.ProjectId;
import io.spine.test.event.ProjectStarred;
import io.spine.test.event.ProjectStarted;
import io.spine.test.event.ProjectStartedEnrichment;
import io.spine.test.event.SeparateEnrichmentForMultipleProjectEvents;
import io.spine.test.event.enrichment.ProjectCreatedEnrichmentAnotherPackage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.core.Enrichments.getEnrichment;
import static io.spine.core.given.GivenEvent.arbitrary;
import static io.spine.server.event.given.EventEnricherTestEnv.Enrichment.GetProjectName;
import static io.spine.server.event.given.EventEnricherTestEnv.Enrichment.GetProjectOwnerId;
import static io.spine.server.event.given.EventEnricherTestEnv.Enrichment.newEventEnricher;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEvent.permissionGranted;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEvent.permissionRevoked;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEvent.projectStarted;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEventMessage.projectCompleted;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEventMessage.projectCreated;
import static io.spine.server.event.given.EventEnricherTestEnv.GivenEventMessage.projectStarred;
import static io.spine.server.event.given.EventEnricherTestEnv.createEvent;
import static io.spine.testdata.TestBoundedContextFactory.MultiTenant.newBoundedContext;
import static io.spine.util.Exceptions.newIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Enricher should")
public class EnricherTest {

    private BoundedContext boundedContext;
    private EventBus eventBus;
    private TestEventSubscriber subscriber;
    private Enricher enricher;
    private final BiFunction<ProjectId, EventContext, String> getProjectName = new GetProjectName();
    private final BiFunction<ProjectId, EventContext, UserId> getProjectOwnerId = new GetProjectOwnerId();

    @BeforeEach
    void setUp() {
        enricher = newEventEnricher();
        boundedContext = newBoundedContext(enricher);
        eventBus = boundedContext.getEventBus();
        subscriber = new TestEventSubscriber();
        eventBus.register(subscriber);
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    @DisplayName("have builder")
    void haveBuilder() {
        assertNotNull(Enricher.newBuilder());
    }

    @Nested
    @DisplayName("enrich event with enrichment whose definition")
    class EnrichEventWithEnrichment {

        @Test
        @DisplayName("is enclosed to event")
        void enclosedToEvent() {
            ProjectStarted msg = GivenEventMessage.projectStarted();
            Event event = createEvent(msg);

            eventBus.post(event);

            assertEquals(getProjectName.apply(msg.getProjectId(), event.getContext()),
                         subscriber.projectStartedEnrichment.getProjectName());
        }

        @Test
        @DisplayName("is not enclosed to event and is located in same package")
        void fromSamePackage() {
            ProjectCreated msg = projectCreated();

            Event event = createEvent(msg);
            eventBus.post(event);

            assertEquals(getProjectName.apply(msg.getProjectId(), event.getContext()),
                         subscriber.projectCreatedSeparateEnrichment.getProjectName());
        }

        @Test
        @DisplayName("is located in another package")
        void fromAnotherPackage() {
            ProjectCreated msg = projectCreated();

            Event event = createEvent(msg);
            eventBus.post(event);

            assertEquals(getProjectName.apply(msg.getProjectId(), event.getContext()),
                         subscriber.projectCreatedAnotherPackEnrichment.getProjectName());
        }
    }

    @Test
    @DisplayName("enrich event with several fields by same source ID")
    void enrichSeveralFieldsProperly() {
        ProjectCreated msg = projectCreated();
        ProjectId projectId = msg.getProjectId();

        Event event = createEvent(msg);
        eventBus.post(event);

        assertEquals(getProjectName.apply(projectId, event.getContext()),
                     subscriber.projectCreatedEnrichment.getProjectName());
        assertEquals(getProjectOwnerId.apply(projectId, event.getContext()),
                     subscriber.projectCreatedEnrichment.getOwnerId());
    }

    @Nested
    @DisplayName("enrich several events")
    class EnrichSeveralEvents {

        @Test
        @DisplayName("with same enrichment message having wildcard")
        void byMessageWithWildcard() {
            ProjectCompleted completed = projectCompleted();
            ProjectStarred starred = projectStarred();
            ProjectId completedProjectId = completed.getProjectId();
            ProjectId starredProjectId = starred.getProjectId();

            Event completedEvent = createEvent(completed);
            eventBus.post(completedEvent);
            Event starredEvent = createEvent(starred);
            eventBus.post(starredEvent);

            assertEquals(getProjectName.apply(completedProjectId, completedEvent.getContext()),
                         subscriber.projectCompletedEnrichment.getProjectName());
            assertEquals(getProjectName.apply(starredProjectId, starredEvent.getContext()),
                         subscriber.projectStarredEnrichment.getProjectName());
        }

        @Test
        @DisplayName("bound by fields")
        void boundByFields() {
            EventEnvelope permissionGranted = EventEnvelope.of(permissionGranted());
            EventEnvelope permissionRevoked = EventEnvelope.of(permissionRevoked());
            EventEnvelope sharingRequestApproved =
                    EventEnvelope.of(GivenEvent.sharingRequestApproved());

            assertCanBeEnriched(permissionGranted);
            assertCanBeEnriched(permissionRevoked);
            assertCanBeEnriched(sharingRequestApproved);
        }
    }

    void assertCanBeEnriched(EventEnvelope e) {
        assertThatCanBeEnriched(e).isTrue();
    }

    void assertCannotBeEnriched(EventEnvelope e) {
        assertThatCanBeEnriched(e).isFalse();
    }

    private BooleanSubject assertThatCanBeEnriched(EventEnvelope e) {
        return assertThat(enricher.canBeEnriched(e));
    }

    @Test
    @DisplayName("state event can be enriched if its enrichment is registered")
    void stateEventEnrichable() {
        assertCanBeEnriched(EventEnvelope.of(projectStarted()));
    }

    @Nested
    @DisplayName("state event cannot be enriched")
    class StateEventNonEnrichable {

        @Test
        @DisplayName("if there is no enrichment registered for it")
        void withoutEnrichment() {
            EventEnvelope dummyEvent = EventEnvelope.of(arbitrary());

            assertCannotBeEnriched(dummyEvent);
        }

        @Test
        @DisplayName("if its enrichment is disabled")
        void withDisabledEnrichment() {
            Event event = createEvent(projectStarred());
            Enrichment.Builder enrichment = event.getContext()
                                                 .getEnrichment()
                                                 .toBuilder()
                                                 .setDoNotEnrich(true);
            EventContext.Builder context = event.getContext()
                                                .toBuilder()
                                                .setEnrichment(enrichment);
            EventEnvelope notEnrichableEvent = EventEnvelope.of(
                    event.toBuilder()
                         .setContext(context)
                         .build()
            );

            assertCannotBeEnriched(notEnrichableEvent);
        }
    }

    /**
     * Event subscriber that remembers enrichments.
     *
     * <p>This class is a part of assert checking, and as such it is not placed under the test
     * {@linkplain io.spine.server.event.given.EventEnricherTestEnv environment class} .
     */
    @SuppressWarnings("InstanceVariableNamingConvention")
    private static class TestEventSubscriber extends AbstractEventSubscriber {

        private ProjectCreatedEnrichment projectCreatedEnrichment;
        private ProjectCreatedSeparateEnrichment projectCreatedSeparateEnrichment;
        private ProjectStartedEnrichment projectStartedEnrichment;
        private SeparateEnrichmentForMultipleProjectEvents projectCompletedEnrichment;
        private SeparateEnrichmentForMultipleProjectEvents projectStarredEnrichment;
        private ProjectCreatedEnrichmentAnotherPackage projectCreatedAnotherPackEnrichment;

        @Subscribe
        public void on(ProjectCreated event, EventContext context) {
            this.projectCreatedEnrichment =
                    get(ProjectCreatedEnrichment.class, event, context);
            this.projectCreatedSeparateEnrichment =
                    get(ProjectCreatedSeparateEnrichment.class, event, context);
            this.projectCreatedAnotherPackEnrichment =
                    get(ProjectCreatedEnrichmentAnotherPackage.class, event, context);
        }

        @Subscribe
        public void on(ProjectStarted event, EventContext context) {
            this.projectStartedEnrichment = get(ProjectStartedEnrichment.class, event, context);
        }

        @Subscribe
        public void on(ProjectCompleted event, EventContext context) {
            this.projectCompletedEnrichment =
                    get(SeparateEnrichmentForMultipleProjectEvents.class, event, context);
        }

        @Subscribe
        public void on(ProjectStarred event, EventContext context) {
            this.projectStarredEnrichment =
                    get(SeparateEnrichmentForMultipleProjectEvents.class, event, context);
        }

        private static <E extends Message>
        E get(Class<E> enrichmentClass, EventMessage e, EventContext ctx) {
            return getEnrichment(enrichmentClass, ctx).orElseThrow(
                    () -> newIllegalStateException(
                            "Unable to obtain enrichment of class `%s`" +
                                    " from the event of class `%s`",
                            enrichmentClass.getCanonicalName(), e.getClass().getCanonicalName()
                    )
            );
        }
    }
}
