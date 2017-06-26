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
package io.spine.server.failure;

import io.spine.core.FailureEnvelope;
import io.spine.server.bus.BusFilter;
import io.spine.test.Tests;
import org.junit.Test;

import java.util.Deque;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Alex Tymchenko
 */
public class FailureBusBuilderShould {

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_DispatcherDelivery() {
        FailureBus.newBuilder()
                  .setDispatcherFailureDelivery(Tests.<DispatcherFailureDelivery>nullRef());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")    //It's fine as for the Builder tests.
    @Test
    public void return_set_DispatcherDelivery() {
        final DispatcherFailureDelivery delivery = deliveryForTests();
        assertEquals(delivery, FailureBus.newBuilder()
                                         .setDispatcherFailureDelivery(delivery)
                                         .getDispatcherFailureDelivery()
                                         .get());
    }

    @Test
    public void append_filters() {
        @SuppressWarnings("unchecked")
        final BusFilter<FailureEnvelope> first = mock(BusFilter.class);
        @SuppressWarnings("unchecked")
        final BusFilter<FailureEnvelope> second = mock(BusFilter.class);

        final FailureBus.Builder builder = FailureBus.newBuilder();
        builder.appendFilter(first)
               .appendFilter(second);
        final Deque<BusFilter<FailureEnvelope>> filters = builder.getFilters();
        assertEquals(first, filters.pop());
        assertEquals(second, filters.pop());
    }


    private static DispatcherFailureDelivery deliveryForTests() {
        return new DispatcherFailureDelivery() {
            @Override
            protected boolean shouldPostponeDelivery(FailureEnvelope deliverable,
                                                     FailureDispatcher consumer) {
                return false;   // Does not really matter for tests.
            }
        };
    }
}
