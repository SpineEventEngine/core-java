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
package io.spine.server.integration;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.Identifier;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.core.Failure;
import io.spine.core.FailureId;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for working with {@linkplain IntegrationMessage integration messages}.
 *
 * @author Alex Tymchenko
 */
class IntegrationMessages {

    private IntegrationMessages() {
        // prevent an instantiation of this utility class.
    }

    static IntegrationMessage of(Event event) {
        checkNotNull(event);
        final IntegrationMessage result = of(event.getId(), event);
        return result;
    }

    static IntegrationMessage of(Failure failure) {
        checkNotNull(failure);

        final IntegrationMessage result = of(failure.getId(), failure);
        return result;
    }

    static IntegrationMessage of(RequestedMessageTypes messageTypes) {
        checkNotNull(messageTypes);

        //TODO:2017-08-2:alex.tymchenko: identify the message with some BC identity.
        final String idString = Identifier.newUuid();
        final IntegrationMessage result = of(StringValue.newBuilder()
                                                        .setValue(idString)
                                                        .build(), messageTypes);
        return result;
    }

    private static IntegrationMessage of(Message messageId, Message message) {
        final Any packedId = Identifier.pack(messageId);
        final Any packedMessage = AnyPacker.pack(message);

        return IntegrationMessage.newBuilder()
                                 .setId(packedId)
                                 .setOriginalMessage(packedMessage)
                                 .build();
    }
}
