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

import io.spine.testing.server.blackbox.event.BbProjectCreated;
import io.spine.testing.server.blackbox.event.BbProjectStarted;
import io.spine.testing.server.blackbox.event.BbTaskAdded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.client.blackbox.Count.count;
import static io.spine.testing.client.blackbox.Count.none;
import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.client.blackbox.Count.thrice;
import static io.spine.testing.client.blackbox.Count.twice;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvent;
import static io.spine.testing.server.blackbox.VerifyEvents.emittedEvents;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.event;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.projectCreated;
import static io.spine.testing.server.blackbox.given.EmittedEventsTestEnv.taskAdded;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Emitted Events Verifier should")
class VerifyEventsTest {

    private EmittedEvents emittedEvents;

    @BeforeEach
    void setUp() {
        emittedEvents = new EmittedEvents(asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        ));
    }

    @Test
    @DisplayName("verify count")
    void countEvents() {
        emittedEvent(thrice()).verify(emittedEvents);

        assertThrows(AssertionError.class, () -> verify(emittedEvent(twice())));
        assertThrows(AssertionError.class, () -> verify(emittedEvent(count(4))));
    }

    @Test
    @DisplayName("verify contains classes")
    void containsClasses() {
        verify(emittedEvents(BbProjectCreated.class, BbTaskAdded.class));

        assertThrows(AssertionError.class, () -> verify(emittedEvents(BbProjectStarted.class)));
        assertThrows(AssertionError.class, () -> verify(emittedEvents(BbTaskAdded.class,
                                                                      BbProjectCreated.class,
                                                                      BbProjectStarted.class)));
    }

    @Test
    @DisplayName("verify contains classes represented by list")
    void verifyNumberOfEvents() {
        verify(emittedEvent(BbProjectStarted.class, none()));
        verify(emittedEvent(BbProjectCreated.class, once()));
        verify(emittedEvent(BbTaskAdded.class, twice()));

        assertThrows(AssertionError.class,
                     () -> verify(emittedEvent(BbProjectStarted.class, once())));
        assertThrows(AssertionError.class,
                     () -> verify(emittedEvent(BbTaskAdded.class, thrice())));
    }

    private void verify(VerifyEvents verifier) {
        verifier.verify(emittedEvents);
    }
}
