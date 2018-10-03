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
import io.spine.server.delivery.ShardedMessage;
import io.spine.server.delivery.ShardedMessageId;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Utilities for working with {@linkplain ExternalMessage external messages}.
 *
 * @author Alex Tymchenko
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

    /**
     * Wraps the instance of {@link ShardedMessage} into an {@code ExternalMessage}.
     *
     * @param message the message to wrap
     * @param origin  the name of bounded context in which the request message created
     * @return the external message wrapping the given message
     */
    public static ExternalMessage of(ShardedMessage message, BoundedContextName origin) {
        checkNotNull(message);
        checkNotNull(origin);

        ShardedMessageId id = message.getId();
        ExternalMessage result = of(id, message, origin);
        return result;
    }

    /**
     * Unpacks the given {@code ExternalMessage} into a {@link ShardedMessage}.
     *
     * <p>Callees of this method should ensure that the given external message indeed wraps
     * an instance of {@code ShardedMessage}, before calling this method.
     * A {@link ClassCastException} is thrown otherwise.
     *
     * @param value the value to unpack
     * @return the unpacked value
     */
    public static ShardedMessage asShardedMessage(ExternalMessage value) {
        checkNotNull(value);
        Any originalMessage = value.getOriginalMessage();
        ShardedMessage result = unpack(originalMessage, ShardedMessage.class);
        return result;
    }

    private static ExternalMessage of(Message messageId,
                                      Message message,
                                      BoundedContextName boundedContextName) {
        Any packedId = Identifier.pack(messageId);
        Any packedMessage = AnyPacker.pack(message);

        ExternalMessage result = ExternalMessage.newBuilder()
                                                .setId(packedId)
                                                .setOriginalMessage(packedMessage)
                                                .setBoundedContextName(boundedContextName)
                                                .build();
        return result;
    }
}
