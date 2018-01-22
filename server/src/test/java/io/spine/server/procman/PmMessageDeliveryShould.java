/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
package io.spine.server.procman;

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
import io.spine.server.procman.given.PmMessageDeliveryTestEnv.PostponingCommandDelivery;
import io.spine.server.procman.given.PmMessageDeliveryTestEnv.PostponingEventDelivery;
import io.spine.server.procman.given.PmMessageDeliveryTestEnv.PostponingRejectionDelivery;
import io.spine.server.procman.given.PmMessageDeliveryTestEnv.PostponingRepository;
import io.spine.server.procman.given.PmMessageDeliveryTestEnv.ReactingProjectWizard;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.command.PmCreateProject;
import io.spine.test.procman.event.PmProjectStarted;
import io.spine.test.procman.rejection.Rejections.PmCannotStartArchivedProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static io.spine.core.Rejections.getMessage;
import static io.spine.server.model.ModelTests.clearModel;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.cannotStartProject;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.createProject;
import static io.spine.server.procman.given.PmMessageDeliveryTestEnv.projectStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class PmMessageDeliveryShould {

    private BoundedContext boundedContext;
    private PostponingRepository repository;

    @Before
    public void setUp() {
        clearModel();
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new PostponingRepository();
        boundedContext.register(repository);
    }

    @After
    public void tearDown() throws Exception {
        repository.close();
        boundedContext.close();
    }

    @Test
    public void postpone_events_dispatched_to_reactor_method() {
        assertNull(ReactingProjectWizard.getEventReceived());

        final Event event = projectStarted();
        boundedContext.getEventBus()
                      .post(event);

        assertNull(ReactingProjectWizard.getEventReceived());

        final EventEnvelope expectedEnvelope = EventEnvelope.of(event);
        final PostponingEventDelivery delivery = repository.getEventEndpointDelivery();
        final Map<ProjectId, EventEnvelope> postponedEvents = delivery.getPostponedEvents();
        assertTrue(postponedEvents.size() == 1 &&
                           postponedEvents.containsValue(expectedEnvelope));

        final ProjectId projectId = postponedEvents.keySet()
                                                   .iterator()
                                                   .next();

        delivery.deliverNow(projectId, postponedEvents.get(projectId));

        final PmProjectStarted deliveredEventMsg = ReactingProjectWizard.getEventReceived();
        assertNotNull(deliveredEventMsg);
        assertEquals(Events.getMessage(event), deliveredEventMsg);
    }


    @Test
    public void postpone_rejections_dispatched_to_reactor_method() {
        assertNull(ReactingProjectWizard.getEventReceived());

        final Rejection rejection = cannotStartProject();
        boundedContext.getRejectionBus()
                      .post(rejection);

        assertNull(ReactingProjectWizard.getRejectionReceived());

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

        final PmCannotStartArchivedProject deliveredRejectionMsg =
                ReactingProjectWizard.getRejectionReceived();
        assertNotNull(deliveredRejectionMsg);
        assertEquals(getMessage(rejection), deliveredRejectionMsg);
    }

    @Test
    public void postpone_commands_dispatched_to_command_subscriber_method() {
        assertNull(ReactingProjectWizard.getCommandReceived());

        final Command command = createProject();
        boundedContext.getCommandBus()
                      .post(command, StreamObservers.<Ack>noOpObserver());

        assertNull(ReactingProjectWizard.getCommandReceived());

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

        final PmCreateProject deliveredCommandMsg = ReactingProjectWizard.getCommandReceived();
        assertNotNull(deliveredCommandMsg);
        assertEquals(Commands.getMessage(command), deliveredCommandMsg);
    }
}
