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

package io.spine.server.entity;

import com.google.protobuf.FieldMask;
import io.spine.core.Event;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.Task;
import io.spine.test.entity.event.EntProjectCreated;
import io.spine.test.entity.event.EntTaskAdded;
import io.spine.testdata.Sample;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.core.Events.getMessage;
import static io.spine.server.entity.FieldMasks.maskOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("EventFieldFilter should")
class EventFieldFilterTest {

    private static final TestEventFactory eventFactory =
            TestEventFactory.newInstance(EventFieldFilterTest.class);

    @Test
    @DisplayName("pass event if not specified otherwise")
    void allowByDefault() {
        EventFieldFilter filter = EventFieldFilter
                .newBuilder()
                .build();
        Event event = eventFactory.createEvent(EntProjectCreated.getDefaultInstance());
        Optional<Event> filtered = filter.filter(event);
        assertTrue(filtered.isPresent());
        assertEquals(event, filtered.get());
    }

    @Test
    @DisplayName("apply mask and pass")
    void applyFieldMask() {
        FieldMask mask = maskOf(EntTaskAdded.getDescriptor(), EntTaskAdded.PROJECT_ID_FIELD_NUMBER);
        EventFieldFilter filter = EventFieldFilter
                .newBuilder()
                .putMask(EntTaskAdded.class, mask)
                .build();
        EntTaskAdded eventMessage = EntTaskAdded
                .newBuilder()
                .setProjectId(Sample.messageOfType(ProjectId.class))
                .setTask(Sample.messageOfType(Task.class))
                .build();
        Event event = eventFactory.createEvent(eventMessage);

        Optional<Event> filtered = filter.filter(event);
        assertTrue(filtered.isPresent());
        EntTaskAdded maskedEventMessage = (EntTaskAdded) getMessage(filtered.get());
        assertTrue(maskedEventMessage.hasProjectId());
        assertEquals(eventMessage.getProjectId(), maskedEventMessage.getProjectId());
        assertFalse(maskedEventMessage.hasTask());
    }
}
