/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.server.ServerEnvironment;
import io.spine.server.storage.given.RecordStorageDelegateTestEnv;
import io.spine.server.storage.given.StgProjectStorage;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.protobuf.util.Durations.fromDays;
import static com.google.protobuf.util.Durations.fromMinutes;
import static com.google.protobuf.util.Timestamps.add;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.currentTime;
import static io.spine.query.Direction.ASC;
import static io.spine.query.Direction.DESC;
import static io.spine.server.storage.given.GivenStorageProject.newState;
import static io.spine.server.storage.given.RecordStorageDelegateTestEnv.assertOnlyIdAndDueDate;
import static io.spine.server.storage.given.RecordStorageDelegateTestEnv.coupleOfDone;
import static io.spine.server.storage.given.RecordStorageDelegateTestEnv.dozenOfRecords;
import static io.spine.server.storage.given.RecordStorageDelegateTestEnv.idAndDueDate;
import static io.spine.server.storage.given.RecordStorageDelegateTestEnv.toIds;
import static io.spine.server.storage.given.StgColumn.due_date;
import static io.spine.server.storage.given.StgColumn.status;
import static io.spine.test.storage.StgProject.Status.CREATED;
import static io.spine.test.storage.StgProject.Status.DONE;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of the API provided by {@link RecordStorageDelegate} to the descendant classes.
 *
 * <p>Sample storage implementation used in the test is {@link StgProjectStorage}.
 *
 * <p>The aim of this test is to ensure that any storage implementations built on top of
 * the {@code RecordStorageDelegate} is able to utilize the API with the expected results.
 */
@DisplayName("A `RecordStorageDelegate` descendant should")
public class RecordStorageDelegateTest
        extends AbstractStorageTest<StgProjectId, StgProject, StgProjectStorage> {

    @Override
    protected StgProjectStorage newStorage() {
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new StgProjectStorage(factory, false);
    }

    @Override
    protected StgProject newStorageRecord(StgProjectId id) {
        return newState(id);
    }

    @Override
    protected StgProjectId newId() {
        return RecordStorageDelegateTestEnv.generateId();
    }

    @Nested
    @DisplayName("write and read")
    class WriteAndRead {

        @Test
        @DisplayName("batch of records")
        void manyRecords() {
            Iterable<StgProject> records = dozenOfRecords()
                    .values();
            storage().writeBatch(records);

            Iterator<StgProject> actualIterator = storage().readAll();
            ImmutableList<StgProject> actualRecords = ImmutableList.copyOf(actualIterator);
            assertThat(actualRecords).containsExactlyElementsIn(records);
        }

        @Test
        @DisplayName("batch of records, recalling them by their IDs")
        void allByIds() {
            ImmutableMap<StgProjectId, StgProject> recordMap = dozenOfRecords();
            storage().writeBatch(recordMap.values());

            ImmutableSet<StgProjectId> ids = recordMap.keySet();
            ImmutableList<StgProjectId> partOfIds = RecordStorageDelegateTestEnv.halfDozenOf(ids);
            Iterator<StgProject> actualIterator = storage().readAll(partOfIds);
            ImmutableList<StgProject> actualRecords = ImmutableList.copyOf(actualIterator);
            RecordStorageDelegateTestEnv.assertHaveIds(actualRecords, partOfIds);
        }
    }

    @Nested
    @DisplayName("read")
    class Query {

        @Test
        @DisplayName("a single record with the particular `FieldMask`")
        void singleRecordWithMask() {
            StgProject record = newState(newId());
            storage().write(record);

            Optional<StgProject> result = storage().read(record.getId(),
                                                         idAndDueDate());
            assertThat(result).isPresent();
            StgProject actual = result.get();
            assertOnlyIdAndDueDate(actual);
        }

        @Test
        @DisplayName("several records according to the query with the `FieldMask` set")
        void allByMask() {
            ImmutableCollection<StgProject> records = dozenOfRecords().values();
            storage().writeBatch(records);

            RecordQuery<StgProjectId, StgProject> query = queryBuilder().withMask(idAndDueDate())
                                                                        .build();
            Iterator<StgProject> iterator = storage().readAll(query);
            ImmutableList<StgProject> actualResults = ImmutableList.copyOf(iterator);
            for (StgProject result : actualResults) {
                assertOnlyIdAndDueDate(result);
            }
        }

        @Test
        @DisplayName("several records according to the given limit and ordering")
        void allRecordsWithLimitAndOrdering() {
            StgProject oldest = newState(newId(), DONE, add(currentTime(), fromMinutes(0)));
            StgProject older = newState(newId(), DONE, add(currentTime(), fromMinutes(1)));
            StgProject almostNew = newState(newId(), DONE, add(currentTime(), fromMinutes(2)));
            StgProject newest = newState(newId(), DONE, add(currentTime(), fromMinutes(3)));

            storage().writeBatch(ImmutableList.of(newest, older, oldest, almostNew));

            int limit = 2;
            RecordQuery<StgProjectId, StgProject> query = queryBuilder().orderBy(due_date, ASC)
                              .limit(limit)
                              .build();
            Iterator<StgProject> iterator = storage().readAll(query);
            ImmutableList<StgProject> actualRecords = ImmutableList.copyOf(iterator);
            assertThat(actualRecords).hasSize(limit);
            assertThat(actualRecords.get(0)).isEqualTo(oldest);
            assertThat(actualRecords.get(1)).isEqualTo(older);
        }

        @Test
        @DisplayName("several records by their IDs and the `FieldMask`")
        void allByIdsAndMask() {
            ImmutableMap<StgProjectId, StgProject> recordMap = dozenOfRecords();
            storage().writeBatch(recordMap.values());

            Iterator<StgProject> iterator = storage().readAll(recordMap.keySet(), idAndDueDate());
            ImmutableList<StgProject> actualResults = ImmutableList.copyOf(iterator);
            for (StgProject result : actualResults) {
                assertOnlyIdAndDueDate(result);
            }
        }

        @Test
        @DisplayName("many records by a single column value only")
        void manyRecordsBySingleColumnWithDefaultResponseFormat() {
            ImmutableCollection<StgProject> createdProjects = dozenOfRecords().values();
            storage().writeBatch(createdProjects);

            ImmutableList<StgProject> doneProjects = coupleOfDone(currentTime());
            storage().writeBatch(doneProjects);

            RecordQuery<StgProjectId, StgProject> query = queryDoneProjects().build();
            Iterator<StgProject> iterator = storage().readAll(query);
            ImmutableList<StgProject> actualProjects = ImmutableList.copyOf(iterator);

            assertThat(actualProjects).containsExactlyElementsIn(doneProjects);
        }

        @Test
        @DisplayName("many records by several columns only")
        void manyRecordsBySeveralColumnsWithDefaultResponseFormat() {
            ImmutableCollection<StgProject> createdProjects = dozenOfRecords().values();
            storage().writeBatch(createdProjects);

            Timestamp now = currentTime();
            ImmutableList<StgProject> doneDueToday = coupleOfDone(now);
            storage().writeBatch(doneDueToday);

            ImmutableList<StgProject> doneDueYesterday = coupleOfDone(subtract(now, fromDays(1)));
            storage().writeBatch(doneDueYesterday);

            Timestamp aMinuteAgo = subtract(now, fromMinutes(1));

            RecordQuery<StgProjectId, StgProject> query =
                    queryDoneProjects()
                            .where(due_date)
                            .isLessThan(aMinuteAgo)
                            .build();

            Iterator<StgProject> iterator = storage().readAll(query);
            ImmutableList<StgProject> actualProjects = ImmutableList.copyOf(iterator);

            assertThat(actualProjects).containsExactlyElementsIn(doneDueYesterday);
        }

        private RecordQueryBuilder<StgProjectId, StgProject> queryDoneProjects() {
            return queryBuilder().where(status).is(DONE.name());
        }

        @Test
        @DisplayName("many records by a single column with the limit and ordering")
        void manyRecordsBySingleColumnAndLimit() {
            ImmutableCollection<StgProject> createdProjects = dozenOfRecords().values();
            storage().writeBatch(createdProjects);

            RecordQuery<StgProjectId, StgProject> queryDone =
                    queryDoneProjects().orderBy(due_date, ASC)
                                       .limit(10)
                                       .build();

            Iterator<StgProject> iterator = storage().readAll(queryDone);
            assertThat(iterator.hasNext()).isFalse();

            List<StgProject> sortedByDueDate = new ArrayList<>(createdProjects);
            sortedByDueDate.sort((r1, r2) -> Timestamps.compare(r1.getDueDate(), r2.getDueDate()));
            int limit = 2;
            List<StgProject> expected = sortedByDueDate.subList(0, limit);

            RecordQuery<StgProjectId, StgProject> queryCreated =
                    queryBuilder()
                              .where(status)
                              .is(CREATED.name())
                              .orderBy(due_date, ASC)
                              .limit(limit)
                              .build();
            Iterator<StgProject> limitedIterator = storage().readAll(queryCreated);
            ImmutableList<StgProject> actual = ImmutableList.copyOf(limitedIterator);
            assertThat(actual).containsExactlyElementsIn(expected);
        }

        @Test
        @DisplayName("many records by several columns with the limit and ordering")
        void manyRecordsBySeveralColumnsAndLimit() {
            Timestamp now = currentTime();

            ImmutableList<StgProject> doneLongAgo = coupleOfDone(subtract(now, fromDays(10)));
            ImmutableList<StgProject> records =
                    ImmutableList.<StgProject>builder()
                            .addAll(doneLongAgo)
                            .addAll(coupleOfDone(subtract(now, fromDays(1))))
                            .addAll(coupleOfDone(now))
                            .addAll(dozenOfRecords().values())  // in `CREATED` status.
                            .build();
            storage().writeBatch(records);

            RecordQuery<StgProjectId, StgProject> query =
                    queryDoneProjects().where(due_date)
                                       .isLessThan(now)
                                       .orderBy(due_date, ASC)
                                       .limit(2)
                                       .build();

            Iterator<StgProject> iterator = storage().readAll(query);
            ImmutableList<StgProject> actual = ImmutableList.copyOf(iterator);
            assertThat(actual).containsExactlyElementsIn(doneLongAgo);
        }

        @Test
        @DisplayName("many records by several columns with the limit and the field mask")
        void manyRecordsBySeveralColumnsWithLimitAndMask() {
            Timestamp now = currentTime();

            ImmutableList<StgProject> doneDueYesterday = coupleOfDone(subtract(now, fromDays(1)));
            ImmutableList<StgProject> records =
                    ImmutableList.<StgProject>builder()
                            .addAll(coupleOfDone(subtract(now, fromDays(10))))
                            .addAll(doneDueYesterday)
                            .addAll(coupleOfDone(now))
                            .addAll(dozenOfRecords().values())  // in `CREATED` status.
                            .build();
            storage().writeBatch(records);

            RecordQuery<StgProjectId, StgProject> doneAndDueBeforeNow =
                    queryDoneProjects().where(due_date)
                                       .isLessThan(now)
                                       .withMask(idAndDueDate())
                                       .orderBy(due_date, DESC)
                                       .limit(2)
                                       .build();

            Iterator<StgProject> iterator = storage().readAll(doneAndDueBeforeNow);
            ImmutableList<StgProject> actual = ImmutableList.copyOf(iterator);
            RecordStorageDelegateTestEnv.assertHaveIds(actual, toIds(doneDueYesterday));

            for (StgProject readResult : actual) {
                assertOnlyIdAndDueDate(readResult);
            }
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("a single record by its ID")
        void recordById() {
            StgProject record = randomRecord();
            storage().write(record);
            Optional<StgProject> readResult = storage().read(record.getId());
            assertThat(readResult).isPresent();

            boolean deleted = storage().delete(record.getId());
            assertThat(deleted).isTrue();
            Optional<StgProject> anotherReadResult = storage().read(record.getId());
            assertThat(anotherReadResult).isEmpty();

            boolean deletedAgain = storage().delete(record.getId());
            assertThat(deletedAgain).isFalse();
        }

        @Test
        @DisplayName("several records by their IDs at once")
        void manyRecordByIds() {
            ImmutableMap<StgProjectId, StgProject> recordMap = dozenOfRecords();
            storage().writeBatch(recordMap.values());

            ImmutableSet<StgProjectId> ids = recordMap.keySet();
            ImmutableList<StgProjectId> partOfIds = RecordStorageDelegateTestEnv.halfDozenOf(ids);
            Iterator<StgProject> actualIterator = storage().readAll(partOfIds);
            ImmutableList<StgProject> actualRecords = ImmutableList.copyOf(actualIterator);

            RecordStorageDelegateTestEnv.assertHaveIds(actualRecords, partOfIds);

            storage().deleteAll(partOfIds);
            Iterator<StgProject> afterDeletion = storage().readAll(partOfIds);
            assertThat(afterDeletion.hasNext()).isFalse();

            Iterator<StgProject> iterator = storage().readAll();
            ImmutableList<StgProject> remainder = ImmutableList.copyOf(iterator);
            Sets.SetView<StgProjectId> expectedRemainedIds =
                    Sets.symmetricDifference(ids, ImmutableSet.copyOf(partOfIds));
            RecordStorageDelegateTestEnv.assertHaveIds(remainder, expectedRemainedIds);
        }
    }

    @Nested
    @DisplayName("throw an `IllegalStateException` if it is closed and the user invokes")
    @SuppressWarnings("ResultOfMethodCallIgnored")
            // as we just call the method!
    class ThrowIseIfClosed {

        @BeforeEach
        void closeStorage() {
            storage().close();
        }

        @Test
        @DisplayName("`write(record)` method")
        void write() {
            assertThrows(IllegalStateException.class, () -> storage().write(randomRecord()));
        }

        @Test
        @DisplayName("`writeAll(Iterable)` method")
        void writeAll() {
            assertThrows(IllegalStateException.class,
                         () -> storage().writeBatch(ImmutableSet.of(randomRecord()))
            );
        }

        @Test
        @DisplayName("`write(id, record)` method")
        void writeIdRecord() {
            StgProject record = randomRecord();
            assertThrows(IllegalStateException.class,
                         () -> storage().write(record.getId(), record)
            );
        }

        @Test
        @DisplayName("`read(id, FieldMask)` method")
        void readIdFieldMask() {
            assertThrows(IllegalStateException.class,
                         () -> storage().read(newId(), idAndDueDate())
            );
        }

        @Test
        @DisplayName("`readAll()` method")
        void readAll() {
            assertThrows(IllegalStateException.class,
                         () -> storage().readAll()
            );
        }

        @Test
        @DisplayName("`readAll(RecordQuery)` method")
        void readAllByQuery() {
            RecordQuery<StgProjectId, StgProject> query = queryBuilder().build();
            assertThrows(IllegalStateException.class,
                         () -> storage().readAll(query)
            );
        }

        @Test
        @DisplayName("`readAll(IDs)` method")
        void readAllByIds() {
            assertThrows(IllegalStateException.class,
                         () -> storage().readAll(ImmutableSet.of(newId(), newId()))
            );
        }

        @Test
        @DisplayName("`readAll(IDs, FieldMask)` method")
        void readAllByIdsAndMask() {
            assertThrows(IllegalStateException.class,
                         () -> storage().readAll(ImmutableSet.of(newId()),
                                                 idAndDueDate())
            );
        }

        @Test
        @DisplayName("`delete(ID)` method")
        void delete() {
            assertThrows(IllegalStateException.class,
                         () -> storage().delete(newId())
            );
        }

        @Test
        @DisplayName("`deleteAll(IDs)` method")
        void deleteAll() {
            assertThrows(IllegalStateException.class,
                         () -> storage().deleteAll(ImmutableList.of(newId(), newId()))
            );
        }
    }

    private StgProject randomRecord() {
        return newStorageRecord(RecordStorageDelegateTestEnv.generateId());
    }

    private static RecordQueryBuilder<StgProjectId, StgProject> queryBuilder() {
        return RecordQuery.newBuilder(StgProjectId.class, StgProject.class);
    }
}
