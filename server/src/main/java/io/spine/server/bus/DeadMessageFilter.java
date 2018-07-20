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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.Identifier;
import io.spine.core.Ack;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.bus.Buses.reject;

/**
 * The {@link BusFilter} preventing the messages that have no dispatchers from being posted to
 * the bus.
 *
 * @author Dmytro Dashenkov
 */
final class DeadMessageFilter<T extends Message,
                              E extends MessageEnvelope<?, T, ?>,
                              C extends MessageClass,
                              D extends MessageDispatcher<C, E, ?>>
        extends AbstractBusFilter<E> {

    private final DeadMessageHandler<E> deadMessageHandler;
    private final DispatcherRegistry<C, D> registry;

    DeadMessageFilter(DeadMessageHandler<E> deadMessageHandler, DispatcherRegistry<C, D> registry) {
        super();
        this.deadMessageHandler = checkNotNull(deadMessageHandler);
        this.registry = checkNotNull(registry);
    }

    @Override
    public Optional<Ack> accept(E envelope) {
        @SuppressWarnings("unchecked") C cls = (C) envelope.getMessageClass();
        Collection<D> dispatchers = registry.getDispatchers(cls);
        if (dispatchers.isEmpty()) {
            MessageUnhandled report = deadMessageHandler.handle(envelope);
            Error error = report.asError();
            Any packedId = Identifier.pack(envelope.getId());
            Ack result = reject(packedId, error);
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }
}
