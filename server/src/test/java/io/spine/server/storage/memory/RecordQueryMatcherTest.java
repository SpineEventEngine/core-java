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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.query.ColumnName;
import io.spine.query.RecordColumn;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.query.Subject;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.anyColumn;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.anyValue;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.booleanColumn;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.recordSubject;
import static io.spine.testing.Tests.nullRef;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`RecordQueryMatcher` should")
class RecordQueryMatcherTest {

    @Test
    @DisplayName("match everything except `null` to empty query")
    void matchEverythingToEmpty() {
        Subject<Object, EntityRecord> sampleSubject = recordSubject();
        RecordQueryMatcher<?, EntityRecord> matcher = new RecordQueryMatcher<>(sampleSubject);

        assertFalse(matcher.test(nullRef()));
        assertTrue(matcher.test(EntityRecordWithColumns.of(sampleEntityRecord())));
    }

    @Test
    @DisplayName("match IDs")
    void matchIds() {
        ProjectId genericId = Sample.messageOfType(ProjectId.class);
        Subject<ProjectId, EntityRecord> subject = recordSubject(genericId);

        RecordQueryMatcher<ProjectId, EntityRecord> matcher = new RecordQueryMatcher<>(subject);
        EntityRecord matching = sampleEntityRecord(genericId);
        EntityRecord nonMatching = sampleEntityRecord(Sample.messageOfType(ProjectId.class));
        EntityRecordWithColumns<ProjectId> matchingRecord = EntityRecordWithColumns.of(matching);
        EntityRecordWithColumns<ProjectId> nonMatchingRecord =
                EntityRecordWithColumns.of(nonMatching);
        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match columns")
    void matchColumns() {
        RecordColumn<EntityRecord, Boolean> column = booleanColumn();
        boolean actualValue = false;
        ColumnName columnName = column.name();
        RecordQuery<Object, EntityRecord> query =
                newBuilder()
                        .where(column)
                        .is(actualValue)
                        .build();

        RecordQueryMatcher<Object, EntityRecord> matcher =
                new RecordQueryMatcher<>(query.subject());

        EntityRecord matching = sampleEntityRecord(Sample.messageOfType(TaskId.class));
        Map<ColumnName, Object> matchingColumns = ImmutableMap.of(columnName, actualValue);
        EntityRecordWithColumns<Object> matchingRecord =
                EntityRecordWithColumns.of(matching, matchingColumns);

        EntityRecord nonMatching = sampleEntityRecord(Sample.messageOfType(TaskId.class));
        EntityRecordWithColumns<Object> nonMatchingRecord = EntityRecordWithColumns.of(nonMatching);

        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match `Any` instances")
    void matchAnyInstances() {
        RecordColumn<EntityRecord, Any> column = anyColumn();
        Any actualValue = anyValue();

        ColumnName columnName = column.name();

        EntityRecord record = sampleEntityRecord();
        Map<ColumnName, Object> columns = singletonMap(columnName, actualValue);
        EntityRecordWithColumns<Object> recordAndCols = EntityRecordWithColumns.of(record, columns);
        RecordQuery<Object, EntityRecord> query = newBuilder().where(column)
                                                              .is(actualValue)
                                                              .build();
        RecordQueryMatcher<Object, EntityRecord> matcher = new RecordQueryMatcher<>(query);
        assertTrue(matcher.test(recordAndCols));
    }

    @Test
    @DisplayName("not match by wrong field name")
    void notMatchByWrongField() {
        RecordColumn<EntityRecord, Boolean> target = booleanColumn("some_random_name");
        RecordQuery<Object, EntityRecord> query =
                newBuilder()
                        .where(target)
                        .is(true)
                        .build();
        RecordQueryMatcher<Object, EntityRecord> matcher = new RecordQueryMatcher<>(query);

        EntityRecord record = sampleEntityRecord();
        EntityRecordWithColumns<Object> recordWithColumns = EntityRecordWithColumns.of(record);
        assertFalse(matcher.test(recordWithColumns));
    }

    private static RecordQueryBuilder<Object, EntityRecord> newBuilder() {
        return RecordQuery.newBuilder(Object.class, EntityRecord.class);
    }

    private static EntityRecord sampleEntityRecord() {
        return sampleEntityRecord(Identifier.newUuid());
    }

    private static EntityRecord sampleEntityRecord(Object id) {
        return EntityRecord.newBuilder()
                           .setEntityId(Identifier.pack(id))
                           .build();
    }
}
