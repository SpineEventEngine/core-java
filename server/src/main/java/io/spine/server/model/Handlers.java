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

package io.spine.server.model;

import io.spine.type.MessageClass;
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;

/**
 * A set of utilities for message handler methods.
 */
public final class Handlers {

    /**
     * Prevents the utility class instantiation.
     */
    private Handlers() {
    }

    /**
     * Creates a new {@link HandlerId} with the given class as the handled message type.
     *
     * @param messageClass the handled message class
     * @return new handler ID
     */
    public static HandlerId createId(MessageClass<?> messageClass) {
        HandlerTypeInfo typeInfo = HandlerTypeInfo
                .newBuilder()
                .setMessageType(typeUrl(messageClass))
                .build();
        return HandlerId
                .newBuilder()
                .setType(typeInfo)
                .build();
    }

    /**
     * Creates a new {@link HandlerId} with handled message type and origin message type.
     *
     * @param messageClass the handled message class
     * @param originClass  the origin message class
     * @return new handler ID
     */
    public static HandlerId createId(MessageClass<?> messageClass, MessageClass<?> originClass) {
        HandlerTypeInfo typeInfo = HandlerTypeInfo
                .newBuilder()
                .setMessageType(typeUrl(messageClass))
                .setOriginType(typeUrl(originClass))
                .build();
        return HandlerId
                .newBuilder()
                .setType(typeInfo)
                .build();
    }

    private static String typeUrl(MessageClass<?> messageClass) {
        TypeName typeName = messageClass.typeName();
        TypeUrl typeUrl = typeName.toUrl();
        return typeUrl.value();
    }
}
