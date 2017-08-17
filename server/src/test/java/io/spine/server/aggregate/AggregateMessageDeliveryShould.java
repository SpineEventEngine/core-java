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
package io.spine.server.aggregate;

import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Commands;
import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.grpc.StreamObservers;
import io.spine.server.BoundedContext;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.PostponedReactingRepository;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.PostponingCommandDelivery;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.PostponingEventDelivery;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.PostponingRejectionDelivery;
import io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.ReactingProject;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.event.AggProjectStarted;
import io.spine.test.aggregate.rejection.Rejections.AggCannotStartArchivedProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static io.spine.core.Rejections.getMessage;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.cannotStartProject;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.createProject;
import static io.spine.server.aggregate.given.AggregateMessageDeliveryTestEnv.projectStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class AggregateMessageDeliveryShould {

    private BoundedContext boundedContext;
    private PostponedReactingRepository repository;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new PostponedReactingRepository();
        boundedContext.register(repository);
    }

    @After
    public void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Test
    public void postpone_events_dispatched_to_reactor_method() {
        assertNull(ReactingProject.getEventReceived());

        final Event event = projectStarted();
        boundedContext.getEventBus()
                      .post(event);

        assertNull(ReactingProject.getEventReceived());

        final EventEnvelope expectedEnvelope = EventEnvelope.of(event);
        final PostponingEventDelivery delivery = repository.getEventEndpointDelivery();
        final Map<ProjectId, EventEnvelope> postponedEvents = delivery.getPostponedEvents();
        assertTrue(postponedEvents.size() == 1 &&
                           postponedEvents.containsValue(expectedEnvelope));

        final ProjectId projectId = postponedEvents.keySet()
                                              .iterator()
                                              .next();

        delivery.deliverNow(projectId, postponedEvents.get(projectId));

        final AggProjectStarted deliveredEventMsg = ReactingProject.getEventReceived();
        assertNotNull(deliveredEventMsg);
        assertEquals(Events.getMessage(event), deliveredEventMsg);
    }

    @Test
    public void postpone_rejections_dispatched_to_reactor_method() {
        assertNull(ReactingProject.getEventReceived());

        final Rejection rejection = cannotStartProject();
        boundedContext.getRejectionBus()
                      .post(rejection);

        assertNull(ReactingProject.getRejectionReceived());

        final RejectionEnvelope expectedEnvelope = RejectionEnvelope.of(rejection);
        final PostponingRejectionDelivery delivery = repository.getRejectionEndpointDelivery();
        final Map<ProjectId, RejectionEnvelope> postponedRejections =
                delivery.getPostponedRejections();
        assertTrue(postponedRejections.size() == 1 &&
                           postponedRejections.containsValue(expectedEnvelope));

        final ProjectId projectId = postponedRejections.keySet()
                                                   .iterator()
                                                   .next();

        delivery.deliverNow(projectId, postponedRejections.get(projectId));

        final AggCannotStartArchivedProject deliveredRejectionMsg
                = ReactingProject.getRejectionReceived();
        assertNotNull(deliveredRejectionMsg);
        assertEquals(getMessage(rejection), deliveredRejectionMsg);
    }

    @Test
    public void postpone_commands_dispatched_to_command_handler_method() {
        assertNull(ReactingProject.getCommandReceived());

        final Command command = createProject();
        boundedContext.getCommandBus()
                      .post(command, StreamObservers.<Ack>noOpObserver());

        assertNull(ReactingProject.getCommandReceived());

        final CommandEnvelope expectedEnvelope = CommandEnvelope.of(command);
        final PostponingCommandDelivery delivery = repository.getCommandEndpointDelivery();
        final Map<ProjectId, CommandEnvelope> postponedCommands =
                delivery.getPostponedCommands();
        assertTrue(postponedCommands.size() == 1 &&
                           postponedCommands.containsValue(expectedEnvelope));

        final ProjectId projectId = postponedCommands.keySet()
                                                       .iterator()
                                                       .next();

        delivery.deliverNow(projectId, postponedCommands.get(projectId));

        final AggCreateProject deliveredCommandMsg = ReactingProject.getCommandReceived();
        assertNotNull(deliveredCommandMsg);
        assertEquals(Commands.getMessage(command), deliveredCommandMsg);
    }
}
