/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.rejection.StandardRejections;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.event.EntProjectCreated;
import io.spine.test.entity.event.EntProjectStarted;
import io.spine.test.entity.event.EntTaskAdded;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static io.spine.base.Identifier.newUuid;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`NoOpEventFilter` should")
class NoOpEventFilterTest {

    private final EventFilter filter = EventFilter.allowAll();

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(NoOpEventFilterTest.class);

    @Test
    @DisplayName("be singleton")
    void beSingleton() {
        assertSame(NoOpEventFilter.INSTANCE, filter);
    }

    @Test
    @DisplayName("allow any event")
    void allowAny() {
        events().map(Event::enclosedMessage)
                .forEach(event -> {
            Optional<? extends Message> filtered = this.filter.filter(event);
            assertTrue(filtered.isPresent());
            assertSame(event, filtered.get());
        });
    }

    @Test
    @DisplayName("allow any bulk of events")
    void allowAnyBulk() {
        var events = events().collect(toList());
        var filtered = filter.filter(events);
        assertEquals(events, filtered);
    }

    private static Stream<Event> events() {
        var projectId = ProjectId
                .newBuilder()
                .setId(newUuid())
                .build();
        var result =
                Stream.<EventMessage>of(
                        EntProjectCreated.newBuilder()
                                .setProjectId(projectId)
                                .build(),
                        EntProjectStarted.newBuilder()
                                .setProjectId(projectId)
                                .build(),
                        EntTaskAdded.newBuilder()
                                .setProjectId(projectId)
                                .build(),
                        StandardRejections.EntityAlreadyArchived.newBuilder()
                                .setEntityId(AnyPacker.pack(projectId))
                                .build())
                      .map(eventFactory::createEvent);
        return result;
    }
}
