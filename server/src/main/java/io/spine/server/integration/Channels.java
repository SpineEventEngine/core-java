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

import io.spine.server.transport.ChannelId;
import io.spine.type.TypeUrl;

import static io.spine.server.transport.MessageChannel.channelIdFor;

/**
 * Utility for creation of predefined channels used by {@link IntegrationBroker}.
 */
final class Channels {

    private static final ChannelId STATUS_CHANNEL = channelIdFor(
            TypeUrl.of(BoundedContextOnline.class)
    );
    private static final ChannelId EVENT_NEEDS_CHANNEL = channelIdFor(
            TypeUrl.of(ExternalEventsWanted.class)
    );

    /**
     * Prevents this utility from instantiation.
     */
    private Channels() {
    }

    /**
     * Returns the ID of the channel used to exchange the "online" statuses
     * of the Bounded Contexts connected.
     */
    static ChannelId statuses() {
        return STATUS_CHANNEL;
    }

    /**
     * Returns the ID of the channel used to exchange the types of the domain events
     * each Bounded Context would like to receive as "external" events.
     */
    static ChannelId eventsWanted() {
        return EVENT_NEEDS_CHANNEL;
    }
}
