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

package io.spine.testing.server;

import com.google.common.truth.Subject;
import io.spine.core.Event;
import io.spine.testing.server.given.entity.TuTaskId;
import io.spine.testing.server.given.entity.event.TuCommentAdded;

import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.server.EventSubject.events;

class EventSubjectTest extends EmittedMessageSubjectTest<EventSubject, Event> {

    private static final TestEventFactory events =
            TestEventFactory.newInstance(EventSubjectTest.class);

    @Override
    Subject.Factory<EventSubject, Iterable<Event>> subjectFactory() {
        return events();
    }

    @Override
    EventSubject assertWithSubjectThat(Iterable<Event> messages) {
        return EventSubject.assertThat(messages);
    }

    @Override
    Event createMessage() {
        TuTaskId taskId = TuTaskId
                .vBuilder()
                .setValue(newUuid())
                .build();
        TuCommentAdded event = TuCommentAdded
                .vBuilder()
                .setId(taskId)
                .build();
        return events.createEvent(event);
    }
}
