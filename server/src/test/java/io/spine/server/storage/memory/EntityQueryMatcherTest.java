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

package io.spine.server.storage.memory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.ColumnFilter;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.CompositeQueryParameter;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.QueryParameters;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.ImmutableMultimap.of;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.server.entity.storage.EntityRecordWithColumns.of;
import static io.spine.server.entity.storage.TestCompositeQueryParameterFactory.createParams;
import static io.spine.server.entity.storage.TestEntityQueryFactory.createQuery;
import static io.spine.server.entity.storage.TestEntityRecordWithColumnsFactory.createRecord;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("EntityQueryMatcher should")
class EntityQueryMatcherTest {

    @Test
    @DisplayName("match everything except null to empty query")
    void matchEverythingToEmpty() {
        Collection<Object> idFilter = Collections.emptyList();
        EntityQuery<?> query = createQuery(idFilter, defaultQueryParameters());

        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        assertFalse(matcher.apply(null));
        assertTrue(matcher.apply(of(EntityRecord.getDefaultInstance())));
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
        EntityRecordWithColumns matchingRecord = of(matching);
        EntityRecordWithColumns nonMatchingRecord = of(nonMatching);
        assertTrue(matcher.apply(matchingRecord));
        assertFalse(matcher.apply(nonMatchingRecord));
    }

    @SuppressWarnings({"unchecked",           // Mocks <-> reflection issues
                       "ConstantConditions"}) // Test data is constant
    @Test
    @DisplayName("match columns")
    void matchColumns() {
        String targetName = "feature";
        Serializable acceptedValue = true;
        EntityColumn target = mock(EntityColumn.class);
        when(target.isNullable()).thenReturn(true);
        when(target.getStoredName()).thenReturn(targetName);
        when(target.getType()).thenReturn(Boolean.class);
        when(target.toPersistedValue(any())).thenReturn(acceptedValue);

        Collection<Object> ids = Collections.emptyList();

        Multimap<EntityColumn, ColumnFilter> filters = of(target, eq(targetName, acceptedValue));
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
        EntityColumn.MemoizedValue storedValue = mock(EntityColumn.MemoizedValue.class);
        when(storedValue.getSourceColumn()).thenReturn(target);
        when(storedValue.getValue()).thenReturn(acceptedValue);
        Map<String, EntityColumn.MemoizedValue> matchingColumns =
                ImmutableMap.of(targetName, storedValue);
        EntityRecordWithColumns nonMatchingRecord = of(nonMatching);
        EntityRecordWithColumns matchingRecord = createRecord(matching, matchingColumns);

        assertTrue(matcher.apply(matchingRecord));
        assertFalse(matcher.apply(nonMatchingRecord));
    }

    @Test
    @DisplayName("match Any instances")
    void matchAnyInstances() {
        String columnName = "column";

        Project someMessage = Sample.messageOfType(Project.class);
        Any actualValue = AnyPacker.pack(someMessage);

        EntityColumn column = mock(EntityColumn.class);
        when(column.getType()).thenReturn(Any.class);
        when(column.getStoredName()).thenReturn(columnName);
        when(column.toPersistedValue(any())).thenReturn(actualValue);

        EntityColumn.MemoizedValue value = mock(EntityColumn.MemoizedValue.class);
        when(value.getSourceColumn()).thenReturn(column);
        when(value.getValue()).thenReturn(actualValue);

        EntityRecord record = Sample.messageOfType(EntityRecord.class);
        Map<String, EntityColumn.MemoizedValue> columns = singletonMap(columnName, value);
        EntityRecordWithColumns recordWithColumns = createRecord(record, columns);

        Multimap<EntityColumn, ColumnFilter> filters = of(column, eq(columnName, actualValue));
        CompositeQueryParameter parameter = createParams(filters, ALL);
        QueryParameters parameters = QueryParameters.newBuilder()
                                                    .add(parameter)
                                                    .build();
        EntityQuery<?> query = createQuery(emptySet(), parameters);

        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        assertTrue(matcher.apply(recordWithColumns));
    }

    @Test
    @DisplayName("not match by wrong field name")
    void notMatchByWrongField() {
        String wrongName = "wrong";
        EntityColumn target = mock(EntityColumn.class);

        Multimap<EntityColumn, ColumnFilter> filters = of(target, eq(wrongName, "any"));
        CompositeQueryParameter parameter = createParams(filters, EITHER);
        QueryParameters params = QueryParameters.newBuilder()
                                                .add(parameter)
                                                .build();
        EntityQuery<?> query = createQuery(Collections.emptyList(), params);
        EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        EntityRecord record = EntityRecord.newBuilder()
                                          .setEntityId(Any.getDefaultInstance())
                                          .build();
        EntityRecordWithColumns recordWithColumns = of(record);
        assertFalse(matcher.apply(recordWithColumns));
    }

    private static QueryParameters defaultQueryParameters() {
        return QueryParameters.newBuilder()
                              .build();
    }
}
