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

package io.spine.server.type;

import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventId;
import io.spine.protobuf.AnyPacker;
import io.spine.test.core.ProjectId;
import io.spine.test.core.TaskAssigned;
import io.spine.test.core.TaskId;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.validate.Validate.checkValid;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Event message should")
class EventMessageTest {

    @Test
    @DisplayName("valid")
    void valid() {
        var msg = TaskAssigned.newBuilder()
                .setId(newTaskId())
                .setProjectId(newProjectId())
                .setUserName("John Doe")
                .build();
        var event = event(msg);
        checkValid(event);
    }

    @Test
    @DisplayName("not be invalid")
    void invalid() {
        var msg = TaskAssigned.newBuilder()
                .setId(newTaskId())
                .setProjectId(newProjectId())
                .build();
        var event = event(msg);
        assertThrows(ValidationException.class, () -> checkValid(event));
    }

    private static TaskId newTaskId() {
        return TaskId.newBuilder()
                .setId(Identifier.newUuid())
                .build();
    }

    private static ProjectId newProjectId() {
        return ProjectId.newBuilder()
                .setId(Identifier.newUuid())
                .build();
    }

    private static Event event(Message message) {
        var id = EventId.newBuilder()
                .setValue(Identifier.newUuid())
                .build();
        var wrappedMessage = AnyPacker.pack(message);
        var context = EventContext.newBuilder()
                .setTimestamp(Time.currentTime())
                .build();
        var result = Event.newBuilder()
                .setId(id)
                .setMessage(wrappedMessage)
                .setContext(context)
                .build();
        return result;
    }
}
