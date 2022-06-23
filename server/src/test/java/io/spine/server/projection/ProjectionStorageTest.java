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

package io.spine.server.projection;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import io.spine.base.EntityState;
import io.spine.client.ResponseFormat;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordStorageTest;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.testing.core.given.GivenVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newArrayListWithCapacity;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.FieldMaskUtil.fromStringList;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.projection.given.ProjectionStorageTestEnv.givenProject;
import static io.spine.server.storage.given.RecordStorageTestEnv.withLifecycleColumns;
import static io.spine.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;
import static io.spine.testing.Tests.assertMatchesMask;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Projection storage tests.
 */
public abstract class ProjectionStorageTest
        extends RecordStorageTest<ProjectionStorage<ProjectId>> {

    private ProjectionStorage<ProjectId> storage;

    @SuppressWarnings("BreakStatement")
    @CanIgnoreReturnValue
    private static Project checkProjectIdIsInList(EntityRecord project, List<ProjectId> ids) {
        Any packedState = project.getState();
        Project state = unpack(packedState, Project.class);
        ProjectId id = state.getId();

        boolean isIdPresent = false;
        for (ProjectId genericId : ids) {
            isIdPresent = genericId.equals(id);
            if (isIdPresent) {
                break;
            }
        }
        assertTrue(isIdPresent);

        return state;
    }

    @Override
    protected EntityState newState(ProjectId id) {
        String uniqueName = format("Projection_name-%s-%s", id.getId(), System.nanoTime());
        Project state = givenProject(id, uniqueName);
        return state;
    }

    @Override
    protected EntityRecord newStorageRecord() {
        return newEntityStorageRecord();
    }

    @BeforeEach
    void setUpProjectionStorageTest() {
        storage = storage();
    }

    @AfterEach
    void tearDownProjectionStorageTest() {
        close(storage);
    }

    @Nested
    @DisplayName("read")
    class ReadMessages {

        @Test
        @DisplayName("all messages")
        void all() {
            List<ProjectId> ids = fillStorage(5);

            Iterator<EntityRecord> read = storage.readAll(ResponseFormat.getDefaultInstance());
            Collection<EntityRecord> readRecords = newArrayList(read);
            assertThat(readRecords).hasSize(ids.size());
            for (EntityRecord record : readRecords) {
                Project state = unpack(record.getState(), Project.class);
                ProjectId id = state.getId();
                assertThat(ids).contains(id);
            }
        }

        @Test
        @DisplayName("all messages with field mask")
        void allWithFieldMask() {
            List<ProjectId> ids = fillStorage(5);

            FieldMask fieldMask = fromStringList(Project.class, ImmutableList.of("id", "name"));

            ResponseFormat format = ResponseFormat
                    .newBuilder()
                    .setFieldMask(fieldMask)
                    .vBuild();
            Iterator<EntityRecord> read = storage.readAll(format);
            Collection<EntityRecord> readRecords = newArrayList(read);
            assertThat(readRecords).hasSize(ids.size());
            for (EntityRecord record : readRecords) {
                Any packedState = record.getState();
                Project state = unpack(packedState, Project.class);
                assertMatchesMask(state, fieldMask);
            }
        }

        @Test
        @DisplayName("bulk of messages")
        void bulk() {
            // Get a subset of IDs
            List<ProjectId> ids = fillStorage(10).subList(0, 5);

            Iterator<EntityRecord> read = storage.readMultiple(ids, FieldMask.getDefaultInstance());
            Collection<EntityRecord> readRecords = newArrayList(read);
            assertThat(readRecords).hasSize(ids.size());

            // Check data consistency
            for (EntityRecord record : readRecords) {
                checkProjectIdIsInList(record, ids);
            }
        }

        @Test
        @DisplayName("bulk of messages with field mask")
        void bulkWithFieldMask() {
            // Get a subset of IDs
            List<ProjectId> ids = fillStorage(10).subList(0, 5);

            FieldMask fieldMask = fromStringList(Project.class, ImmutableList.of("id", "status"));

            Iterator<EntityRecord> read = storage.readMultiple(ids, fieldMask);
            Collection<EntityRecord> readRecords = newArrayList(read);
            assertThat(readRecords).hasSize(ids.size());

            // Check data consistency
            for (EntityRecord record : readRecords) {
                Project state = checkProjectIdIsInList(record, ids);
                assertMatchesMask(state, fieldMask);
            }
        }

        private List<ProjectId> fillStorage(int count) {
            List<ProjectId> ids = newArrayListWithCapacity(count);

            for (int i = 0; i < count; i++) {
                ProjectId id = newId();
                Project state = givenProject(id, format("project-%d", i));
                Any packedState = AnyPacker.pack(state);

                EntityRecord rawRecord = EntityRecord
                        .newBuilder()
                        .setState(packedState)
                        .setVersion(GivenVersion.withNumber(1))
                        .build();
                EntityRecordWithColumns record = withLifecycleColumns(rawRecord);
                storage.write(id, record);
                ids.add(id);
            }

            return ids;
        }
    }
}
