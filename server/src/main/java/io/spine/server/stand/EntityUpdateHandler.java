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
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.client.EntityStateUpdate;
import io.spine.client.EntityUpdates;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.TargetFilters;
import io.spine.core.Responses;
import io.spine.protobuf.AnyPacker;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.EntityStateChanged;

import java.util.Optional;

/**
 * The update handler of {@code Subscription}s for {@code Entity} state updates.
 */
class EntityUpdateHandler extends UpdateHandler {

    EntityUpdateHandler(Subscription subscription) {
        super(subscription);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Entity state often experience several changes down the lifecycle path. Some entities which
     * initially passed the subscription criteria, stop passing those as their state updates.
     *
     * <p>In this case a special {@code SubscriptionUpdate} is emitted. Its
     * {@linkplain SubscriptionUpdate#getEntityUpdates() EntityUpdates} will have
     * {@link EntityStateUpdate#getNotMatchingAnymore() EntityStateUpdate.getNotMatchingAnymore()}
     * set to {@code true}.
     *
     * <p>In all other cases the {@code EntityUpdates} holds an updated
     * {@link EntityStateUpdate#getState() Entity state}.
     */
    @Override
    Optional<SubscriptionUpdate> detectUpdate(EventEnvelope event) {
        SubscriptionUpdate result = null;

        if (isTypeMatching(event)) {
            if (includeAll()) {
                result = newStateUpdate(event);
            } else {
                if (isIdMatching(event)) {
                    if (isStateMatching(newStateFrom(event))) {
                        result = newStateUpdate(event);
                    } else if (isStateMatching(oldStateFrom(event))) {
                        result = notMatchingAnymore(event);
                    }
                }
            }
        }
        return Optional.ofNullable(result);
    }

    @Override
    protected boolean isTypeMatching(EventEnvelope event) {
        String expectedTypeUrl = target().getType();
        String actualTypeUrl = asEntityEvent(event).getEntity()
                                                   .getTypeUrl();
        return expectedTypeUrl.equals(actualTypeUrl);
    }

    @Override
    protected Any extractId(EventEnvelope event) {
        Any entityId = asEntityEvent(event).getEntity()
                                           .getId();
        return entityId;
    }

    private static EntityStateChanged asEntityEvent(EventEnvelope event) {
        return (EntityStateChanged) event.message();
    }

    /**
     * Checks if the event message matches the subscription filters.
     */
    private boolean isStateMatching(Message state) {
        TargetFilters filters = target().getFilters();
        boolean result = filters
                .getFilterList()
                .stream()
                .allMatch(f -> checkPasses(state, f));
        return result;
    }

    private static Message newStateFrom(EventEnvelope event) {
        EntityStateChanged eventMessage = asEntityEvent(event);
        Any newState = eventMessage.getNewState();
        Message result = AnyPacker.unpack(newState);
        return result;
    }

    private static Message oldStateFrom(EventEnvelope event) {
        EntityStateChanged eventMessage = asEntityEvent(event);
        Any newState = eventMessage.getNewState();
        Message result = AnyPacker.unpack(newState);
        return result;
    }

    private static Any packId(EntityStateChanged event) {
        EntityId entityId = EntityId
                .newBuilder()
                .setId(event.getEntity()
                            .getId())
                .build();
        return Identifier.pack(entityId);
    }

    private SubscriptionUpdate newStateUpdate(EventEnvelope event) {
        EntityStateChanged theEvent = asEntityEvent(event);
        Any packedId = packId(theEvent);
        Any packedState = theEvent.getNewState();
        EntityStateUpdate stateUpdate = EntityStateUpdate
                .newBuilder()
                .setId(packedId)
                .setState(packedState)
                .vBuild();
        return toSubscriptionUpdate(stateUpdate);
    }

    private SubscriptionUpdate notMatchingAnymore(EventEnvelope event) {
        EntityStateChanged theEvent = asEntityEvent(event);
        Any packedId = packId(theEvent);
        EntityStateUpdate stateUpdate = EntityStateUpdate
                .newBuilder()
                .setId(packedId)
                .setNotMatchingAnymore(true)
                .vBuild();
        return toSubscriptionUpdate(stateUpdate);
    }

    private SubscriptionUpdate toSubscriptionUpdate(EntityStateUpdate stateUpdate) {
        EntityUpdates updates = EntityUpdates
                .newBuilder()
                .addUpdate(stateUpdate)
                .vBuild();
        SubscriptionUpdate result = SubscriptionUpdate
                .newBuilder()
                .setSubscription(subscription())
                .setResponse(Responses.ok())
                .setEntityUpdates(updates)
                .vBuild();
        return result;
    }
}
