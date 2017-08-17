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
package io.spine.server.procman;

import io.spine.core.Event;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.server.BoundedContext;
import io.spine.server.procman.given.PmEventDeliveryTestEnv.PostponedReactingRepository;
import io.spine.server.procman.given.PmEventDeliveryTestEnv.PostponingDelivery;
import io.spine.server.procman.given.PmEventDeliveryTestEnv.ReactingProjectWizard;
import io.spine.test.procman.ProjectId;
import io.spine.test.procman.event.PmProjectStarted;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static io.spine.server.procman.given.PmEventDeliveryTestEnv.projectStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
public class PmEventDeliveryShould {

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
        assertNull(ReactingProjectWizard.getEventReceived());

        final Event event = projectStarted();
        boundedContext.getEventBus()
                      .post(event);

        assertNull(ReactingProjectWizard.getEventReceived());

        final EventEnvelope expectedEnvelope = EventEnvelope.of(event);
        final PostponingDelivery delivery = PostponedReactingRepository.getDelivery();
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
}
