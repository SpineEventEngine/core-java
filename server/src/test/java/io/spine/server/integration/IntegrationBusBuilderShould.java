/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.event.EventBus;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.transport.TransportFactory;
import io.spine.test.Tests;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
public class IntegrationBusBuilderShould
        extends BusBuilderTest<IntegrationBus.Builder, ExternalMessageEnvelope, ExternalMessage> {

    @Override
    protected IntegrationBus.Builder builder() {
        return IntegrationBus.newBuilder();
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null TransportFactory")
    void doNotAcceptNullTransportFactory() {
        builder().setTransportFactory(Tests.<TransportFactory>nullRef());
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null EventBus")
    void doNotAcceptNullEventBus() {
        builder().setEventBus(Tests.<EventBus>nullRef());
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null RejectionBus")
    void doNotAcceptNullRejectionBus() {
        builder().setRejectionBus(Tests.<RejectionBus>nullRef());
    }

    @Test(expected = NullPointerException.class)
    @DisplayName("do not accept null BoundedContextName")
    void doNotAcceptNullBoundedContextName() {
        builder().setBoundedContextName(Tests.<BoundedContextName>nullRef());
    }

    @Test
    @DisplayName("return previously set TransportFactory")
    void returnPreviouslySetTransportFactory() {
        final TransportFactory mock = mock(TransportFactory.class);
        assertEquals(mock, builder().setTransportFactory(mock)
                                    .getTransportFactory()
                                    .get());
    }

    @Test
    @DisplayName("return previously set EventBus")
    void returnPreviouslySetEventBus() {
        final EventBus mock = mock(EventBus.class);
        assertEquals(mock, builder().setEventBus(mock)
                                    .getEventBus()
                                    .get());
    }

    @Test
    @DisplayName("return previously set RejectionBus")
    void returnPreviouslySetRejectionBus() {
        final RejectionBus mock = mock(RejectionBus.class);
        assertEquals(mock, builder().setRejectionBus(mock)
                                    .getRejectionBus()
                                    .get());
    }

    @Test
    @DisplayName("return previously set BoundedContextName")
    void returnPreviouslySetBoundedContextName() {
        final BoundedContextName name =
                BoundedContext.newName("Name that is expected back from the Builder");
        assertEquals(name, builder().setBoundedContextName(name)
                                    .getBoundedContextName()
                                    .get());
    }
}
