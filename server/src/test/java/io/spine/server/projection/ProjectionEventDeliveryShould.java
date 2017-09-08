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
package io.spine.server.projection;

import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.server.BoundedContext;
import io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.PostponingEventDelivery;
import io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.PostponingRepository;
import io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.ProjectDetails;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.event.PrjProjectCreated;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static io.spine.server.model.ModelTests.clearModel;
import static io.spine.server.projection.given.ProjectionEventDeliveryTestEnv.projectCreated;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class ProjectionEventDeliveryShould {

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
    public void postpone_events_dispatched_to_subscriber_method() {
        assertNull(ProjectDetails.getEventReceived());

        final Event event = projectCreated();
        boundedContext.getEventBus()
                      .post(event);

        assertNull(ProjectDetails.getEventReceived());

        final EventEnvelope expectedEnvelope = EventEnvelope.of(event);
        final PostponingEventDelivery delivery = repository.getEndpointDelivery();
        final Map<ProjectId, EventEnvelope> postponedEvents = delivery.getPostponedEvents();
        assertTrue(postponedEvents.size() == 1 &&
                           postponedEvents.containsValue(expectedEnvelope));

        final ProjectId projectId = postponedEvents.keySet()
                                                   .iterator()
                                                   .next();

        delivery.deliverNow(projectId, postponedEvents.get(projectId));

        final PrjProjectCreated deliveredEventMsg = ProjectDetails.getEventReceived();
        assertNotNull(deliveredEventMsg);
        assertEquals(Events.getMessage(event), deliveredEventMsg);
    }
}
