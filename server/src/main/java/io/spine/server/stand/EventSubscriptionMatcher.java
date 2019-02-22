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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.client.Subscription;
import io.spine.core.EventId;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

/**
 * Matches the event subscription against an incoming event.
 */
final class EventSubscriptionMatcher extends SubscriptionMatcher {

    EventSubscriptionMatcher(Subscription subscription) {
        super(subscription);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the type of the event itself.
     */
    @Override
    protected TypeUrl extractType(EventEnvelope event) {
        TypeUrl result = TypeUrl.of(event.message());
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the event ID.
     */
    @Override
    protected Any extractId(EventEnvelope event) {
        EventId eventId = event.getId();
        Any result = Identifier.pack(eventId);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the event message itself.
     */
    @Override
    protected Message extractMessage(EventEnvelope event) {
        EventMessage result = event.message();
        return result;
    }
}
