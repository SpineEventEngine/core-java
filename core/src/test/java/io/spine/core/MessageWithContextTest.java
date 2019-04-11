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

package io.spine.core;

import io.spine.base.EventMessage;
import io.spine.protobuf.AnyPacker;
import io.spine.test.core.ProjectCreated;
import io.spine.test.core.ProjectId;
import io.spine.test.core.TaskAssigned;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("MessageWithContext should")
class MessageWithContextTest {

    @Test
    @DisplayName("verify type of the enclosed message")
    void checkType() {
        Event event = stubEvent();

        assertThat(event.hasType(ProjectCreated.class))
                .isTrue();
        assertThat(event.hasType(EventMessage.class))
                .isTrue();
        assertThat(event.hasType(TaskAssigned.class))
                .isFalse();
    }

    /**
     * Creates a stub instance of {@code Event} with the type {@link ProjectCreated}.
     */
    private Event stubEvent() {
        Event event = Event
                .newBuilder()
                .setMessage(AnyPacker.pack(
                        ProjectCreated
                                .newBuilder()
                                .setProjectId(ProjectId.newBuilder()
                                                       .setId(getClass().getName()))
                                .build()))
                .build();
        return event;
    }
}
