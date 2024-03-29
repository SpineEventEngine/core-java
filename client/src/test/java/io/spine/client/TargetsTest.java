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
package io.spine.client;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.spine.test.client.TestEntity;
import io.spine.test.queries.TaskId;
import io.spine.testing.UtilityClassTest;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.client.Targets.acceptingOnly;
import static io.spine.client.Targets.allOf;
import static io.spine.client.Targets.someOf;
import static io.spine.client.given.TargetsTestEnv.newTaskId;
import static io.spine.type.TypeUrl.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Targets` utility should")
class TargetsTest extends UtilityClassTest<Targets> {

    TargetsTest() {
        super(Targets.class);
    }

    @Nested
    @DisplayName("compose `Target`")
    class Compose {

        @Test
        @DisplayName("for all of type")
        void allOfType() {
            var target = allOf(TestEntity.class);

            assertUrl(target);
        }

        private void assertUrl(Target target) {
            assertEquals(TypeUrl.of(TestEntity.class), parse(target.getType()));
        }

        @Test
        @DisplayName("with `Message` IDs")
        void withMessageIds() {
            var taskId = newTaskId();
            var target = someOf(TestEntity.class, ImmutableSet.of(taskId));

            assertUrl(target);

            var expected = acceptingOnly(taskId);
            assertEquals(expected, target.getFilters());
        }

        @Test
        @DisplayName("with `String` IDs")
        void withStringIds() {
            var firstId = "a";
            var secondId = "b";
            var thirdId = "c";

            var target = someOf(TestEntity.class, ImmutableSet.of(firstId, secondId, thirdId));

            assertUrl(target);

            var expected = acceptingOnly(StringValue.of(firstId),
                                         StringValue.of(secondId),
                                         StringValue.of(thirdId));
            assertEquals(expected, target.getFilters());
        }

        @Test
        @DisplayName("with `Integer` IDs")
        void withIntIds() {
            var firstId = 1;
            var secondId = 2;
            var thirdId = 3;

            var target = someOf(TestEntity.class, ImmutableSet.of(firstId, secondId, thirdId));

            assertUrl(target);

            var expected = acceptingOnly(Int32Value.of(firstId),
                                         Int32Value.of(secondId),
                                         Int32Value.of(thirdId));
            assertEquals(expected, target.getFilters());
        }

        @Test
        @DisplayName("with `Long` IDs")
        void withLongIds() {
            var firstId = 1L;
            var secondId = 2L;
            var thirdId = 3L;

            var target = someOf(TestEntity.class, ImmutableSet.of(firstId, secondId, thirdId));

            assertUrl(target);

            var expected = acceptingOnly(Int64Value.of(firstId),
                                         Int64Value.of(secondId),
                                         Int64Value.of(thirdId));
            assertEquals(expected, target.getFilters());
        }
    }

    @Test
    @DisplayName("throw `IAE` for unsupported IDs")
    void throwIaeForUnsupportedIds() {
        assertThrows(IllegalArgumentException.class,
                     () -> someOf(TaskId.class, ImmutableSet.of(new Object())));
    }
}
