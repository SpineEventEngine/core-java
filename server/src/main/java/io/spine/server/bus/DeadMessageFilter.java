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

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@link BusFilter} preventing the messages that have no dispatchers from being posted to
 * the bus.
 */
final class DeadMessageFilter<T extends Message,
                              E extends MessageEnvelope<?, T, ?>,
                              C extends MessageClass<? extends Message>,
                              D extends MessageDispatcher<C, E>>
        implements BusFilter<E> {

    private final DeadMessageHandler<E> deadMessageHandler;
    private final DispatcherRegistry<C, E, D> registry;

    DeadMessageFilter(DeadMessageHandler<E> deadMessageHandler,
                      DispatcherRegistry<C, E, D> registry) {
        super();
        this.deadMessageHandler = checkNotNull(deadMessageHandler);
        this.registry = checkNotNull(registry);
    }

    @Override
    public Optional<Ack> doFilter(E envelope) {
        Collection<D> dispatchers = registry.dispatchersOf(envelope);
        if (dispatchers.isEmpty()) {
            MessageUnhandled report = deadMessageHandler.handle(envelope);
            Error error = report.asError();
            return reject(envelope, error);
        } else {
            return letPass();
        }
    }
}
