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

package io.spine.server.entity;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import io.spine.server.entity.given.FieldMasksTestEnv.Given;
import io.spine.test.aggregate.AggProject;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.testing.Assertions.assertMatchesMask;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("`FieldMasks` utility should")
class FieldMasksTest extends UtilityClassTest<FieldMasks> {

    FieldMasksTest() {
        super(FieldMasks.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(FieldMask.class, FieldMask.getDefaultInstance())
              .setDefault(Descriptors.Descriptor.class, Any.getDescriptor());
    }

    @Nested
    @DisplayName("apply mask")
    class ApplyMask {

        @Test
        @DisplayName("to single message")
        void toSingleMessage() {
            var fieldMask = fromFieldNumbers(AggProject.class,
                                             AggProject.ID_FIELD_NUMBER,
                                             AggProject.NAME_FIELD_NUMBER);
            var original = Given.newProject("some-string-id");

            var masked = FieldMasks.applyMask(fieldMask, original);

            assertEquals(original.getId(), masked.getId());
            assertEquals(original.getName(), masked.getName());
            assertMatchesMask(masked, fieldMask);
        }

        @SuppressWarnings("MethodWithMultipleLoops")
        @Test
        @DisplayName("to message collection")
        void toMessageCollections() {
            var fieldMask = fromFieldNumbers(AggProject.class,
                                             AggProject.STATUS_FIELD_NUMBER,
                                             AggProject.TASK_FIELD_NUMBER);
            var count = 5;

            Collection<AggProject> original = newArrayListWithCapacity(count);

            for (var i = 0; i < count; i++) {
                var project = Given.newProject(format("project-%s", i));
                original.add(project);
            }

            var masked = FieldMasks.applyMask(fieldMask, original);

            assertThat(masked).hasSize(original.size());

            // Collection references are not the same
            assertNotSame(original, masked);

            for (var project : masked) {
                assertMatchesMask(project, fieldMask);

                // Can't check repeated fields with assertMatchesMask
                assertFalse(project.getTaskList()
                                   .isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("not apply empty mask")
    class NotApplyEmptyMask {

        @Test
        @DisplayName("to single `Message`")
        void toSingleMessage() {
            var emptyMask = Given.fieldMask();

            var origin = Given.newProject("read_whole_message");
            var clone = AggProject.newBuilder(origin)
                                         .build();

            var processed = FieldMasks.applyMask(emptyMask, origin);

            // Check object itself was returned
            assertSame(processed, origin);

            // Check object was not changed
            assertEquals(processed, clone);
        }

        @SuppressWarnings("MethodWithMultipleLoops")
        @Test
        @DisplayName("to `Message` collection")
        void toMessageCollection() {
            var emptyMask = Given.fieldMask();

            Collection<AggProject> original = newLinkedList();
            var count = 5;

            for (var i = 0; i < count; i++) {
                var project = Given.newProject(format("test-data--%s", i));
                original.add(project);
            }

            var processed = FieldMasks.applyMask(emptyMask, original);

            assertThat(processed).hasSize(original.size());

            // The argument is not returned
            assertNotSame(original, processed);

            // A copy of the argument is returned (Collection type may differ)
            var processedProjects = processed.iterator();

            for (var anOriginal : original) {
                assertEquals(processedProjects.next(), anOriginal);
            }
        }
    }
}
