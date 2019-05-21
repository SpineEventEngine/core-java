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

package io.spine.server.stand;

import io.spine.client.EventUpdates;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.Event;
import io.spine.core.Responses;
import io.spine.server.type.EventEnvelope;

/**
 * Updates an event subscription based on the incoming event.
 */
final class EventSubscriptionCallback extends SubscriptionCallback {

    EventSubscriptionCallback(Subscription subscription) {
        super(subscription);
    }

    /**
     * Creates a subscription update with a single {@link Event} obtained from the envelope.
     */
    @Override
    protected SubscriptionUpdate createSubscriptionUpdate(EventEnvelope event) {
        EventUpdates updates = extractEventUpdates(event);
        SubscriptionUpdate result = SubscriptionUpdate
                .newBuilder()
                .setSubscription(subscription())
                .setResponse(Responses.ok())
                .setEventUpdates(updates)
                .build();
        return result;
    }

    private static EventUpdates extractEventUpdates(EventEnvelope event) {
        Event eventObject = event.outerObject();
        EventUpdates result = EventUpdates
                .newBuilder()
                .addEvents(eventObject)
                .build();
        return result;
    }
}
