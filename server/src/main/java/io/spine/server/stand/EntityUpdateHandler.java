/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.stand;

import com.google.protobuf.Any;
import io.spine.base.EntityState;
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
final class EntityUpdateHandler extends UpdateHandler {

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
     * {@link EntityStateUpdate#getNoLongerMatching() EntityStateUpdate.getNoLongerMatching()}
     * set to {@code true}.
     *
     * <p>In all other cases the {@code EntityUpdates} holds an updated
     * {@link EntityStateUpdate#getState() Entity state}.
     */
    @Override
    Optional<SubscriptionUpdate> detectUpdate(EventEnvelope event) {
        SubscriptionUpdate result = null;

        if (typeMatches(event)) {
            if (includeAll()) {
                result = newStateUpdate(event);
            } else {
                if (idMatches(event)) {
                    if (stateMatches(newStateFrom(event))) {
                        result = newStateUpdate(event);
                    } else if (stateMatches(oldStateFrom(event))) {
                        result = noLongerMatching(event);
                    }
                }
            }
        }
        return Optional.ofNullable(result);
    }

    @Override
    protected boolean typeMatches(EventEnvelope event) {
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
     * Checks if the entity state matches the subscription filters.
     */
    private boolean stateMatches(EntityState state) {
        TargetFilters filters = target().getFilters();
        boolean result = filters
                .getFilterList()
                .stream()
                .allMatch(f -> f.test(state));
        return result;
    }

    private static EntityState newStateFrom(EventEnvelope event) {
        EntityStateChanged eventMessage = asEntityEvent(event);
        Any newState = eventMessage.getNewState();
        EntityState result = (EntityState) AnyPacker.unpack(newState);
        return result;
    }

    private static EntityState oldStateFrom(EventEnvelope event) {
        EntityStateChanged eventMessage = asEntityEvent(event);
        Any newState = eventMessage.getOldState();
        EntityState result = (EntityState) AnyPacker.unpack(newState);
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

    private SubscriptionUpdate noLongerMatching(EventEnvelope event) {
        EntityStateChanged theEvent = asEntityEvent(event);
        Any packedId = packId(theEvent);
        EntityStateUpdate stateUpdate = EntityStateUpdate
                .newBuilder()
                .setId(packedId)
                .setNoLongerMatching(true)
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
