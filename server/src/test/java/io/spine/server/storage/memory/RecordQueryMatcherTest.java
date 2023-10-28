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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableMap;
import io.spine.base.Identifier;
import io.spine.protobuf.AnyPacker;
import io.spine.query.ColumnName;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.StorageConverter;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.SpecScanner;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.given.GivenStorageProject;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import io.spine.testdata.Sample;
import io.spine.testing.core.given.GivenVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.spine.base.Identifier.pack;
import static io.spine.base.Identifier.pack;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.anyColumn;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.anyValue;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.booleanColumn;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.recordSubject;
import static io.spine.testing.TestValues.nullRef;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`RecordQueryMatcher` should")
class RecordQueryMatcherTest {

    private static final MessageRecordSpec<StgProjectId, EntityRecord> spec =
            SpecScanner.scan(StgProjectId.class, StgProject.class);

    @Test
    @DisplayName("match everything except `null` to empty query")
    void matchEverythingToEmpty() {
        var sampleSubject = recordSubject();
        RecordQueryMatcher<?, EntityRecord> matcher = new RecordQueryMatcher<>(sampleSubject);

        assertFalse(matcher.test(nullRef()));
        assertTrue(matcher.test(RecordWithColumns.create(sampleEntityRecord(), ));
    }

    @Test
    @DisplayName("match IDs")
    void matchIds() {
        var genericId = Sample.messageOfType(ProjectId.class);
        var subject = recordSubject(genericId);

        var matcher = new RecordQueryMatcher<>(subject);
        var matching = sampleEntityRecord(genericId);
        var nonMatching = sampleEntityRecord(Sample.messageOfType(StgProject.class));
        var matchingRecord = EntityRecordWithColumns.<ProjectId>of(matching);
        var nonMatchingRecord = EntityRecordWithColumns.<ProjectId>of(nonMatching);
        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match columns")
    void matchColumns() {
        var column = booleanColumn();
        var actualValue = false;
        var columnName = column.name();
        var query = newBuilder().where(column).is(actualValue).build();

        var matcher = new RecordQueryMatcher<>(query.subject());

        var matching = sampleEntityRecord();
        Map<ColumnName, Object> matchingColumns = ImmutableMap.of(columnName, actualValue);
        var matchingRecord = EntityRecordWithColumns.of(matching, matchingColumns);

        var nonMatching = sampleEntityRecord(Sample.messageOfType(TaskId.class));
        var nonMatchingRecord = EntityRecordWithColumns.of(nonMatching);

        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match `Any` instances")
    void matchAnyInstances() {
        var column = anyColumn();
        var actualValue = anyValue();

        var columnName = column.name();

        var record = sampleEntityRecord();
        Map<ColumnName, Object> columns = singletonMap(columnName, actualValue);
        var recordAndCols = EntityRecordWithColumns.of(record, columns);
        var query = newBuilder().where(column).is(actualValue).build();
        var matcher = new RecordQueryMatcher<>(query);
        assertTrue(matcher.test(recordAndCols));
    }

    @Test
    @DisplayName("not match by wrong field name")
    void notMatchByWrongField() {
        var target = booleanColumn("some_random_name");
        var query = newBuilder().where(target).is(true).build();
        var matcher = new RecordQueryMatcher<>(query);

        var record = sampleEntityRecord();
        var recordWithColumns = EntityRecordWithColumns.of(record);
        assertFalse(matcher.test(recordWithColumns));
    }

    private static RecordQueryBuilder<Object, EntityRecord> newBuilder() {
        return RecordQuery.newBuilder(Object.class, EntityRecord.class);
    }

    private static EntityRecord sampleEntityRecord() {
        var id = GivenStorageProject.newId();
        return sampleEntityRecord(id);
    }

    private static EntityRecord sampleEntityRecord(StgProjectId id) {
        var state = GivenStorageProject.newState(id);
        return EntityRecord.newBuilder()
                .setEntityId(Identifier.pack(id))
                .setState(pack(state))
                .setVersion(GivenVersion.withNumber(11))
                .build();
    }
}
