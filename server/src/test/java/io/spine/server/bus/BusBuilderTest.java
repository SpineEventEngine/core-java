/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import io.spine.core.Ack;
import io.spine.server.type.MessageEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertThat;

/**
 * The abstract test suite for the tests of the builders of buses.
 *
 * @see io.spine.server.commandbus.CommandBusBuilderTest
 * @see io.spine.server.event.EventBusBuilderTest
 * @see io.spine.server.rejection.RejectionBusBuilderTest
 */
public abstract class BusBuilderTest<B extends BusBuilder<?, T, E, ?, ?>,
                                     E extends MessageEnvelope<?, T, ?>,
                                     T extends Message> {

    protected abstract B builder();

    @Test
    @DisplayName("allow adding filter")
    void allowAddingFilter() {
        BusFilter<E> filter = new MockFilter<>();

        assertThat(builder().appendFilter(filter)
                            .filters())
                .contains(filter);
    }

    @SuppressWarnings("CheckReturnValue") // calling builder
    @Test
    @DisplayName("preserve filters order")
    void preserveFiltersOrder() {
        BusFilter<E> first = new MockFilter<>();
        BusFilter<E> second = new MockFilter<>();

        B builder = builder();
        builder.appendFilter(first)
               .appendFilter(second);
        Deque<BusFilter<E>> filters = builder.filters();

        assertThat(filters.pop())
                .isEqualTo(first);
        assertThat(filters.pop())
                .isEqualTo(second);
    }

    @Test
    @DisplayName("add listener")
    void addingListener() {
        Consumer<E> listener = (e) -> {};

        assertThat(builder().addListener(listener)
                            .listeners())
                .contains(listener);
    }

    @Test
    @DisplayName("remove listener")
    void removingListener() {
        Consumer<E> listener = (e) -> {};

        assertThat(builder().addListener(listener)
                            .removeListener(listener)
                            .listeners())
                .doesNotContain(listener);
    }

    /**
     * Mock implementation of {@code BusFilter}.
     */
    private static final class MockFilter<E extends MessageEnvelope<?, ?, ?>>
            implements BusFilter<E> {

        @Override
        public Optional<Ack> accept(E envelope) {
            return Optional.empty();
        }
    }
}
