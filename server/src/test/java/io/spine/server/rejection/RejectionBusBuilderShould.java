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
package io.spine.server.rejection;

import io.spine.core.Rejection;
import io.spine.core.RejectionEnvelope;
import io.spine.server.bus.BusBuilderShould;
import io.spine.test.Tests;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alex Tymchenko
 */
public class RejectionBusBuilderShould extends BusBuilderShould<RejectionBus.Builder,
        RejectionEnvelope,
        Rejection> {

    @Override
    protected RejectionBus.Builder builder() {
        return RejectionBus.newBuilder();
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_DispatcherDelivery() {
        builder().setDispatcherRejectionDelivery(Tests.<DispatcherRejectionDelivery>nullRef());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")    //It's fine as for the Builder tests.
    @Test
    public void return_set_DispatcherDelivery() {
        final DispatcherRejectionDelivery delivery = deliveryForTests();
        assertEquals(delivery, builder().setDispatcherRejectionDelivery(delivery)
                                        .getDispatcherRejectionDelivery()
                                        .get());
    }

    private static DispatcherRejectionDelivery deliveryForTests() {
        return new DispatcherRejectionDelivery() {
            @Override
            protected boolean shouldPostponeDelivery(RejectionEnvelope deliverable,
                                                     RejectionDispatcher<?> consumer) {
                return false;   // Does not really matter for tests.
            }
        };
    }
}
