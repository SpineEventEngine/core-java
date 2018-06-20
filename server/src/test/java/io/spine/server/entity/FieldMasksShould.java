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
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.Task;
import io.spine.test.aggregate.TaskId;
import io.spine.test.commandservice.customer.Customer;
import io.spine.type.TypeUrl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Tests.assertMatchesMask;
import static io.spine.test.Verify.assertSize;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * @author Dmytro Dashenkov
 */
public class FieldMasksShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    @DisplayName("have private constructor")
    void havePrivateConstructor() {
        assertHasPrivateParameterlessCtor(FieldMasks.class);
    }

    @Test
    @DisplayName("create masks for given field tags")
    void createMasksForGivenFieldTags() {
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
    @DisplayName("retrieve default field mask if no field tags requested")
    void retrieveDefaultFieldMaskIfNoFieldTagsRequested() {
        Descriptor descriptor = Project.getDescriptor();
        FieldMask mask = FieldMasks.maskOf(descriptor);
        assertEquals(FieldMask.getDefaultInstance(), mask);
    }

    @Test
    @DisplayName("apply mask to single message")
    void applyMaskToSingleMessage() {
        FieldMask fieldMask = Given.fieldMask(Project.ID_FIELD_NUMBER, Project.NAME_FIELD_NUMBER);
        Project original = Given.newProject("some-string-id");

        Project masked = FieldMasks.applyMask(fieldMask, original, Given.TYPE);

        assertNotEquals(original, masked);
        assertMatchesMask(masked, fieldMask);
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "ObjectEquality"})
    @Test
    @DisplayName("apply mask to message collections")
    void applyMaskToMessageCollections() {
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

    @Test
    @DisplayName("apply only non empty mask to single item")
    void applyOnlyNonEmptyMaskToSingleItem() {
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
    @DisplayName("apply only non empty mask to collection")
    void applyOnlyNonEmptyMaskToCollection() {
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

    @Test
    @DisplayName("fail to mask message if passed type does not match")
    void failToMaskMessageIfPassedTypeDoesNotMatch() {
        FieldMask mask = Given.fieldMask(Project.ID_FIELD_NUMBER);

        Project origin = Given.newProject("some-string");

        thrown.expect(IllegalArgumentException.class);
        FieldMasks.applyMask(mask, origin, Given.OTHER_TYPE);
    }

    private static class Given {

        private static final TypeUrl TYPE = TypeUrl.of(Project.class);
        private static final TypeUrl OTHER_TYPE = TypeUrl.of(Customer.class);

        private static final Descriptor TYPE_DESCRIPTOR = Project.getDescriptor();

        private static Project newProject(String id) {
            ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(id)
                                                 .build();
            Task first = Task.newBuilder()
                                   .setTaskId(TaskId.newBuilder()
                                                    .setId(1)
                                                    .build())
                                   .setTitle("First Task")
                                   .build();

            Task second = Task.newBuilder()
                                    .setTaskId(TaskId.newBuilder()
                                                     .setId(2)
                                                     .build())
                                    .setTitle("Second Task")
                                    .build();

            Project project = Project.newBuilder()
                                           .setId(projectId)
                                           .setName(format("Test project : %s", id))
                                           .addTask(first)
                                           .addTask(second)
                                           .setStatus(Status.CREATED)
                                           .build();
            return project;
        }

        private static FieldMask fieldMask(int... fieldIndices) {
            return FieldMasks.maskOf(TYPE_DESCRIPTOR, fieldIndices);
        }
    }
}
