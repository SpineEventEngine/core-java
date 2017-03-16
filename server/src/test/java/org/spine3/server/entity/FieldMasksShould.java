/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import org.junit.Test;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.aggregate.Status;
import org.spine3.test.aggregate.Task;
import org.spine3.test.aggregate.TaskId;
import org.spine3.test.commandservice.customer.Customer;
import org.spine3.type.TypeUrl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Tests.assertMatchesMask;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public class FieldMasksShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(FieldMasks.class);
    }

    @Test
    public void create_masks_for_given_field_tags() {
        final Descriptors.Descriptor descriptor = Project.getDescriptor();
        final int[] fieldNumbers = {1, 2, 3};
        @SuppressWarnings("DuplicateStringLiteralInspection")
        final String[] fieldNames = {"id", "name", "task"};
        final FieldMask mask = FieldMasks.maskOf(descriptor, fieldNumbers);

        final List<String> paths = mask.getPathsList();
        assertSize(fieldNumbers.length, paths);

        for (int i = 0; i < paths.size(); i++) {
            final String expectedPath = descriptor.getFullName() + '.' + fieldNames[i];
            assertEquals(expectedPath, paths.get(i));
        }
    }

    @Test
    public void retrieve_default_field_mask_if_no_field_tags_requested() {
        final Descriptors.Descriptor descriptor = Project.getDescriptor();
        final FieldMask mask = FieldMasks.maskOf(descriptor);
        assertEquals(FieldMask.getDefaultInstance(), mask);
    }

    @Test
    public void apply_mask_to_single_message() {
        final FieldMask fieldMask = Given.fieldMask(Project.ID_FIELD_NUMBER, Project.NAME_FIELD_NUMBER);
        final Project original = Given.newProject("some-string-id");

        final Project masked = FieldMasks.applyMask(fieldMask, original, Given.TYPE);

        assertNotEquals(original, masked);
        assertMatchesMask(masked, fieldMask);
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "ObjectEquality"})
    @Test
    public void apply_mask_to_message_collections() {
        final FieldMask fieldMask = Given.fieldMask(Project.STATUS_FIELD_NUMBER,
                                                    Project.TASK_FIELD_NUMBER);
        final int count = 5;

        final Collection<Project> original = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final Project project = Given.newProject(String.format("project-%s", i));
            original.add(project);
        }

        final Collection<Project> masked = FieldMasks.applyMask(fieldMask, original, Given.TYPE);

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
    public void apply_only_non_empty_mask_to_single_item() {
        final FieldMask emptyMask = Given.fieldMask();

        final Project origin = Given.newProject("read_whole_message");
        final Project clone = Project.newBuilder(origin)
                                     .build();

        final Project processed = FieldMasks.applyMask(emptyMask, origin, Given.TYPE);

        // Check object itself was returned
        assertTrue(processed == origin);

        // Check object was not changed
        assertTrue(processed.equals(clone));
    }

    @SuppressWarnings({"ObjectEquality", "MethodWithMultipleLoops"})
    @Test
    public void apply_only_non_empty_mask_to_collection() {
        final FieldMask emptyMask = Given.fieldMask();

        final Collection<Project> original = new LinkedList<>();
        final int count = 5;

        for (int i = 0; i < count; i++) {
            final Project project = Given.newProject(String.format("test-data--%s", i));
            original.add(project);
        }

        final Collection<Project> processed = FieldMasks.applyMask(emptyMask, original, Given.TYPE);

        assertSize(original.size(), processed);

        // The argument is not returned
        assertFalse(original == processed);

        // A copy of the argument is returned (Collection type may differ)
        final Iterator<Project> processedProjects = processed.iterator();

        for (Project anOriginal : original) {
            assertTrue(processedProjects.next()
                                        .equals(anOriginal));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_mask_message_if_passed_type_does_not_match() {
        final FieldMask mask = Given.fieldMask(Project.ID_FIELD_NUMBER);

        final Project origin = Given.newProject("some-string");

        FieldMasks.applyMask(mask, origin, Given.OTHER_TYPE);
    }

    private static class Given {

        private static final TypeUrl TYPE = TypeUrl.of(Project.class);
        private static final TypeUrl OTHER_TYPE = TypeUrl.of(Customer.class);

        private static final Descriptors.Descriptor TYPE_DESCRIPTOR = Project.getDescriptor();

        private static Project newProject(String id) {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(id)
                                                 .build();
            final Task first = Task.newBuilder()
                                   .setTaskId(TaskId.newBuilder()
                                                    .setId(1)
                                                    .build())
                                   .setTitle("First Task")
                                   .build();

            final Task second = Task.newBuilder()
                                    .setTaskId(TaskId.newBuilder()
                                                     .setId(2)
                                                     .build())
                                    .setTitle("Second Task")
                                    .build();

            final Project project = Project.newBuilder()
                                           .setId(projectId)
                                           .setName(String.format("Test project : %s", id))
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
