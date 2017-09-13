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
import io.spine.core.BoundedContextId;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for working with {@linkplain ExternalMessage external messages}.
 *
 * @author Alex Tymchenko
 */
class ExternalMessages {

    private ExternalMessages() {
        // prevent an instantiation of this utility class.
    }

    /**
     * Wraps the instance of {@link Event} into an {@code ExternalMessage}.
     *
     * @param event            the event to wrap
     * @param boundedContextId the bounded context ID in which the event was created
     * @return the external message wrapping the given event
     */
    static ExternalMessage of(Event event, BoundedContextId boundedContextId) {
        checkNotNull(event);

        final ExternalMessage result = of(event.getId(), event, boundedContextId);
        return result;
    }

    /**
     * Wraps the instance of {@link Rejection} into an {@code ExternalMessage}.
     *
     * @param rejection        the rejection to wrap
     * @param boundedContextId the bounded context ID in which the rejection was created
     * @return the external message wrapping the given rejection
     */
    static ExternalMessage of(Rejection rejection, BoundedContextId boundedContextId) {
        checkNotNull(rejection);

        final ExternalMessage result = of(rejection.getId(), rejection, boundedContextId);
        return result;
    }

    /**
     * Wraps the instance of {@link RequestForExternalMessages} into an {@code ExternalMessage}.
     *
     * @param request          the request to wrap
     * @param boundedContextId the bounded context ID in which the request was created
     * @return the external message wrapping the given request
     */
    static ExternalMessage of(RequestForExternalMessages request,
                              BoundedContextId boundedContextId) {
        checkNotNull(request);
        final String idString = Identifier.newUuid();
        final ExternalMessage result = of(StringValue.newBuilder()
                                                     .setValue(idString)
                                                     .build(),
                                          request,
                                          boundedContextId);
        return result;
    }

    private static ExternalMessage of(Message messageId,
                                      Message message,
                                      BoundedContextId boundedContextId) {
        final Any packedId = Identifier.pack(messageId);
        final Any packedMessage = AnyPacker.pack(message);

        return ExternalMessage.newBuilder()
                              .setId(packedId)
                              .setOriginalMessage(packedMessage)
                              .setBoundedContextId(boundedContextId)
                              .build();
    }
}
