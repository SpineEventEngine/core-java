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

package io.spine.core;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.GeneratedMixin;
import io.spine.base.MessageContext;
import io.spine.base.SerializableMessage;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base interfaces for outer objects of messages with contexts, such as commands or events.
 *
 * @param <I>
 *         the type of the message identifier
 * @param <M>
 *         the type of the enclosed messages
 * @param <C>
 *         the type of the message context
 */
@GeneratedMixin
public interface MessageWithContext<I extends MessageId,
                                    M extends SerializableMessage,
                                    C extends MessageContext>
        extends Message {

    /**
     * Obtains the identifier of the message.
     */
    I getId();

    /**
     * Obtains the packed version of the enclosed message.
     *
     * @see #enclosedMessage()
     */
    Any getMessage();

    /**
     * Obtains the context of the enclosed message.
     */
    C getContext();

    /**
     * Obtains the identifier of the message.
     */
    default I id() {
        return getId();
    }

    /**
     * Obtains the unpacked form of the enclosed message.
     *
     * @see #getMessage()
     */
    @SuppressWarnings("unchecked") // protected by generic params of extending interfaces
    default M enclosedMessage() {
        Message enclosed = AnyPacker.unpack(getMessage());
        return (M) enclosed;
    }

    /**
     * Obtains the context of the enclosed message.
     */
    default C context() {
        return getContext();
    }

    /**
     *
     * Obtains the ID of the tenant under which the message was created.
     */
    TenantId tenant();

    /**
     * Obtains the time when the message was created.
     */
    Timestamp time();

    /**
     * Obtains the type URL of the enclosed message.
     */
    default TypeUrl typeUrl() {
        return TypeUrl.ofEnclosed(getMessage());
    }

    /**
     * Verifies if the enclosed message has the same type as the passed, or the passed type
     * is the super-type of the message.
     */
    default boolean is(Class<? extends Message> enclosedMessageClass) {
        checkNotNull(enclosedMessageClass);
        Message enclosed = enclosedMessage();
        boolean result = enclosedMessageClass.isAssignableFrom(enclosed.getClass());
        return result;
    }
}
