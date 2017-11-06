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

import io.spine.core.BoundedContextName;
import io.spine.server.BoundedContext;
import io.spine.server.bus.BusBuilderShould;
import io.spine.server.event.EventBus;
import io.spine.server.integration.route.RoutingSchema;
import io.spine.server.rejection.RejectionBus;
import io.spine.test.Tests;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusBuilderShould
        extends BusBuilderShould<IntegrationBus.Builder, ExternalMessageEnvelope, ExternalMessage> {

    @Override
    protected IntegrationBus.Builder builder() {
        return IntegrationBus.newBuilder();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_TransportFactory() {
        builder().setTransportFactory(Tests.<TransportFactory>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_EventBus() {
        builder().setEventBus(Tests.<EventBus>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_RejectionBus() {
        builder().setRejectionBus(Tests.<RejectionBus>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_BoundedContextName() {
        builder().setBoundedContextName(Tests.<BoundedContextName>nullRef());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_RoutingSchema() {
        builder().setRoutingSchema(Tests.<RoutingSchema>nullRef());
    }

    @Test
    public void return_previously_set_TransportFactory() {
        final TransportFactory mock = mock(TransportFactory.class);
        assertEquals(mock, builder().setTransportFactory(mock)
                                    .getTransportFactory()
                                    .get());
    }

    @Test
    public void return_previously_set_EventBus() {
        final EventBus mock = mock(EventBus.class);
        assertEquals(mock, builder().setEventBus(mock)
                                    .getEventBus()
                                    .get());
    }

    @Test
    public void return_previously_set_RejectionBus() {
        final RejectionBus mock = mock(RejectionBus.class);
        assertEquals(mock, builder().setRejectionBus(mock)
                                    .getRejectionBus()
                                    .get());
    }

    @Test
    public void return_previously_set_BoundedContextName() {
        final BoundedContextName name =
                BoundedContext.newName("Name that is expected back from the Builder");
        assertEquals(name, builder().setBoundedContextName(name)
                                    .getBoundedContextName()
                                    .get());
    }

    @Test
    public void return_previously_set_RoutingSchema() {
        final RoutingSchema mock = mock(RoutingSchema.class);
        assertEquals(mock, builder().setRoutingSchema(mock)
                                    .getRoutingSchema()
                                    .get());
    }
}
