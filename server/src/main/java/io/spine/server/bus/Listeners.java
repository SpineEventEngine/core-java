/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import io.spine.server.type.SignalEnvelope;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages consumption of a message posted to the bus by its listeners.
 *
 * @param <E> the type of the {@link SignalEnvelope} posted by the bus
 */
final class Listeners<E extends SignalEnvelope<?, ?, ?>> implements Consumer<E> {

    private final ImmutableSet<Listener<E>> listeners;

    Listeners(BusBuilder<?, ?, E, ?, ?> builder) {
        checkNotNull(builder);
        this.listeners = ImmutableSet.copyOf(builder.listeners());
    }

    @Override
    public void accept(E envelope) {
        listeners.forEach(listener -> {
            try {
                listener.accept(envelope);
            } catch (RuntimeException ignored) {
                // Do nothing.
            }
        });
    }

    @VisibleForTesting
    boolean contains(Listener<E> listener) {
        return listeners.contains(listener);
    }
}
