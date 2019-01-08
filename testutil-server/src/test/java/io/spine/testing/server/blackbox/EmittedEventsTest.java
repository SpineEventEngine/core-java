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

package io.spine.testing.server.blackbox;

import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbProjectStarted;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import io.spine.testing.server.blackbox.given.EmittedEventsTestEnv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.event;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.events;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.projectCreated;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.taskAdded;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Emitted Events should")
class EmittedEventsTest {

    @Test
    @DisplayName("return proper total events count")
    void count() {
        EmittedEvents noEmittedEvents = new EmittedEvents(newArrayList());
        assertEquals(0, noEmittedEvents.count());

        EmittedEvents emittedEvent =
                new EmittedEvents(events(1, EmittedEventsTestEnv::projectCreated));
        assertEquals(1, emittedEvent.count());

        EmittedEvents twoEmittedEvents =
                new EmittedEvents(events(2, EmittedEventsTestEnv::projectCreated));
        assertEquals(2, twoEmittedEvents.count());

        EmittedEvents threeEmittedEvents =
                new EmittedEvents(events(3, EmittedEventsTestEnv::projectCreated));
        assertEquals(3, threeEmittedEvents.count());
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
        assertEquals(0, emittedEvents.count(BbProjectStarted.class));
        assertEquals(1, emittedEvents.count(BbProjectCreated.class));
        assertEquals(2, emittedEvents.count(BbTaskAdded.class));
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
        assertEquals(0, emittedEvents.count(EventClass.from(BbProjectStarted.class)));
        assertEquals(1, emittedEvents.count(EventClass.from(BbProjectCreated.class)));
        assertEquals(2, emittedEvents.count(EventClass.from(BbTaskAdded.class)));
    }

    @Test
    @DisplayName("return true if contains the provided message class")
    void containMessageClass() {
        List<Event> events = asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        );
        EmittedEvents emittedEvents = new EmittedEvents(events);
        assertFalse(emittedEvents.contain(BbProjectStarted.class));
        assertTrue(emittedEvents.contain(BbProjectCreated.class));
        assertTrue(emittedEvents.contain(BbTaskAdded.class));
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
        assertFalse(emittedEvents.contain(EventClass.from(BbProjectStarted.class)));
        assertTrue(emittedEvents.contain(EventClass.from(BbProjectCreated.class)));
        assertTrue(emittedEvents.contain(EventClass.from(BbTaskAdded.class)));
    }
}
