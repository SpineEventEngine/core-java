/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.integration;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.TenantId;
import io.spine.server.command.TestEventFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.base.Identifier.newUuid;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
@DisplayName("Emitted Events should")
class EmittedEventsTest {

    @Test
    @DisplayName("return proper total count")
    void count() {
        EmittedEvents noEmittedEvents = new EmittedEvents(newArrayList());
        assertEquals(0, noEmittedEvents.count());

        EmittedEvents emittedEvent = new EmittedEvents(
                events(1, EmittedEventsTest::projectCreated));
        assertEquals(1, emittedEvent.count());

        EmittedEvents twoEmittedEvents = new EmittedEvents(
                events(2, EmittedEventsTest::projectCreated));
        assertEquals(2, twoEmittedEvents.count());

        EmittedEvents threeEmittedEvents = new EmittedEvents(
                events(3, EmittedEventsTest::projectCreated));
        assertEquals(3, threeEmittedEvents.count());
    }

    private static List<Event> events(int count, Supplier<Message> messageSupplier) {
        List<Event> events = newArrayList();
        for (int i = 0; i < count; i++) {
            events.add(event(messageSupplier.get()));
        }
        return events;
    }

    private static Event event(Message domainEvent) {
        TestEventFactory factory = eventFactory(requestFactory(newTenantId()));
        return factory.createEvent(domainEvent);
    }

    private static TenantId newTenantId() {
        return TenantId.newBuilder()
                       .setValue(newUuid())
                       .build();
    }

    private static TestEventFactory eventFactory(TestActorRequestFactory requestFactory) {
        return TestEventFactory.newInstance(requestFactory);
    }

    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(BlackBoxBoundedContext.class, tenantId);
    }

    private static IntCreateProject createProject() {
        return IntCreateProject.newBuilder()
                               .setProjectId(newProjectId())
                               .build();
    }

    private static IntAddTask addTask() {
        return IntAddTask.newBuilder()
                         .setProjectId(newProjectId())
                         .build();

    }

    private static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    private static IntProjectCreated projectCreated() {
        return IntProjectCreated.newBuilder()
                                .setProjectId(newProjectId())
                                .build();
    }

    private static IntTaskAdded taskAdded() {
        return IntTaskAdded.newBuilder()
                           .setProjectId(newProjectId())
                           .build();

    }

    @Test
    @DisplayName("return proper total count for event message class")
    void countMessageClass() {
        List<Event> events = asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        );
        EmittedEvents emittedEvents = new EmittedEvents(events);
        assertEquals(0, emittedEvents.count(IntProjectStarted.class));
        assertEquals(1, emittedEvents.count(IntProjectCreated.class));
        assertEquals(2, emittedEvents.count(IntTaskAdded.class));
    }

    @Test
    @DisplayName("return proper total count for event class")
    void countEventClass() {
        List<Event> events = asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        );
        EmittedEvents emittedEvents = new EmittedEvents(events);
        assertEquals(0, emittedEvents.count(EventClass.of(IntProjectStarted.class)));
        assertEquals(1, emittedEvents.count(EventClass.of(IntProjectCreated.class)));
        assertEquals(2, emittedEvents.count(EventClass.of(IntTaskAdded.class)));
    }

    @Test
    @DisplayName("return true if contains the provided class")
    void containMessageClass() {
        List<Event> events = asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        );
        EmittedEvents emittedEvents = new EmittedEvents(events);
        assertFalse(emittedEvents.contain(IntProjectStarted.class));
        assertTrue(emittedEvents.contain(IntProjectCreated.class));
        assertTrue(emittedEvents.contain(IntTaskAdded.class));
    }

    @Test
    @DisplayName("return true if contains the provided event class")
    void containEventClass() {
        List<Event> events = asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        );
        EmittedEvents emittedEvents = new EmittedEvents(events);
        assertFalse(emittedEvents.contain(EventClass.of(IntProjectStarted.class)));
        assertTrue(emittedEvents.contain(EventClass.of(IntProjectCreated.class)));
        assertTrue(emittedEvents.contain(EventClass.of(IntTaskAdded.class)));
    }
}
