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

package io.spine.server.blackbox;

import io.spine.test.server.blackbox.IntProjectCreated;
import io.spine.test.server.blackbox.IntProjectStarted;
import io.spine.test.server.blackbox.IntTaskAdded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.client.blackbox.Count.count;
import static io.spine.client.blackbox.Count.none;
import static io.spine.client.blackbox.Count.once;
import static io.spine.client.blackbox.Count.thrice;
import static io.spine.client.blackbox.Count.twice;
import static io.spine.server.blackbox.EmittedEventsVerifier.emitted;
import static io.spine.server.blackbox.given.EmittedEventsTestEnv.event;
import static io.spine.server.blackbox.given.EmittedEventsTestEnv.projectCreated;
import static io.spine.server.blackbox.given.EmittedEventsTestEnv.taskAdded;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Emitted Events Verifier should")
class EmittedEventsVerifierTest {

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
        emitted(thrice()).verify(emittedEvents);

        assertThrows(AssertionError.class, () -> verify(emitted(twice())));
        assertThrows(AssertionError.class, () -> verify(emitted(count(4))));
    }

    @Test
    @DisplayName("verify contains classes")
    void containsClasses() {
        verify(emitted(IntProjectCreated.class, IntTaskAdded.class));

        assertThrows(AssertionError.class, () -> verify(emitted(IntProjectStarted.class)));
        assertThrows(AssertionError.class, () -> verify(emitted(IntTaskAdded.class,
                                                                IntProjectCreated.class,
                                                                IntProjectStarted.class)));
    }

    @Test
    @DisplayName("verify contains classes represented by list")
    void verifyNumberOfEvents() {
        verify(emitted(IntProjectStarted.class, none()));
        verify(emitted(IntProjectCreated.class, once()));
        verify(emitted(IntTaskAdded.class, twice()));

        assertThrows(AssertionError.class,
                     () -> verify(emitted(IntProjectStarted.class, once())));
        assertThrows(AssertionError.class,
                     () -> verify(emitted(IntTaskAdded.class, thrice())));
    }

    private void verify(EmittedEventsVerifier verifier) {
        verifier.verify(emittedEvents);
    }
}
