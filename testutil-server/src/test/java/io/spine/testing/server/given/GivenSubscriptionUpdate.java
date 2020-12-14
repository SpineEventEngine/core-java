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

package io.spine.testing.server.given;

import io.spine.client.EntityStateUpdate;
import io.spine.client.EntityUpdates;
import io.spine.client.EventUpdates;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.Event;
import io.spine.testing.server.blackbox.BbProject;
import io.spine.testing.server.blackbox.BbProjectId;
import io.spine.testing.server.blackbox.event.BbProjectCreated;

import static io.spine.protobuf.AnyPacker.pack;

public final class GivenSubscriptionUpdate {

    private GivenSubscriptionUpdate() {
    }

    public static SubscriptionUpdate withTwoEntities() {
        BbProject state1 = BbProject
                .newBuilder()
                .setId(BbProjectId.generate())
                .build();
        BbProject state2 = BbProject
                .newBuilder()
                .setId(BbProjectId.generate())
                .build();
        EntityStateUpdate stateUpdate1 = EntityStateUpdate
                .newBuilder()
                .setState(pack(state1))
                .build();
        EntityStateUpdate stateUpdate2 = EntityStateUpdate
                .newBuilder()
                .setState(pack(state2))
                .build();
        EntityUpdates updates = EntityUpdates
                .newBuilder()
                .addUpdate(stateUpdate1)
                .addUpdate(stateUpdate2)
                .build();
        SubscriptionUpdate update = SubscriptionUpdate
                .newBuilder()
                .setEntityUpdates(updates)
                .build();
        return update;
    }

    public static SubscriptionUpdate withTwoEvents() {
        BbProjectCreated eventMessage1 = BbProjectCreated
                .newBuilder()
                .setProjectId(BbProjectId.generate())
                .build();
        BbProjectCreated eventMessage2 = BbProjectCreated
                .newBuilder()
                .setProjectId(BbProjectId.generate())
                .build();
        Event event1 = Event
                .newBuilder()
                .setMessage(pack(eventMessage1))
                .build();
        Event event2 = Event
                .newBuilder()
                .setMessage(pack(eventMessage2))
                .build();
        EventUpdates updates = EventUpdates
                .newBuilder()
                .addEvent(event1)
                .addEvent(event2)
                .build();
        SubscriptionUpdate update = SubscriptionUpdate
                .newBuilder()
                .setEventUpdates(updates)
                .build();
        return update;
    }

    public static SubscriptionUpdate empty() {
        SubscriptionUpdate update = SubscriptionUpdate
                .newBuilder()
                .build();
        return update;
    }
}
