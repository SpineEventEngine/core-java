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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import io.spine.server.entity.given.FieldMasksTestEnv.Given;
import io.spine.test.aggregate.Project;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.Verify.assertSize;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("FieldMasks utility should")
class FieldMasksTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(FieldMasks.class);
    }

    @Nested
    @DisplayName("create masks")
    class CreateMasks {

        @Test
        @DisplayName("for given field tags")
        void forGivenFields() {
            Descriptor descriptor = Project.getDescriptor();
            int[] fieldNumbers = {1, 2, 3};
            @SuppressWarnings("DuplicateStringLiteralInspection")
            String[] fieldNames = {"id", "name", "task"};
            FieldMask mask = FieldMasks.maskOf(descriptor, fieldNumbers);

            List<String> paths = mask.getPathsList();
            assertSize(fieldNumbers.length, paths);

            for (int i = 0; i < paths.size(); i++) {
                String expectedPath = descriptor.getFullName() + '.' + fieldNames[i];
                assertEquals(expectedPath, paths.get(i));
            }
        }

        @Test
        @DisplayName("when no field tags are given")
        void whenNoFieldsAreGiven() {
            Descriptor descriptor = Project.getDescriptor();
            FieldMask mask = FieldMasks.maskOf(descriptor);
            assertEquals(FieldMask.getDefaultInstance(), mask);
        }
    }

    @Nested
    @DisplayName("apply mask")
    class ApplyMask {

        @Test
        @DisplayName("to single message")
        void toSingleMessage() {
            FieldMask fieldMask =
                    Given.fieldMask(Project.ID_FIELD_NUMBER, Project.NAME_FIELD_NUMBER);
            Project original = Given.newProject("some-string-id");

            Project masked = FieldMasks.applyMask(fieldMask, original, Given.TYPE);

            assertNotEquals(original, masked);
            assertMatchesMask(masked, fieldMask);
        }

        @SuppressWarnings({"MethodWithMultipleLoops", "ObjectEquality"})
        @Test
        @DisplayName("to message collection")
        void toMessageCollections() {
            FieldMask fieldMask = Given.fieldMask(Project.STATUS_FIELD_NUMBER,
                                                  Project.TASK_FIELD_NUMBER);
            int count = 5;

            Collection<Project> original = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                Project project = Given.newProject(format("project-%s", i));
                original.add(project);
            }

            Collection<Project> masked = FieldMasks.applyMask(fieldMask, original, Given.TYPE);

            assertSize(original.size(), masked);

            // Collection references are not the same
            assertFalse(original == masked);

            for (Project project : masked) {
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
        @DisplayName("to single message")
        void toSingleMessage() {
            FieldMask emptyMask = Given.fieldMask();

            Project origin = Given.newProject("read_whole_message");
            Project clone = Project.newBuilder(origin)
                                   .build();

            Project processed = FieldMasks.applyMask(emptyMask, origin, Given.TYPE);

            // Check object itself was returned
            assertSame(processed, origin);

            // Check object was not changed
            assertEquals(processed, clone);
        }

        @SuppressWarnings({"ObjectEquality", "MethodWithMultipleLoops"})
        @Test
        @DisplayName("to message collection")
        void toMessageCollection() {
            FieldMask emptyMask = Given.fieldMask();

            Collection<Project> original = new LinkedList<>();
            int count = 5;

            for (int i = 0; i < count; i++) {
                Project project = Given.newProject(format("test-data--%s", i));
                original.add(project);
            }

            Collection<Project> processed = FieldMasks.applyMask(emptyMask, original, Given.TYPE);

            assertSize(original.size(), processed);

            // The argument is not returned
            assertNotSame(original, processed);

            // A copy of the argument is returned (Collection type may differ)
            Iterator<Project> processedProjects = processed.iterator();

            for (Project anOriginal : original) {
                assertEquals(processedProjects.next(), anOriginal);
            }
        }
    }

    @Test
    @DisplayName("fail to mask message if passed type does not match")
    void notMaskWrongType() {
        FieldMask mask = Given.fieldMask(Project.ID_FIELD_NUMBER);

        Project origin = Given.newProject("some-string");

        assertThrows(IllegalArgumentException.class,
                     () -> FieldMasks.applyMask(mask, origin, Given.OTHER_TYPE));
    }
}
