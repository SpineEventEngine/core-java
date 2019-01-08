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
package io.spine.server.integration;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.BoundedContextName;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for working with {@linkplain ExternalMessage external messages}.
 */
@Internal
public final class ExternalMessages {

    /** Prevents instantiation of this utility class. */
    private ExternalMessages() {}

    /**
     * Wraps the instance of {@link Event} into an {@code ExternalMessage}.
     *
     * @param event  the event to wrap
     * @param origin the name of the bounded context in which the event was created
     * @return the external message wrapping the given event
     */
    public static ExternalMessage of(Event event, BoundedContextName origin) {
        checkNotNull(event);
        checkNotNull(origin);

        ExternalMessage result = of(event.getId(), event, origin);
        return result;
    }

    /**
     * Wraps the instance of {@link Command} into an {@code ExternalMessage}.
     *
     * @param command the command to wrap
     * @param origin  the name of the bounded context in which the command was created
     * @return the external message wrapping the given command
     */
    public static ExternalMessage of(Command command, BoundedContextName origin) {
        checkNotNull(command);
        checkNotNull(origin);

        ExternalMessage result = of(command.getId(), command, origin);
        return result;
    }

    /**
     * Wraps the instance of {@link RequestForExternalMessages} into an {@code ExternalMessage}.
     *
     * @param request the request to wrap
     * @param origin  the name of bounded context in which the request was created
     * @return the external message wrapping the given request
     */
    static ExternalMessage of(RequestForExternalMessages request, BoundedContextName origin) {
        checkNotNull(request);
        checkNotNull(origin);

        String idString = Identifier.newUuid();
        ExternalMessage result = of(StringValue.newBuilder()
                                               .setValue(idString)
                                               .build(),
                                    request,
                                    origin);
        return result;
    }

    private static ExternalMessage of(Message messageId,
                                      Message message,
                                      BoundedContextName boundedContextName) {
        Any packedId = Identifier.pack(messageId);
        Any packedMessage = AnyPacker.pack(message);

        ExternalMessage result = ExternalMessage
                .newBuilder()
                .setId(packedId)
                .setOriginalMessage(packedMessage)
                .setBoundedContextName(boundedContextName)
                .build();
        return result;
    }
}
