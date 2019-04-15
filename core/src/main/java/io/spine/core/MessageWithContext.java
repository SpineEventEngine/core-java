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
import io.spine.annotation.GeneratedMixin;
import io.spine.base.MessageContext;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base interfaces for outer objects of messages with contexts, such as commands or events.
 *
 * @apiNote Some methods use the {@code 'get'} prefix to mix-in with the generated code.
 */
@GeneratedMixin
public interface MessageWithContext extends Message {

    /**
     * Obtains the identifier of the message.
     */
    Message getId();

    /**
     * Obtains the packed version of the enclosed message.
     *
     * @see #enclosedMessage()
     */
    Any getMessage();

    /**
     * Obtains the context of the enclosed message.
     */
    MessageContext getContext();

    /**
     * Obtains the unpacked form of the enclosed message.
     *
     * @see #getMessage()
     */
    default Message enclosedMessage() {
        Message enclosed = AnyPacker.unpack(getMessage());
        return enclosed;
    }

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
