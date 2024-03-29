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

import com.google.protobuf.Message;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.Task;
import io.spine.test.entity.event.EntProjectCreated;
import io.spine.test.entity.event.EntTaskAdded;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`EventFieldFilter` should")
class EventFieldFilterTest {

    @Test
    @DisplayName("pass event if not specified otherwise")
    void allowByDefault() {
        var filter = EventFieldFilter.newBuilder().build();
        var event = EntProjectCreated.getDefaultInstance();
        Optional<? extends Message> filtered = filter.filter(event);
        assertTrue(filtered.isPresent());
        assertEquals(event, filtered.get());
    }

    @Test
    @DisplayName("apply mask and pass")
    void applyFieldMask() {
        var mask = fromFieldNumbers(EntTaskAdded.class, EntTaskAdded.PROJECT_ID_FIELD_NUMBER);
        var filter = EventFieldFilter.newBuilder()
                .putMask(EntTaskAdded.class, mask)
                .build();
        var event = EntTaskAdded.newBuilder()
                .setProjectId(Sample.messageOfType(ProjectId.class))
                .setTask(Sample.messageOfType(Task.class))
                .build();
        Optional<? extends Message> filtered = filter.filter(event);
        assertTrue(filtered.isPresent());
        var maskedEventMessage = (EntTaskAdded) filtered.get();
        assertTrue(maskedEventMessage.hasProjectId());
        assertEquals(event.getProjectId(), maskedEventMessage.getProjectId());
        assertFalse(maskedEventMessage.hasTask());
    }
}
