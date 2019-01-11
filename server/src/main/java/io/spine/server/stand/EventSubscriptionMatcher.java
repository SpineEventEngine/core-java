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
import io.spine.client.Target;
import io.spine.core.EventEnvelope;
import io.spine.core.EventId;
import io.spine.type.TypeUrl;

final class EventSubscriptionMatcher extends SubscriptionMatcher {

    private EventSubscriptionMatcher(Target target) {
        super(target);
    }

    static EventSubscriptionMatcher createFor(Subscription subscription) {
        Target target = subscription.getTopic()
                                    .getTarget();
        return new EventSubscriptionMatcher(target);
    }

    @Override
    protected TypeUrl getCheckedType(EventEnvelope event) {
        TypeUrl result = TypeUrl.of(event.getMessage());
        return result;
    }

    @Override
    protected Any getCheckedId(EventEnvelope event) {
        EventId eventId = event.getId();
        Any result = Identifier.pack(eventId);
        return result;
    }

    @Override
    protected Message getCheckedMessage(EventEnvelope event) {
        EventMessage result = event.getMessage();
        return result;
    }
}
