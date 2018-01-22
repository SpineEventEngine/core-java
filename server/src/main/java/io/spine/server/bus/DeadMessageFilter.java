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

package io.spine.server.bus;

import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Collection;

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

    private final DeadMessageTap<E> deadMessageTap;
    private final DispatcherRegistry<C, D> registry;

    DeadMessageFilter(DeadMessageTap<E> deadMessageTap, DispatcherRegistry<C, D> registry) {
        super();
        this.deadMessageTap = checkNotNull(deadMessageTap);
        this.registry = checkNotNull(registry);
    }

    @Override
    public Optional<Ack> accept(E envelope) {
        @SuppressWarnings("unchecked")
        final C cls = (C) envelope.getMessageClass();
        final Collection<D> dispatchers = registry.getDispatchers(cls);
        if (dispatchers.isEmpty()) {
            final MessageUnhandled report = deadMessageTap.capture(envelope);
            final Error error = report.asError();
            final Any packedId = Identifier.pack(envelope.getId());
            final Ack result = reject(packedId, error);
            return Optional.of(result);
        } else {
            return Optional.absent();
        }
    }
}
