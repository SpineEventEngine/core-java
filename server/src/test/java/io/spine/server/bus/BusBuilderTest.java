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

package io.spine.server.bus;

import com.google.protobuf.Message;
import io.spine.core.MessageEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The abstract test suite for the tests of the builders of buses.
 *
 * @author Dmytro Dashenkov
 * @see io.spine.server.commandbus.CommandBusBuilderTest
 * @see io.spine.server.event.EventBusBuilderTest
 * @see io.spine.server.rejection.RejectionBusBuilderTest
 */
public abstract class BusBuilderTest<B extends Bus.AbstractBuilder<E, T, ?>,
                                     E extends MessageEnvelope<?, T, ?>,
                                     T extends Message> {

    protected abstract B builder();

    @Test
    @DisplayName("allow adding filter")
    void allowAddingFilter() {
        @SuppressWarnings("unchecked") BusFilter<E> filter = mock(BusFilter.class);

        assertTrue(builder().appendFilter(filter)
                            .getFilters()
                            .contains(filter));
    }

    @Test
    @DisplayName("allow removing filter")
    void allowRemovingFilter() {
        @SuppressWarnings("unchecked") BusFilter<E> filter = mock(BusFilter.class);

        assertFalse(builder().appendFilter(filter)
                             .removeFilter(filter)
                             .getFilters()
                             .contains(filter));
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    @Test
    @DisplayName("preserve filters order")
    void preserveFiltersOrder() {
        @SuppressWarnings("unchecked") BusFilter<E> first = mock(BusFilter.class);
        @SuppressWarnings("unchecked") BusFilter<E> second = mock(BusFilter.class);

        B builder = builder();
        builder.appendFilter(first)
               .appendFilter(second);
        Deque<BusFilter<E>> filters = builder.getFilters();
        assertEquals(first, filters.pop());
        assertEquals(second, filters.pop());
    }
}
