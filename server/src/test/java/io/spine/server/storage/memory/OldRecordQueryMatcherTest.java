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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import io.spine.base.Identifier;
import io.spine.client.Filter;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.OldColumnName;
import io.spine.server.storage.CompositeQueryParameter;
import io.spine.server.storage.OldColumn;
import io.spine.server.storage.OldRecordQuery;
import io.spine.server.storage.QueryParameters;
import io.spine.server.storage.RecordQueries;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filters.eq;
import static io.spine.server.entity.storage.TestCompositeQueryParameterFactory.createParams;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.anyColumn;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.anyValue;
import static io.spine.server.storage.memory.given.RecordQueryMatcherTestEnv.booleanColumn;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`RecordQueryMatcher` should")
class OldRecordQueryMatcherTest {

    @Test
    @DisplayName("match everything except null to empty query")
    void matchEverythingToEmpty() {
        Collection<Object> idFilter = Collections.emptyList();
        OldRecordQuery<?> query = RecordQueries.of(idFilter);

        RecordQueryMatcher<?, EntityRecord> matcher = new RecordQueryMatcher<>(query);

        assertFalse(matcher.test(null));
        assertTrue(matcher.test(EntityRecordWithColumns.of(sampleEntityRecord())));
    }

    @Test
    @DisplayName("match IDs")
    void matchIds() {
        ProjectId genericId = Sample.messageOfType(ProjectId.class);
        Collection<ProjectId> idFilter = singleton(genericId);
        OldRecordQuery<ProjectId> query = RecordQueries.of(idFilter);

        RecordQueryMatcher<ProjectId, EntityRecord> matcher = new RecordQueryMatcher<>(query);
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
        OldColumn column = booleanColumn();
        boolean actualValue = false;

        OldColumnName columnName = column.name();
        QueryParameters params = QueryParameters.eq(column, actualValue);
        OldRecordQuery<TaskId> query = RecordQueries.of(params);

        RecordQueryMatcher<TaskId, EntityRecord> matcher = new RecordQueryMatcher<>(query);

        EntityRecord matching = sampleEntityRecord(Sample.messageOfType(TaskId.class));
        Map<OldColumnName, Object> matchingColumns = ImmutableMap.of(columnName, actualValue);
        EntityRecordWithColumns<TaskId> matchingRecord =
                EntityRecordWithColumns.of(matching, matchingColumns);

        EntityRecord nonMatching = sampleEntityRecord(Sample.messageOfType(TaskId.class));
        EntityRecordWithColumns<TaskId> nonMatchingRecord = EntityRecordWithColumns.of(nonMatching);

        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match `Any` instances")
    void matchAnyInstances() {
        OldColumn column = anyColumn();
        Any actualValue = anyValue();

        OldColumnName columnName = column.name();

        EntityRecord record = sampleEntityRecord();
        Map<OldColumnName, Object> columns = singletonMap(columnName, actualValue);
        EntityRecordWithColumns<String> recordAndCols = EntityRecordWithColumns.of(record, columns);

        QueryParameters parameters = QueryParameters.eq(column, actualValue);
        OldRecordQuery<String> query = RecordQueries.of(parameters);

        RecordQueryMatcher<String, EntityRecord> matcher = new RecordQueryMatcher<>(query);
        assertTrue(matcher.test(recordAndCols));
    }

    @Test
    @DisplayName("not match by wrong field name")
    void notMatchByWrongField() {
        String wrongName = "wrong";
        OldColumn target = booleanColumn();

        Multimap<OldColumn, Filter> filters =
                ImmutableMultimap.of(target, eq(wrongName, "any"));
        CompositeQueryParameter parameter = createParams(filters, EITHER);
        QueryParameters params = QueryParameters.newBuilder()
                                                .add(parameter)
                                                .build();
        OldRecordQuery<String> query = RecordQueries.of(params);
        RecordQueryMatcher<String, EntityRecord> matcher = new RecordQueryMatcher<>(query);

        EntityRecord record = sampleEntityRecord();
        EntityRecordWithColumns<String> recordWithColumns = EntityRecordWithColumns.of(record);
        assertFalse(matcher.test(recordWithColumns));
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
