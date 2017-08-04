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

import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.integration.given.IntegrationBusTestEnv.ExternalSubscriber;
import io.spine.server.integration.given.IntegrationBusTestEnv.ProjectDetails;
import io.spine.server.integration.local.LocalTransportFactory;
import org.junit.Test;

import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExtEntitySubscriber;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExternalSubscriber;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithTransport;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusShould {

    @Test
    public void dispatch_events_from_one_BC_to_entities_with_external_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        contextWithExtEntitySubscriber(transportFactory);

        assertNull(ProjectDetails.getExternalEvent());

        final Event event = projectCreated();
        sourceContext.getEventBus().post(event);

        assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getExternalEvent());
    }

    @Test
    public void avoid_dispatch_events_from_one_BC_to_domestic_entity_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        final BoundedContext destContext = contextWithExtEntitySubscriber(transportFactory);

        assertNull(ProjectDetails.getDomesticEvent());

        final Event event = projectStarted();
        sourceContext.getEventBus().post(event);

        assertNotEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
        assertNull(ProjectDetails.getDomesticEvent());

        destContext.getEventBus().post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());
    }

    @Test
    public void dispatch_events_from_one_BC_to_external_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        contextWithExternalSubscriber(transportFactory);

        assertNull(ExternalSubscriber.getExternalEvent());

        final Event event = projectCreated();
        sourceContext.getEventBus().post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()), ExternalSubscriber.getExternalEvent());
    }

    @Test
    public void avoid_dispatch_events_from_one_BC_to_domestic_st_alone_subscribers_of_another_BC() {
        final LocalTransportFactory transportFactory = LocalTransportFactory.newInstance();

        final BoundedContext sourceContext = contextWithTransport(transportFactory);
        final BoundedContext destContext = contextWithExternalSubscriber(transportFactory);

        assertNull(ExternalSubscriber.getDomesticEvent());

        final Event event = projectStarted();
        sourceContext.getEventBus().post(event);

        assertNotEquals(AnyPacker.unpack(event.getMessage()),
                        ExternalSubscriber.getDomesticEvent());
        assertNull(ExternalSubscriber.getDomesticEvent());

        destContext.getEventBus().post(event);
        assertEquals(AnyPacker.unpack(event.getMessage()), ExternalSubscriber.getDomesticEvent());
    }
}
