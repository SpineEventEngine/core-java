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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventBus;
import io.spine.server.integration.given.IntegrationBusTestEnv.ContextAwareProjectDetails;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectCountAggregate;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectDetails;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectEventsSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectRejectionsExtSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectStartedExtSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectWizard;
import io.spine.server.integration.memory.InMemoryRoutingSchema;
import io.spine.server.integration.memory.InMemoryTransportFactory;
import io.spine.server.integration.route.RoutingSchema;
import io.spine.server.rejection.RejectionBus;
import io.spine.validate.Validate;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.server.integration.ExternalMessageValidationError.UNSUPPORTED_EXTERNAL_MESSAGE;
import static io.spine.server.integration.given.IntegrationBusTestEnv.cannotStartArchivedProject;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithContextAwareEntitySubscriber;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExtEntitySubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExternalSubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectCreatedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectStartedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithTransport;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusShould {

    @Before
    public void setUp() {
        ProjectDetails.clear();
        ProjectWizard.clear();
        ProjectCountAggregate.clear();
        ContextAwareProjectDetails.clear();
        ProjectEventsSubscriber.clear();
        ProjectStartedExtSubscriber.clear();
    }

    @Test
    public void dispatch_events_from_one_BC_to_entities_with_ext_subscribers_of_another_BC() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        contextWithExtEntitySubscribers(transportFactory, routingSchema);

        assertNull(ProjectDetails.getExternalEvent());
        assertNull(ProjectWizard.getExternalEvent());
        assertNull(ProjectCountAggregate.getExternalEvent());

        final Event event = projectCreated();
        sourceContext.getEventBus()
                     .post(event);

        final Message expectedMessage = AnyPacker.unpack(event.getMessage());
        assertEquals(expectedMessage, ProjectDetails.getExternalEvent());
        assertEquals(expectedMessage, ProjectWizard.getExternalEvent());
        assertEquals(expectedMessage, ProjectCountAggregate.getExternalEvent());
    }

    @Test
    public void avoid_dispatching_events_from_one_BC_to_domestic_entity_subscribers_of_another_BC() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        final BoundedContext destContext = contextWithExtEntitySubscribers(transportFactory,
                                                                           routingSchema);

        assertNull(ProjectDetails.getDomesticEvent());

        final Event event = projectStarted();
        sourceContext.getEventBus()
                     .post(event);

        assertNotEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
        assertNull(ProjectDetails.getDomesticEvent());

        destContext.getEventBus()
                   .post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
    }

    @Test
    public void dispatch_events_from_one_BC_to_external_subscribers_of_another_BC() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        contextWithExternalSubscribers(transportFactory, routingSchema);

        assertNull(ProjectEventsSubscriber.getExternalEvent());

        final Event event = projectCreated();
        sourceContext.getEventBus()
                     .post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());
    }

    @Test
    public void avoid_dispatching_events_from_one_BC_to_domestic_standalone_subscribers_of_another_BC() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        final BoundedContext destContext = contextWithExternalSubscribers(transportFactory,
                                                                          routingSchema);

        assertNull(ProjectEventsSubscriber.getDomesticEvent());

        final Event event = projectStarted();
        sourceContext.getEventBus()
                     .post(event);

        assertNotEquals(AnyPacker.unpack(event.getMessage()),
                        ProjectEventsSubscriber.getDomesticEvent());
        assertNull(ProjectEventsSubscriber.getDomesticEvent());

        destContext.getEventBus()
                   .post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()),
                     ProjectEventsSubscriber.getDomesticEvent());
    }

    @Test
    public void dispatch_events_from_one_BC_to_entities_with_ext_subscribers_of_multiple_BCs() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final Set<BoundedContextName> destinationNames = newHashSet();
        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        for (int i = 0; i < 42; i++) {
            final BoundedContext destinationCtx =
                    contextWithContextAwareEntitySubscriber(transportFactory, routingSchema);
            final BoundedContextName name = destinationCtx.getName();
            destinationNames.add(name);
        }

        assertTrue(ContextAwareProjectDetails.getExternalContexts()
                                             .isEmpty());

        final Event event = projectCreated();
        sourceContext.getEventBus()
                     .post(event);

        assertEquals(destinationNames.size(),
                     ContextAwareProjectDetails.getExternalContexts()
                                               .size());

        assertEquals(destinationNames.size(),
                     ContextAwareProjectDetails.getExternalEvents()
                                               .size());

    }

    @SuppressWarnings("unused")     // variables declared for readability.
    @Test
    public void dispatch_events_from_one_BC_to_two_BCs_with_different_needs() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        final BoundedContext destA = contextWithProjectCreatedNeeds(transportFactory, routingSchema);
        final BoundedContext destB = contextWithProjectStartedNeeds(transportFactory, routingSchema);

        assertNull(ProjectStartedExtSubscriber.getExternalEvent());
        assertNull(ProjectEventsSubscriber.getExternalEvent());

        final EventBus sourceEventBus = sourceContext.getEventBus();
        final Event eventA = projectCreated();
        sourceEventBus.post(eventA);
        final Event eventB = projectStarted();
        sourceEventBus.post(eventB);

        assertEquals(AnyPacker.unpack(eventA.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());

        assertEquals(AnyPacker.unpack(eventB.getMessage()),
                     ProjectStartedExtSubscriber.getExternalEvent());
    }

    @Test
    public void update_local_subscriptions_upon_repeated_RequestedMessageTypes() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        final BoundedContext destinationCtx = contextWithTransport(transportFactory, routingSchema);

        // Prepare two external subscribers for the different events in the the `destinationCtx`.
        final ProjectEventsSubscriber projectCreatedSubscriber
                = new ProjectEventsSubscriber();
        final ProjectStartedExtSubscriber projectStartedSubscriber
                = new ProjectStartedExtSubscriber();

        // Before anything happens, there were no events received by those.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are prepared along with the `EventBus` of the source bounded context.
        final EventBus sourceEventBus = sourceContext.getEventBus();
        final Event eventA = projectCreated();
        final Event eventB = projectStarted();

        // Both events are emitted, `ProjectCreated` subscriber only is present.
        destinationCtx.getIntegrationBus()
                      .register(projectCreatedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // Only `ProjectCreated` should have been dispatched.
        assertEquals(AnyPacker.unpack(eventA.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Clear before the next round starts.
        ProjectStartedExtSubscriber.clear();
        ProjectEventsSubscriber.clear();

        // Both events are emitted, No external subscribers at all.
        destinationCtx.getIntegrationBus()
                      .unregister(projectCreatedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // No events should have been dispatched.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are emitted, `ProjectStarted` subscriber only is present
        destinationCtx.getIntegrationBus()
                      .register(projectStartedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // This time `ProjectStarted` event should only have been dispatched.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertEquals(AnyPacker.unpack(eventB.getMessage()),
                     ProjectStartedExtSubscriber.getExternalEvent());
    }

    @Test
    public void dispatch_rejections_from_one_BC_to_external_subscribers_of_another_BC() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory, routingSchema);
        contextWithExternalSubscribers(transportFactory, routingSchema);

        assertNull(ProjectRejectionsExtSubscriber.getExternalRejection());
        assertNull(ProjectCountAggregate.getExternalRejection());
        assertNull(ProjectWizard.getExternalRejection());

        final Rejection rejection = cannotStartArchivedProject();
        sourceContext.getRejectionBus()
                     .post(rejection);
        final Message rejectionMessage = AnyPacker.unpack(rejection.getMessage());

        assertEquals(rejectionMessage, ProjectRejectionsExtSubscriber.getExternalRejection());
        assertEquals(rejectionMessage, ProjectCountAggregate.getExternalRejection());
        assertEquals(rejectionMessage, ProjectWizard.getExternalRejection());
    }

    @Test
    public void not_dispatch_events_to_domestic_subscribers_if_they_requested_external() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext context = contextWithExtEntitySubscribers(transportFactory,
                                                                       routingSchema);
        final ProjectEventsSubscriber eventSubscriber = new ProjectEventsSubscriber();
        final EventBus eventBus = context.getEventBus();
        eventBus.register(eventSubscriber);

        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectDetails.getExternalEvent());
        assertNull(ProjectWizard.getExternalEvent());
        assertNull(ProjectCountAggregate.getExternalEvent());

        final Event projectCreated = projectCreated();
        eventBus.post(projectCreated);

        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectDetails.getExternalEvent());
        assertNull(ProjectWizard.getExternalEvent());
        assertNull(ProjectCountAggregate.getExternalEvent());
    }

    @Test
    public void not_dispatch_rejections_to_domestic_subscribers_if_they_requested_external() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();

        final BoundedContext sourceContext = contextWithExtEntitySubscribers(transportFactory,
                                                                             routingSchema);
        final ProjectRejectionsExtSubscriber standaloneSubscriber =
                new ProjectRejectionsExtSubscriber();

        final RejectionBus rejectionBus = sourceContext.getRejectionBus();
        rejectionBus.register(standaloneSubscriber);

        assertNull(ProjectRejectionsExtSubscriber.getExternalRejection());
        assertNull(ProjectWizard.getExternalRejection());
        assertNull(ProjectCountAggregate.getExternalRejection());

        final Rejection rejection = cannotStartArchivedProject();
        rejectionBus.post(rejection);

        assertNull(ProjectRejectionsExtSubscriber.getExternalRejection());
        assertNull(ProjectWizard.getExternalRejection());
        assertNull(ProjectCountAggregate.getExternalRejection());
    }

    @Test
    public void emit_unsupported_external_message_exception_if_message_type_is_unknown() {
        final InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
        final RoutingSchema routingSchema = InMemoryRoutingSchema.newInstance();
        final BoundedContext boundedContext = contextWithTransport(transportFactory, routingSchema);

        final Event event = projectCreated();
        final BoundedContextName boundedContextName = BoundedContext.newName("External context ID");
        final ExternalMessage externalMessage = ExternalMessages.of(event,
                                                                    boundedContextName);
        final MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
        boundedContext.getIntegrationBus()
                      .post(externalMessage, observer);
        final Error error = observer.firstResponse()
                                    .getStatus()
                                    .getError();
        assertFalse(Validate.isDefault(error));
        assertEquals(ExternalMessageValidationError.getDescriptor()
                                                   .getFullName(),
                     error.getType());
        assertTrue(UNSUPPORTED_EXTERNAL_MESSAGE.getNumber() == error.getCode());
    }
}
