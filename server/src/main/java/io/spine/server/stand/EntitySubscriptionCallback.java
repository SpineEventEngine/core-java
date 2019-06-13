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
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.client.EntityStateUpdate;
import io.spine.client.EntityUpdates;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.Responses;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.EntityStateChanged;

/**
 * Updates an entity subscription based on the incoming {@link EntityStateChanged} event.
 */
final class EntitySubscriptionCallback extends SubscriptionCallback {

    EntitySubscriptionCallback(Subscription subscription) {
        super(subscription);
    }

    /**
     * Creates a subscription update with a single {@link EntityStateUpdate} based on the
     * information in the event.
     */
    @Override
    protected SubscriptionUpdate createSubscriptionUpdate(EventEnvelope event) {
        EntityStateChanged theEvent = (EntityStateChanged) event.message();
        EntityUpdates updates = extractEntityUpdates(theEvent);
        SubscriptionUpdate result = SubscriptionUpdate
                .newBuilder()
                .setSubscription(subscription())
                .setResponse(Responses.ok())
                .setEntityUpdates(updates)
                .vBuild();
        return result;
    }

    private static EntityUpdates extractEntityUpdates(EntityStateChanged event) {
        EntityId entityId = event.getId()
                                 .getEntityId();
        Any packedId = Identifier.pack(entityId);
        Any packedState = event.getNewState();
        EntityStateUpdate stateUpdate = EntityStateUpdate
                .newBuilder()
                .setId(packedId)
                .setState(packedState)
                .vBuild();
        EntityUpdates result = EntityUpdates
                .newBuilder()
                .addUpdate(stateUpdate)
                .vBuild();
        return result;
    }
}
