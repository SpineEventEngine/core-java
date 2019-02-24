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

package io.spine.server.type;

import com.google.protobuf.Message;
import io.spine.base.MessageContext;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Abstract base for classes implementing {@link MessageEnvelope}.
 *
 * @param <I> the class of the message ID
 * @param <T> the type of the object that wraps a message
 * @param <C> the type of the message context
 */
public abstract class AbstractMessageEnvelope<I extends Message, T, C extends MessageContext>
        implements MessageEnvelope<I, T, C> {

    private final T object;

    protected AbstractMessageEnvelope(T object) {
        checkNotNull(object);
        this.object = object;
    }

    @Override
    public T outerObject() {
        return object;
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractMessageEnvelope)) {
            return false;
        }
        AbstractMessageEnvelope other = (AbstractMessageEnvelope) obj;
        return Objects.equals(this.object, other.object);
    }

    @Override
    public String toString() {
        return format("%s(%s)", getClass().getSimpleName(), messageClass().typeName());
    }
}
