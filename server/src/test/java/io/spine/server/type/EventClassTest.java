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

package io.spine.server.type;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.type.given.rejection.PhoneNotFound;
import io.spine.server.type.given.rejection.TestRejections;
import io.spine.test.core.ProjectCreated;
import io.spine.test.core.ProjectId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`EventClass` should")
class EventClassTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EventClass.class);
    }

    @Nested
    @DisplayName("be constructed")
    class BeConstructed {

        @Test
        @DisplayName("from `TypeUrl` instance")
        void fromTypeUrl() {
            TypeUrl typeUrl = TypeUrl.from(ProjectCreated.getDescriptor());
            EventClass eventClass = EventClass.from(typeUrl);
            assertThat(eventClass.value()).isEqualTo(ProjectCreated.class);
        }

        @Test
        @DisplayName("from `Event` instance")
        void fromEvent() {
            ProjectId id = ProjectId
                    .newBuilder()
                    .setId(newUuid())
                    .build();
            ProjectCreated projectCreated = ProjectCreated
                    .newBuilder()
                    .setProjectId(id)
                    .build();
            Any any = AnyPacker.pack(projectCreated);
            Event event = Event
                    .newBuilder()
                    .setMessage(any)
                    .build();
            EventClass eventClass = EventClass.from(event);
            assertThat(eventClass.value()).isEqualTo(ProjectCreated.class);
        }
    }

    @SuppressWarnings({"CheckReturnValue", "ResultOfMethodCallIgnored"})
    // Method called to throw exception.
    @Test
    @DisplayName("throw IAE when constructing from non-event type URL")
    void throwOnNonEventType() {
        TypeUrl typeUrl = TypeUrl.from(ProjectId.getDescriptor());
        assertThrows(IllegalArgumentException.class, () -> EventClass.from(typeUrl));
    }

    @Test
    @DisplayName("obtain the value by `ThrowableMessage` class")
    void byThrowableMessage() {
        assertThat(EventClass.fromThrowable(PhoneNotFound.class).value())
                .isEqualTo(TestRejections.PhoneNotFound.class);
    }
}
