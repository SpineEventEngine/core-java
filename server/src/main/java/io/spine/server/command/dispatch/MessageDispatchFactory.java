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

package io.spine.server.command.dispatch;

import io.spine.core.MessageEnvelope;
import io.spine.server.model.AbstractHandlerMethod;

/**
 * {@link Dispatch Dispatches} instantiated using this factory send off a
 * {@link MessageEnvelope message envelope} {@link #to to a specified target method}.
 *
 * @author Mykhailo Drachuk
 */
public abstract class MessageDispatchFactory<E extends MessageEnvelope, M extends AbstractHandlerMethod> {

    private final E envelope;

    MessageDispatchFactory(E envelope) {
        this.envelope = envelope;
    }

    /**
     * @param context an object instance the method belongs to
     * @param method  handler method that processes the message and emits the events
     * @return a new dispatch which deals with a message by invoking the target method
     * in the provided context.
     */
    public abstract Dispatch<E> to(Object context, M method);

    protected E envelope() {
        return envelope;
    }
}
