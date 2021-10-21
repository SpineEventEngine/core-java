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
final class InternalNeedsBroadcast {

    private final BoundedContextName contextName;
    private final Publisher needsPublisher;

    private ImmutableSet<ExternalMessageType> currentNeeds = ImmutableSet.of();

    InternalNeedsBroadcast(BoundedContextName contextName, Publisher publisher) {
        this.contextName = checkNotNull(contextName);
        this.needsPublisher = checkNotNull(publisher);
    }

    /**
     * Notifies other Bounded Contexts about a change in the requested messages.
     *
     * <p>If the given {@code types} are the same as previous ones, the request is not sent.
     *
     * @param needs
     *         types of external messages that are requested
     */
    synchronized void onNeedsChange(ImmutableSet<ExternalMessageType> needs) {
        checkNotNull(needs);

        if (currentNeeds.equals(needs)) {
            return;
        }

        currentNeeds = needs;
        send();
    }

    /**
     * Notifies other Bounded contexts about current needs.
     */
    synchronized void send() {
        RequestForExternalMessages request = RequestForExternalMessages
                .newBuilder()
                .addAllRequestedMessageType(currentNeeds)
                .vBuild();
        ExternalMessage externalMessage = ExternalMessages.of(request, contextName);
        needsPublisher.publish(pack(newUuid()), externalMessage);
    }
}
