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
import io.spine.base.Identifier;
import io.spine.core.MessageEnvelope;
import io.spine.type.MessageClass;

import static java.lang.String.format;

/**
 * A bus which delivers a message to one dispatcher.
 *
 * @author Alexander Yevsyukov
 * @see MulticastBus
 */
public abstract class UnicastBus<T extends Message,
                                 E extends MessageEnvelope<?, T, ?>,
                                 C extends MessageClass,
                                 D extends MessageDispatcher<C, E, ?>> extends Bus<T, E, C, D> {

    protected UnicastBus(BusBuilder<E, T, ?> builder) {
        super(builder);
    }

    protected D getDispatcher(E envelope) {
        @SuppressWarnings("unchecked") // protected by overloaded return values of envelope classes
        C messageClass = (C) envelope.getMessageClass();
        return registry().getDispatcher(messageClass)
                         .orElseThrow(() -> noDispatcherFound(envelope));
    }

    private static IllegalStateException noDispatcherFound(MessageEnvelope envelope) {
        String id = Identifier.toString(envelope.getId());
        String msg = format("No dispatcher found for the command (class: %s id: %s).",
                            envelope.getMessageClass(),
                            id);
        throw new IllegalStateException(msg);
    }
}
