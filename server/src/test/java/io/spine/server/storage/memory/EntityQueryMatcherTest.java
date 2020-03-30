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
import com.google.protobuf.Message;
import io.spine.client.Filter;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import io.spine.server.storage.Column;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filters.eq;
import static io.spine.server.entity.storage.TestCompositeQueryParameterFactory.createParams;
import static io.spine.server.entity.storage.TestEntityQueryFactory.createQuery;
import static io.spine.server.storage.memory.given.EntityQueryMatcherTestEnv.anyColumn;
import static io.spine.server.storage.memory.given.EntityQueryMatcherTestEnv.anyValue;
import static io.spine.server.storage.memory.given.EntityQueryMatcherTestEnv.booleanColumn;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EntityQueryMatcher should")
class EntityQueryMatcherTest {

    @Test
    @DisplayName("match everything except null to empty query")
    void matchEverythingToEmpty() {
        Collection<Object> idFilter = Collections.emptyList();
        EntityQuery<?> query = createQuery(idFilter, defaultQueryParameters());

        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        assertFalse(matcher.test(null));
        assertTrue(matcher.test(EntityRecordWithColumns.of(EntityRecord.getDefaultInstance(),
                                                           Collections.emptyMap())));
    }

    @Test
    @DisplayName("match IDs")
    void matchIds() {
        Message genericId = Sample.messageOfType(ProjectId.class);
        Collection<Object> idFilter = singleton(genericId);
        Any entityId = AnyPacker.pack(genericId);
        EntityQuery<?> query = createQuery(idFilter, defaultQueryParameters());

        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        EntityRecord matching = EntityRecord.newBuilder()
                                            .setEntityId(entityId)
                                            .build();
        Any otherEntityId = AnyPacker.pack(Sample.messageOfType(ProjectId.class));
        EntityRecord nonMatching = EntityRecord.newBuilder()
                                               .setEntityId(otherEntityId)
                                               .build();
        EntityRecordWithColumns matchingRecord =
                EntityRecordWithColumns.of(matching, Collections.emptyMap());
        EntityRecordWithColumns nonMatchingRecord =
                EntityRecordWithColumns.of(nonMatching, Collections.emptyMap());
        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match columns")
    void matchColumns() {
        Column column = booleanColumn();
        boolean actualValue = false;

        ColumnName columnName = column.name();

        Collection<Object> ids = Collections.emptyList();

        Multimap<Column, Filter> filters =
                ImmutableMultimap.of(column, eq(columnName.value(), actualValue));
        CompositeQueryParameter parameter = createParams(filters, ALL);
        QueryParameters params = QueryParameters.newBuilder()
                                                .add(parameter)
                                                .build();
        EntityQuery<?> query = createQuery(ids, params);

        Any matchingId = AnyPacker.pack(Sample.messageOfType(TaskId.class));
        Any nonMatchingId = AnyPacker.pack(Sample.messageOfType(TaskId.class));

        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        EntityRecord matching = EntityRecord.newBuilder()
                                            .setEntityId(matchingId)
                                            .build();
        EntityRecord nonMatching = EntityRecord.newBuilder()
                                               .setEntityId(nonMatchingId)
                                               .build();
        Map<ColumnName, Object> matchingColumns =
                ImmutableMap.of(columnName, actualValue);
        EntityRecordWithColumns nonMatchingRecord =
                EntityRecordWithColumns.of(nonMatching, Collections.emptyMap());
        EntityRecordWithColumns matchingRecord =
                EntityRecordWithColumns.of(matching, matchingColumns);

        assertTrue(matcher.test(matchingRecord));
        assertFalse(matcher.test(nonMatchingRecord));
    }

    @Test
    @DisplayName("match Any instances")
    void matchAnyInstances() {
        Column column = anyColumn();
        Any actualValue = anyValue();

        ColumnName columnName = column.name();

        EntityRecord record = Sample.messageOfType(EntityRecord.class);
        Map<ColumnName, Object> columns = singletonMap(columnName, actualValue);

        EntityRecordWithColumns recordWithColumns = EntityRecordWithColumns.of(record, columns);

        Multimap<Column, Filter> filters =
                ImmutableMultimap.of(column, eq(columnName.value(), actualValue));
        CompositeQueryParameter parameter = createParams(filters, ALL);
        QueryParameters parameters = QueryParameters.newBuilder()
                                                    .add(parameter)
                                                    .build();
        EntityQuery<?> query = createQuery(emptySet(), parameters);

        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        assertTrue(matcher.test(recordWithColumns));
    }

    @Test
    @DisplayName("not match by wrong field name")
    void notMatchByWrongField() {
        String wrongName = "wrong";
        Column target = booleanColumn();

        Multimap<Column, Filter> filters =
                ImmutableMultimap.of(target, eq(wrongName, "any"));
        CompositeQueryParameter parameter = createParams(filters, EITHER);
        QueryParameters params = QueryParameters.newBuilder()
                                                .add(parameter)
                                                .build();
        EntityQuery<?> query = createQuery(Collections.emptyList(), params);
        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        EntityRecord record = EntityRecord.newBuilder()
                                          .setEntityId(Any.getDefaultInstance())
                                          .build();
        EntityRecordWithColumns recordWithColumns =
                EntityRecordWithColumns.of(record, Collections.emptyMap());
        assertFalse(matcher.test(recordWithColumns));
    }

    private static QueryParameters defaultQueryParameters() {
        return QueryParameters.newBuilder()
                              .build();
    }
}
