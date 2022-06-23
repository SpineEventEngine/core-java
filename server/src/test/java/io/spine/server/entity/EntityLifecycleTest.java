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

package io.spine.server.entity;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.EventId;
import io.spine.core.MessageId;
import io.spine.core.Origin;
import io.spine.protobuf.AnyPacker;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.entity.given.entity.TestAggregate;
import io.spine.server.entity.model.EntityClass;
import io.spine.system.server.MemoizedSystemMessage;
import io.spine.system.server.MemoizingWriteSide;
import io.spine.system.server.NoOpSystemWriteSide;
import io.spine.system.server.event.EntityCreated;
import io.spine.system.server.event.EntityStateChanged;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.option.EntityOption.Kind.ENTITY;
import static io.spine.protobuf.AnyPacker.pack;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("EntityLifecycle should")
class EntityLifecycleTest {

    private static final EntityClass<?> TEST_ENTITY_CLASS =
            AggregateClass.asAggregateClass(TestAggregate.class);

    @Test
    @DisplayName("not allow nulls in Builder")
    void nullTolerance() {
        new NullPointerTester()
                .setDefault(EntityClass.class, TEST_ENTITY_CLASS)
                .setDefault(Object.class, 42)
                .testInstanceMethods(EntityLifecycle.newBuilder(), PACKAGE);
    }

    @Test
    @DisplayName("be created with a default EventFilter")
    void allowCreationWithoutEventFilter() {
        EntityLifecycle lifecycle = EntityLifecycle
                .newBuilder()
                .setSystemWriteSide(NoOpSystemWriteSide.INSTANCE)
                .setEntityType(TEST_ENTITY_CLASS)
                .setEntityId("sample-id")
                .build();
        assertNotNull(lifecycle);
    }

    @Test
    @DisplayName("filter system events before posting")
    void filterEventsBeforePosting() {
        EventFilter filter = event -> {
            boolean stateChanged = event instanceof EntityStateChanged;
            return stateChanged
                   ? Optional.empty()
                   : Optional.of(event);
        };
        MemoizingWriteSide writeSide = MemoizingWriteSide.singleTenant();
        int entityId = 42;
        EntityLifecycle lifecycle = EntityLifecycle
                .newBuilder()
                .setEntityId(entityId)
                .setEntityType(TEST_ENTITY_CLASS)
                .setSystemWriteSide(writeSide)
                .setEventFilter(filter)
                .build();
        lifecycle.onEntityCreated(ENTITY);
        MemoizedSystemMessage lastSeenEvent = writeSide.lastSeenEvent();
        assertThat(lastSeenEvent.message())
                .isInstanceOf(EntityCreated.class);

        EntityRecord previousRecord = EntityRecord
                .newBuilder()
                .setEntityId(Identifier.pack(entityId))
                .setState(pack(Timestamp.getDefaultInstance()))
                .build();
        EntityRecord newRecord = previousRecord
                .toBuilder()
                .setState(pack(Time.currentTime()))
                .build();
        EntityRecordChange change = EntityRecordChange
                .newBuilder()
                .setPreviousValue(previousRecord)
                .setNewValue(newRecord)
                .build();
        EventId causeEventId = EventId.newBuilder()
                            .setValue("test event ID")
                            .build();
        MessageId causeMessage = MessageId
                .newBuilder()
                .setId(AnyPacker.pack(causeEventId))
                .setTypeUrl("example.com/test.Event")
                .buildPartial();
        lifecycle.onStateChanged(change,
                                 ImmutableSet.of(causeMessage),
                                 Origin.getDefaultInstance());
        assertSame(lastSeenEvent, writeSide.lastSeenEvent());
    }
}
