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
import io.spine.client.EntityId;
import io.spine.client.EntityStateUpdate;
import io.spine.client.EntityStateUpdateVBuilder;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.SubscriptionUpdateVBuilder;
import io.spine.core.EventEnvelope;
import io.spine.core.Responses;
import io.spine.protobuf.AnyPacker;
import io.spine.system.server.EntityStateChanged;

final class EntitySubscriptionRunner extends SubscriptionCallbackRunner {

    EntitySubscriptionRunner(Subscription subscription) {
        super(subscription);
    }

    @Override
    protected SubscriptionUpdate buildSubscriptionUpdate(EventEnvelope event) {
        EntityStateChanged theEvent = (EntityStateChanged) event.getMessage();
        EntityStateUpdate stateUpdate = toStateUpdate(theEvent);
        SubscriptionUpdate result = SubscriptionUpdateVBuilder
                .newBuilder()
                .setSubscription(subscription())
                .setResponse(Responses.ok())
                .addEntityStateUpdates(stateUpdate)
                .build();
        return result;
    }

    private static EntityStateUpdate toStateUpdate(EntityStateChanged event) {
        EntityId entityId = event.getId()
                                 .getEntityId();
        Any packedEntityId = AnyPacker.pack(entityId);
        Any packedEntityState = event.getNewState();
        EntityStateUpdate result = EntityStateUpdateVBuilder
                .newBuilder()
                .setId(packedEntityId)
                .setState(packedEntityState)
                .build();
        return result;
    }
}
