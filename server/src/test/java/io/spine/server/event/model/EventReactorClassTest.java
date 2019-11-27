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

package io.spine.server.event.model;

import io.spine.server.event.model.given.classes.ConferenceSetup;
import io.spine.server.type.EventClass;
import io.spine.test.event.model.ConferenceAnnounced;
import io.spine.test.event.model.SpeakerJoined;
import io.spine.test.event.model.SpeakersInvited;
import io.spine.test.event.model.TalkSubmissionRequested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.event.model.EventReactorClass.asReactorClass;

@DisplayName("`EventReactorClass` should")
class EventReactorClassTest {

    private final EventReactorClass<?> reactorClass = asReactorClass(ConferenceSetup.class);

    @Nested
    @DisplayName("provide classes of")
    class MessageClasses {

        @Test
        @DisplayName("events (including external) to which instances of this class react")
        void events() {
            assertThat(reactorClass.events())
                    .containsExactlyElementsIn(EventClass.setOf(
                            ConferenceAnnounced.class,
                            SpeakerJoined.class
                    ));
        }

        @Test
        @DisplayName("domestic events to which instances of this class react")
        void domesticEvents() {
            assertThat(reactorClass.domesticEvents())
                    .containsExactlyElementsIn(EventClass.setOf(
                            SpeakerJoined.class
                    ));
        }

        @Test
        @DisplayName("external events to which instances of this class react")
        void externalEvents() {
            assertThat(reactorClass.externalEvents())
                    .containsExactlyElementsIn(EventClass.setOf(
                            ConferenceAnnounced.class
                    ));
        }

        @Test
        @DisplayName("events which instances of this class produce")
        void reactionOutput() {
            assertThat(reactorClass.reactionOutput())
                    .containsExactlyElementsIn(EventClass.setOf(
                            SpeakersInvited.class,
                            TalkSubmissionRequested.class
                    ));
        }
    }

    @Test
    @DisplayName("obtain the method by the class of the event and the class of the origin")
    void reactorOf() {
        //TODO:2019-11-26:alexander.yevsyukov: Implement
    }
}
