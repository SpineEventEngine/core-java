/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import io.spine.core.BoundedContextName;
import io.spine.server.transport.Publisher;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;

/**
 * A client of the {@code RequestForExternalMessages} {@link Publisher}.
 *
 * <p>Posts the updates on the requested messages.
 */
final class ConfigurationBroadcast {

    private final BoundedContextName contextName;
    private final Publisher needsPublisher;

    private ImmutableSet<ExternalMessageType> requestedTypes = ImmutableSet.of();

    ConfigurationBroadcast(BoundedContextName contextName, Publisher publisher) {
        this.contextName = checkNotNull(contextName);
        this.needsPublisher = checkNotNull(publisher);
    }

    /**
     * Notifies other Bounded contexts about a change in the requested messages.
     *
     * <p>If the given {@code types} are the same as previous ones, the request is not sent.
     *
     * @param types
     *         the types
     */
    synchronized void onTypesChanged(ImmutableSet<ExternalMessageType> types) {
        checkNotNull(types);
        if (!requestedTypes.equals(types)) {
            requestedTypes = types;
            send();
        }
    }

    /**
     * Notifies other Bounded contexts about current requested messages.
     */
    synchronized void send() {
        RequestForExternalMessages request = RequestForExternalMessages
                .newBuilder()
                .addAllRequestedMessageType(requestedTypes)
                .buildPartial();
        ExternalMessage externalMessage = ExternalMessages.of(request, contextName);
        needsPublisher.publish(pack(newUuid()), externalMessage);
    }
}
