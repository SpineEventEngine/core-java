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

import io.spine.core.BoundedContextName;

/**
 * Observes the registrations of the sources of the external messages by listening
 * to {@code ExternalMessagesSourceAvailable} signals, and passes
 * this information on to those domestic routines, which might need to consume the messages
 * just made available.
 */
final class ObserveNewRegistrations extends AbstractChannelObserver {

    private final InternalNeedsBroadcast broadcast;

    /**
     * Creates a new observer for the passed context name.
     *
     * <p>Upon receiving the message via the observed channel,
     * passes the shout to the specified {@code InternalNeedsBroadcast}.
     *
     * @param context
     *         the name of the bounded context, in scope of which this observer acts
     * @param broadcast
     *         serves to reach out to those who want to know that some new sources
     *         of external messages became available
     */
    ObserveNewRegistrations(BoundedContextName context, InternalNeedsBroadcast broadcast) {
        super(context, ExternalMessagesSourceAvailable.class);
        this.broadcast = broadcast;
    }

    @Override
    protected void handle(ExternalMessage message) {
        broadcast.send();
    }
}
