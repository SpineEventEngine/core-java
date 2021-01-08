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

package io.spine.server.storage.given;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.base.EntityState;
import io.spine.client.TargetFilters;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.HasLifecycleColumns;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.TestTransaction;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.ProjectWithColumns;
import io.spine.testing.core.given.GivenVersion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.TestTransaction.injectState;
import static io.spine.server.entity.storage.TestEntityRecordWithColumnsFactory.createRecord;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecordStorageTestEnv {

    /** Prevents instantiation of this utility class. */
    private RecordStorageTestEnv() {
    }

    public static EntityRecord buildStorageRecord(ProjectId id, EntityState state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    public static EntityRecord buildStorageRecord(ProjectId id, EntityState state,
                                                  LifecycleFlags lifecycleFlags) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .setLifecycleFlags(lifecycleFlags)
                .build();
        return record;
    }

    /**
     * Creates new instance of the test entity.
     */
    public static TestCounterEntity newEntity(ProjectId id) {
        TestCounterEntity entity = new TestCounterEntity(id);
        injectState(entity, Project.newBuilder().setId(id).build(), Versions.zero());
        return entity;
    }

    public static void archive(TransactionalEntity<ProjectId, ?, ?> entity) {
        TestTransaction.archive(entity);
    }

    public static void delete(TransactionalEntity<ProjectId, ?, ?> entity) {
        TestTransaction.delete(entity);
    }

    public static EntityRecordWithColumns withLifecycleColumns(EntityRecord record) {
        LifecycleFlags flags = record.getLifecycleFlags();
        Map<ColumnName, Object> columns = ImmutableMap.of(
                ColumnName.of(archived),
                flags.getArchived(),
                ColumnName.of(deleted),
                flags.getDeleted()
        );
        EntityRecordWithColumns result = createRecord(record, columns);
        return result;
    }

    public static void assertSingleRecord(EntityRecord expected, Iterator<EntityRecord> actual) {
        assertTrue(actual.hasNext());
        EntityRecord singleRecord = actual.next();
        assertFalse(actual.hasNext());
        assertEquals(expected, singleRecord);
    }

    public static TargetFilters emptyFilters() {
        return TargetFilters.getDefaultInstance();
    }

    public static <E> void assertIteratorsEqual(Iterator<? extends E> first,
                                                Iterator<? extends E> second) {
        Collection<? extends E> firstCollection = newArrayList(first);
        Collection<? extends E> secondCollection = newArrayList(second);
        assertEquals(firstCollection.size(), secondCollection.size());
        assertThat(firstCollection).containsExactlyElementsIn(secondCollection);
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestCounterEntity
            extends TransactionalEntity<ProjectId, Project, Project.Builder>
            implements ProjectWithColumns, HasLifecycleColumns<ProjectId, Project> {

        public static final Timestamp PROJECT_VERSION_TIMESTAMP =
                Timestamp.newBuilder()
                         .setSeconds(124565)
                         .setNanos(2434535)
                         .build();

        private int counter = 0;

        public TestCounterEntity(ProjectId id) {
            super(id);
        }

        @Override
        public String getIdString() {
            return idAsString();
        }

        @Override
        public boolean getInternal() {
            return false;
        }

        @Override
        public Any getWrappedState() {
            return Any.getDefaultInstance();
        }

        @Override
        public int getProjectStatusValue() {
            return state().getStatusValue();
        }

        @Override
        public Version getProjectVersion() {
            return Version.newBuilder()
                          .setNumber(counter)
                          .setTimestamp(PROJECT_VERSION_TIMESTAMP)
                          .build();
        }

        @Override
        public Timestamp getDueDate() {
            return Timestamp.newBuilder()
                            .setSeconds(42800)
                            .setNanos(140000)
                            .build();
        }

        public void assignStatus(Project.Status status) {
            Project newState = Project
                    .newBuilder(state())
                    .setId(id())
                    .setStatus(status)
                    .build();
            injectState(this, newState, getProjectVersion());
        }

        public void assignCounter(int counter) {
            this.counter = counter;
            // Manually inject state so the `project_version` storage field is populated.
            Project newState = Project
                    .newBuilder(state())
                    .setId(id())
                    .setProjectVersion(getProjectVersion())
                    .build();
            injectState(this, newState, getProjectVersion());
        }
    }
}
