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
import io.spine.core.BoundedContextNames;
import io.spine.server.bus.BusBuilderTest;
import io.spine.server.event.EventBus;
import io.spine.server.transport.TransportFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.testing.Tests.nullRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("IntegrationBus Builder should")
class IntegrationBusBuilderTest
        extends BusBuilderTest<IntegrationBus.Builder, ExternalMessageEnvelope, ExternalMessage> {

    @Override
    protected IntegrationBus.Builder builder() {
        return IntegrationBus.newBuilder();
    }

    @Nested
    @DisplayName("not accept null")
    class NotAcceptNull {

        @Test
        @DisplayName("TransportFactory")
        void transportFactory() {
            assertThrows(NullPointerException.class,
                         () -> builder().setTransportFactory(nullRef()));
        }

        @Test
        @DisplayName("EventBus")
        void eventBus() {
            assertThrows(NullPointerException.class,
                         () -> builder().setEventBus(nullRef()));
        }

        @Test
        @DisplayName("BoundedContextName")
        void boundedContextName() {
            assertThrows(NullPointerException.class,
                         () -> builder().setBoundedContextName(nullRef()));
        }
    }

    @Nested
    @DisplayName("return previously set")
    class ReturnSet {

        @Test
        @DisplayName("TransportFactory")
        void transportFactory() {
            TransportFactory mock = mock(TransportFactory.class);
            assertPresent(mock, builder().setTransportFactory(mock)
                                         .getTransportFactory());
        }

        @Test
        @DisplayName("EventBus")
        void eventBus() {
            EventBus mock = mock(EventBus.class);
            assertPresent(mock, builder().setEventBus(mock)
                                         .getEventBus());
        }

        @Test
        @DisplayName("BoundedContextName")
        void boundedContextName() {
            BoundedContextName name =
                    BoundedContextNames.newName("Name that is expected back from the Builder");
            assertPresent(name, builder().setBoundedContextName(name)
                                         .getBoundedContextName());
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // is the purpose of the method.
        private <T> void assertPresent(T expected, Optional<T> optional) {
            assertTrue(optional.isPresent());
            assertEquals(expected, optional.get());
        }
    }
}
