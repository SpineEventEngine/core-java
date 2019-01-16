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

package io.spine.server.storage.given;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Filters;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.core.Version;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.TestTransaction;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.Enumerated;
import io.spine.server.storage.RecordStorage;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.ProjectVBuilder;
import io.spine.testing.core.given.GivenVersion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.entity.TestTransaction.injectState;
import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.server.entity.storage.TestEntityRecordWithColumnsFactory.createRecord;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecordStorageTestEnv {

    /** Prevents instantiation of this utility class. */
    private RecordStorageTestEnv() {
    }

    public static EntityRecord buildStorageRecord(ProjectId id, Message state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    public static EntityRecord buildStorageRecord(ProjectId id, Message state,
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
        return new TestCounterEntity(id);
    }

    public static void archive(TransactionalEntity<ProjectId, ?, ?> entity) {
        TestTransaction.archive(entity);
    }

    public static void delete(TransactionalEntity<ProjectId, ?, ?> entity) {
        TestTransaction.delete(entity);
    }

    public static EntityRecordWithColumns withLifecycleColumns(EntityRecord record) {
        LifecycleFlags flags = record.getLifecycleFlags();
        Map<String, EntityColumn.MemoizedValue> columns = ImmutableMap.of(
                LifecycleColumns.ARCHIVED.columnName(),
                booleanColumn(LifecycleColumns.ARCHIVED.column(), flags.getArchived()),
                LifecycleColumns.DELETED.columnName(),
                booleanColumn(LifecycleColumns.DELETED.column(), flags.getDeleted())
        );
        EntityRecordWithColumns result = createRecord(record, columns);
        return result;
    }

    private static EntityColumn.MemoizedValue booleanColumn(EntityColumn column, boolean value) {
        EntityColumn.MemoizedValue memoizedValue = mock(EntityColumn.MemoizedValue.class);
        when(memoizedValue.getSourceColumn()).thenReturn(column);
        when(memoizedValue.getValue()).thenReturn(value);
        return memoizedValue;
    }

    public static void assertSingleRecord(EntityRecord expected, Iterator<EntityRecord> actual) {
        assertTrue(actual.hasNext());
        EntityRecord singleRecord = actual.next();
        assertFalse(actual.hasNext());
        assertEquals(expected, singleRecord);
    }

    public static <T> EntityQuery<T> newEntityQuery(Filters filters, RecordStorage<T> storage) {
        return EntityQueries.from(filters, emptyOrderBy(), emptyPagination(), storage);
    }

    public static OrderBy emptyOrderBy() {
        return OrderBy.getDefaultInstance();
    }

    public static Pagination emptyPagination() {
        return Pagination.getDefaultInstance();
    }

    public static Filters emptyFilters() {
        return Filters.getDefaultInstance();
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
            extends TransactionalEntity<ProjectId, Project, ProjectVBuilder> {

        private int counter = 0;

        public TestCounterEntity(ProjectId id) {
            super(id);
        }

        @CanIgnoreReturnValue
        @Column
        public int getCounter() {
            return counter;
        }

        @Column
        public long getBigCounter() {
            return getCounter();
        }

        @Column
        public boolean isCounterEven() {
            return counter % 2 == 0;
        }

        @Column
        public String getCounterName() {
            return getId().toString();
        }

        @Column(name = "COUNTER_VERSION" /* Custom name for storing
                                            to check that querying is correct. */)
        public Version getCounterVersion() {
            return Version.newBuilder()
                          .setNumber(counter)
                          .build();
        }

        @Column
        public Timestamp getNow() {
            return Time.getCurrentTime();
        }

        @Column
        public Project getCounterState() {
            return getState();
        }

        @Column
        public int getProjectStatusValue() {
            return getState().getStatusValue();
        }

        @Column
        public Project.Status getProjectStatusOrdinal() {
            return Enum.valueOf(Project.Status.class, getState().getStatus()
                                                                .name());
        }

        @Column
        @Enumerated(STRING)
        public Project.Status getProjectStatusString() {
            return Enum.valueOf(Project.Status.class, getState().getStatus()
                                                                .name());
        }

        public void assignStatus(Project.Status status) {
            Project newState = Project
                    .newBuilder(getState())
                    .setStatus(status)
                    .build();
            injectState(this, newState, getCounterVersion());
        }

        public void assignCounter(int counter) {
            this.counter = counter;
        }
    }

    /**
     * Entity columns representing lifecycle flags, {@code archived} and {@code deleted}.
     *
     * <p>These columns are present in each {@linkplain Entity entity}. For the purpose of
     * tests being as close to the real production environment as possible, these columns are stored
     * with the entity records, even if an actual entity is missing.
     *
     * <p>Note that there are cases, when a {@code RecordStorage} stores entity records with no such
     * columns, e.g. the {@linkplain io.spine.server.event.EEntity event entity}. Thus, do not rely
     * on these columns being present in all the entities by default when implementing
     * a {@code RecordStorage}.
     */
    public enum LifecycleColumns {

        ARCHIVED("isArchived"),
        DELETED("isDeleted");

        private final EntityColumn column;

        LifecycleColumns(String getterName) {
            try {
                this.column = EntityColumn.from(Entity.class.getDeclaredMethod(getterName));
            } catch (NoSuchMethodException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        public EntityColumn column() {
            return column;
        }

        public String columnName() {
            return column.getStoredName();
        }
    }
}
