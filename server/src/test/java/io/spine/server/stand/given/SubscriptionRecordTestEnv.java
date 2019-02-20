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

package io.spine.server.stand.given;

import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.client.Subscription;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.client.Topic;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.protobuf.AnyPacker;
import io.spine.protobuf.TypeConverter;
import io.spine.server.type.EventEnvelope;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityStateChanged;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.commandservice.customer.Customer;
import io.spine.test.event.ProjectCreated;
import io.spine.type.TypeUrl;

import java.util.Collections;

public final class SubscriptionRecordTestEnv {

    public static final TypeUrl TYPE = TypeUrl.of(Project.class);
    public static final TypeUrl OTHER_TYPE = TypeUrl.of(Customer.class);

    /** Prevents instantiation of this utility class. */
    private SubscriptionRecordTestEnv() {
    }

    public static EventEnvelope stateChangedEnvelope(ProjectId id, Project newState) {
        return stateChangedEnvelope(id, newState, TYPE);
    }

    public static EventEnvelope
    stateChangedEnvelope(ProjectId id, Project newState, TypeUrl type) {
        EntityStateChanged eventMessage = entityStateChanged(id, newState, type);
        Any packedMessage = TypeConverter.toAny(eventMessage);
        Event event = Event
                .newBuilder()
                .setMessage(packedMessage)
                .build();
        EventEnvelope result = EventEnvelope.of(event);
        return result;
    }

    private static EntityStateChanged
    entityStateChanged(ProjectId id, Project newState, TypeUrl type) {
        Any packedId = Identifier.pack(id);
        EntityId entityId = EntityId
                .newBuilder()
                .setId(packedId)
                .build();
        EntityHistoryId entityHistoryId = EntityHistoryId
                .newBuilder()
                .setTypeUrl(type.value())
                .setEntityId(entityId)
                .build();
        Any packedMatchingState = TypeConverter.toAny(newState);
        EntityStateChanged result = EntityStateChanged
                .newBuilder()
                .setId(entityHistoryId)
                .setNewState(packedMatchingState)
                .build();
        return result;
    }

    public static EventEnvelope projectCreatedEnvelope(EventId eventId) {
        return projectCreatedEnvelope(eventId, ProjectCreated.getDefaultInstance());
    }

    public static EventEnvelope
    projectCreatedEnvelope(EventId eventId, ProjectCreated eventMessage) {
        Any packedMessage = AnyPacker.pack(eventMessage);
        Event event = Event
                .newBuilder()
                .setId(eventId)
                .setMessage(packedMessage)
                .build();
        EventEnvelope result = EventEnvelope.of(event);
        return result;
    }

    public static Subscription subscription() {
        Target target = target();
        Subscription subscription = subscription(target);
        return subscription;
    }

    public static Subscription subscription(ProjectId targetId) {
        Target target = target(targetId);
        Subscription subscription = subscription(target);
        return subscription;
    }

    public static Subscription subscription(Target target) {
        Topic topic = Topic
                .newBuilder()
                .setTarget(target)
                .build();
        Subscription result = Subscription
                .newBuilder()
                .setTopic(topic)
                .build();
        return result;
    }

    private static Target target() {
        Target target = Targets.allOf(Project.class);
        return target;
    }

    private static Target target(ProjectId targetId) {
        Target target = Targets.someOf(Project.class, Collections.singleton(targetId));
        return target;
    }
}
