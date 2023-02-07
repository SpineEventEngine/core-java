/*
 * Copyright 2022, TeamDev. All rights reserved.
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
import io.spine.client.EntityStateUpdate;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.protobuf.AnyPacker;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.event.EntityStateChanged;

import java.util.Optional;

/**
 * The update handler of {@code Subscription}s for changes in {@code Entity} state.
 *
 * <p>This handler only processes the changes in non-deleted and non-archived
 * {@code Entity} instances.
 */
final class EntityChangeHandler extends AbstractEntityUpdateHandler {

    EntityChangeHandler(Subscription subscription) {
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

    private static EntityStateChanged asEntityStateChanged(EventEnvelope event) {
        return (EntityStateChanged) event.message();
    }

    private static EntityState newStateFrom(EventEnvelope event) {
        EntityStateChanged eventMessage = asEntityStateChanged(event);
        Any newState = eventMessage.getNewState();
        EntityState result = (EntityState) AnyPacker.unpack(newState);
        return result;
    }

    private static EntityState oldStateFrom(EventEnvelope event) {
        EntityStateChanged eventMessage = asEntityStateChanged(event);
        Any newState = eventMessage.getOldState();
        EntityState result = (EntityState) AnyPacker.unpack(newState);
        return result;
    }

    private SubscriptionUpdate newStateUpdate(EventEnvelope event) {
        EntityStateChanged theEvent = asEntityStateChanged(event);
        Any packedId = packId(theEvent);
        Any packedState = theEvent.getNewState();
        EntityStateUpdate stateUpdate = EntityStateUpdate
                .newBuilder()
                .setId(packedId)
                .setState(packedState)
                .vBuild();
        return toSubscriptionUpdate(stateUpdate);
    }
}
