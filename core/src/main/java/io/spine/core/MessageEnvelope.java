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

package io.spine.core;

import com.google.protobuf.Message;
import io.spine.type.MessageClass;

/**
 * A common interface for obtaining messages from wrapping objects.
 *
 * @param <I> the the of the message id
 * @param <T> the type of the object that wraps a message
 * @param <C> the type of the message context
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public interface MessageEnvelope<I extends Message, T, C extends Message> {

    /**
     * The ID of the message.
     */
    I getId();

    /**
     * Obtains the object which contains the message of interest.
     */
    T getOuterObject();

    /**
     * Obtains the message.
     */
    Message getMessage();

    /**
     * Obtains the message class.
     */
    MessageClass getMessageClass();

    /**
     * Obtains the context of the message.
     */
    C getMessageContext();

    /**
     * Sets the context of the enclosed message as the origin into the context of an event
     * to be build.
     *
     * @param builder event context builder into which set the event origin context
     */
    void setOriginContext(EventContext.Builder builder);
}
