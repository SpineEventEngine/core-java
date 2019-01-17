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
import io.spine.client.EntityId;
import io.spine.client.Subscription;
import io.spine.core.EventEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.system.server.EntityStateChanged;
import io.spine.type.TypeUrl;

final class EntitySubscriptionMatcher extends SubscriptionMatcher {

    EntitySubscriptionMatcher(Subscription subscription) {
        super(subscription);
    }

    @Override
    protected TypeUrl getTypeToCheck(EventEnvelope event) {
        EntityStateChanged eventMessage = toEventMessage(event);
        String type = eventMessage.getId()
                                  .getTypeUrl();
        TypeUrl result = TypeUrl.parse(type);
        return result;
    }

    @Override
    protected Any getIdToCheck(EventEnvelope event) {
        EntityStateChanged eventMessage = toEventMessage(event);
        EntityId entityId = eventMessage.getId()
                                        .getEntityId();
        Any result = entityId.getId();
        return result;
    }

    @Override
    protected Message getStateToCheck(EventEnvelope event) {
        EntityStateChanged eventMessage = toEventMessage(event);
        Any newState = eventMessage.getNewState();
        Message result = AnyPacker.unpack(newState);
        return result;
    }

    private static EntityStateChanged toEventMessage(EventEnvelope event) {
        EventMessage message = event.getMessage();
        EntityStateChanged result = (EntityStateChanged) message;
        return result;
    }
}
