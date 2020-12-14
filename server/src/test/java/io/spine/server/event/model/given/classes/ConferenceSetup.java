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

package io.spine.server.event.model.given.classes;

import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.core.EventContext;
import io.spine.core.External;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.event.EventReactor;
import io.spine.server.event.React;
import io.spine.test.event.model.ConferenceAnnounced;
import io.spine.test.event.model.SpeakerJoined;
import io.spine.test.event.model.SpeakersInvited;
import io.spine.test.event.model.TalkSubmissionRequested;
import io.spine.time.LocalDates;

import java.time.LocalDate;

import static io.spine.time.LocalDates.toJavaTime;

/**
 * A test environment {@code EventReactor} class.
 *
 * <p>Normally, what this class does should be done using a Process Manager.
 * We do it this way here to be able to gather classes of events for the tests.
 *
 * @see io.spine.server.event.model.EventReactorClassTest
 */
public class ConferenceSetup implements EventReactor {

    private static final Any id = Identifier.pack(ConferenceSetup.class.getName());

    @React // Just pretend that the event is external.
    SpeakersInvited invitationPolicy(@External ConferenceAnnounced event) {
        LocalDate speakerSubmissionDeadline =
                toJavaTime(event.getDate()).plusWeeks(3);
        return SpeakersInvited
                .newBuilder()
                .setConference(event.getConference())
                .setDeadline(LocalDates.of(speakerSubmissionDeadline))
                .vBuild();
    }

    @React
    TalkSubmissionRequested talkSubmissionPolicy(SpeakerJoined event, EventContext context) {
        LocalDate eventDate = context.localDate();
        LocalDate talkSubmissionDeadline = eventDate.plusWeeks(1);
        return TalkSubmissionRequested
                .newBuilder()
                .setConference(event.getConference())
                .setSpeaker(event.getSpeaker())
                .setDeadline(LocalDates.of(talkSubmissionDeadline))
                .vBuild();
    }

    @Override
    public Any producerId() {
        return id;
    }

    @Override
    public Version version() {
        return Versions.zero();
    }
}
