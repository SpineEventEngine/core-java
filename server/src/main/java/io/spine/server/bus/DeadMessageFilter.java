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

package io.spine.server.bus;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.IsSent;
import io.spine.envelope.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;

/**
 * The {@link BusFilter} preventing the messages that have no dispatchers from being posted to
 * the bus.
 *
 * @author Dmytro Dashenkov
 */
final class DeadMessageFilter<T extends Message,
                              E extends MessageEnvelope<T>,
                              C extends MessageClass,
                              D extends MessageDispatcher<C, E>>
        extends AbstractBusFilter<E> {

    private final Bus<T, E, C, D> bus;
    private final DeadMessageHandler<E> deadMessageHandler;

    DeadMessageFilter(Bus<T, E, C, D> bus) {
        super();
        this.bus = bus;
        this.deadMessageHandler = bus.getDeadMessageHandler();
    }

    @Override
    public Optional<IsSent> accept(E envelope) {
        final DispatcherRegistry<C, D> registry = bus.registry();
        @SuppressWarnings("unchecked")
        final C cls = (C) envelope.getMessageClass();
        final Collection<D> dispatchers = registry.getDispatchers(cls);
        if (dispatchers.isEmpty()) {
            final Error error = deadMessageHandler.handleDeadMessage(envelope);
            final IsSent result = Buses.reject(bus.getId(envelope), error);
            return Optional.of(result);
        } else {
            return Optional.absent();
        }
    }
}
