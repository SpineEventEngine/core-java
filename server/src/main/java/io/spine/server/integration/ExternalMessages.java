/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for working with {@linkplain ExternalMessage external messages}.
 */
@Internal
final class ExternalMessages {

    /** Prevents instantiation of this utility class. */
    private ExternalMessages() {
    }

    /**
     * Wraps the instance of {@link Event} into an {@code ExternalMessage}.
     *
     * @param event
     *         the event to wrap
     * @param origin
     *         the name of the Bounded Context in which the event was created
     * @return the external message wrapping the given event
     */
    static ExternalMessage of(Event event, BoundedContextName origin) {
        checkNotNull(event);
        checkNotNull(origin);

        var result = of(event.getId(), event, origin);
        return result;
    }

    /**
     * Wraps the instance of {@link ExternalEventsWanted} into an {@code ExternalMessage}.
     *
     * @param request
     *         the request to wrap
     * @param origin
     *         the name of Bounded Context in which the request was created
     * @return the external message wrapping the given request
     */
    static ExternalMessage of(ExternalEventsWanted request, BoundedContextName origin) {
        checkNotNull(request);
        checkNotNull(origin);

        var result = of(generateId(), request, origin);
        return result;
    }

    /**
     * Wraps the instance of {@link BoundedContextOnline} into an {@code ExternalMessage}.
     *
     * @param notification
     *         the notification to wrap
     * @return the external message wrapping the given notification
     */
    static ExternalMessage of(BoundedContextOnline notification) {
        checkNotNull(notification);
        var result = of(generateId(), notification, notification.getContext());
        return result;
    }

    private static StringValue generateId() {
        var result = StringValue.of(Identifier.newUuid());
        return result;
    }

    private static ExternalMessage of(Message id, Message message, BoundedContextName origin) {
        var packedId = Identifier.pack(id);
        var packedMessage = AnyPacker.pack(message);

        var result = ExternalMessage
                .newBuilder()
                .setId(packedId)
                .setOriginalMessage(packedMessage)
                .setBoundedContextName(origin)
                .build();
        return result;
    }
}
