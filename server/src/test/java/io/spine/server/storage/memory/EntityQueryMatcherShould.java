/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import org.junit.Test;

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
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public class EntityQueryMatcherShould {

    @Test
    public void match_everything_except_null_to_empty_query() {
        final Collection<Object> idFilter = Collections.emptyList();
        final EntityQuery<?> query = createQuery(idFilter, defaultQueryParameters());

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        assertFalse(matcher.apply(null));
        assertTrue(matcher.apply(of(EntityRecord.getDefaultInstance())));
    }

    @Test
    public void match_ids() {
        final Message genericId = Sample.messageOfType(ProjectId.class);
        final Collection<Object> idFilter = Collections.<Object>singleton(genericId);
        final Any entityId = AnyPacker.pack(genericId);
        final EntityQuery<?> query = createQuery(idFilter, defaultQueryParameters());

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        final EntityRecord matching = EntityRecord.newBuilder()
                                                  .setEntityId(entityId)
                                                  .build();
        final Any otherEntityId = AnyPacker.pack(Sample.messageOfType(ProjectId.class));
        final EntityRecord nonMatching = EntityRecord.newBuilder()
                                                     .setEntityId(otherEntityId)
                                                     .build();
        final EntityRecordWithColumns matchingRecord = of(matching);
        final EntityRecordWithColumns nonMatchingRecord = of(nonMatching);
        assertTrue(matcher.apply(matchingRecord));
        assertFalse(matcher.apply(nonMatchingRecord));
    }

    @SuppressWarnings({"unchecked",           // Mocks <-> reflection issues
                       "ConstantConditions"}) // Test data is constant
    @Test
    public void match_columns() {
        final String targetName = "feature";
        final Serializable acceptedValue = true;

        final EntityColumn target = mock(EntityColumn.class);
        when(target.isNullable()).thenReturn(true);
        when(target.getStoredName()).thenReturn(targetName);
        when(target.getType()).thenReturn(Boolean.class);
        when(target.toPersistenceValue(any())).thenReturn(acceptedValue);

        final Collection<Object> ids = Collections.emptyList();

        final Multimap<EntityColumn, ColumnFilter> filters = of(target,
                                                                eq(targetName, acceptedValue));
        final CompositeQueryParameter parameter = createParams(filters, ALL);
        final QueryParameters params = QueryParameters.newBuilder()
                                                      .add(parameter)
                                                      .build();
        final EntityQuery<?> query = createQuery(ids, params);

        final Any matchingId = AnyPacker.pack(Sample.messageOfType(TaskId.class));
        final Any nonMatchingId = AnyPacker.pack(Sample.messageOfType(TaskId.class));

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        final EntityRecord matching = EntityRecord.newBuilder()
                                                  .setEntityId(matchingId)
                                                  .build();
        final EntityRecord nonMatching = EntityRecord.newBuilder()
                                                     .setEntityId(nonMatchingId)
                                                     .build();
        final EntityColumn.MemoizedValue storedValue = mock(EntityColumn.MemoizedValue.class);
        when(storedValue.getSourceColumn()).thenReturn(target);
        when(storedValue.getValue()).thenReturn(acceptedValue);
        final Map<String, EntityColumn.MemoizedValue> matchingColumns =
                ImmutableMap.of(targetName, storedValue);
        final EntityRecordWithColumns nonMatchingRecord = of(nonMatching);
        final EntityRecordWithColumns matchingRecord = createRecord(matching, matchingColumns);

        assertTrue(matcher.apply(matchingRecord));
        assertFalse(matcher.apply(nonMatchingRecord));
    }

    @Test
    public void match_Any_instances() {
        final String columnName = "column";

        final Project someMessage = Sample.messageOfType(Project.class);
        final Any actualValue = AnyPacker.pack(someMessage);

        final EntityColumn column = mock(EntityColumn.class);
        when(column.getType()).thenReturn(Any.class);
        when(column.getStoredName()).thenReturn(columnName);
        when(column.toPersistenceValue(any())).thenReturn(actualValue);

        final EntityColumn.MemoizedValue value = mock(EntityColumn.MemoizedValue.class);
        when(value.getSourceColumn()).thenReturn(column);
        when(value.getValue()).thenReturn(actualValue);

        final EntityRecord record = Sample.messageOfType(EntityRecord.class);
        final Map<String, EntityColumn.MemoizedValue> columns = singletonMap(columnName, value);
        final EntityRecordWithColumns recordWithColumns = createRecord(record, columns);

        final Multimap<EntityColumn, ColumnFilter> filters = of(column,
                                                                eq(columnName, actualValue));
        final CompositeQueryParameter parameter = createParams(filters, ALL);
        final QueryParameters parameters = QueryParameters.newBuilder()
                                                          .add(parameter)
                                                          .build();
        final EntityQuery<?> query = createQuery(emptySet(), parameters);

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        assertTrue(matcher.apply(recordWithColumns));
    }

    @Test
    public void not_match_by_wrong_field_name() {
        final String wrongName = "wrong";
        final EntityColumn target = mock(EntityColumn.class);

        final Multimap<EntityColumn, ColumnFilter> filters = of(target,
                                                                eq(wrongName, "any"));
        final CompositeQueryParameter parameter = createParams(filters, EITHER);
        final QueryParameters params = QueryParameters.newBuilder()
                                                      .add(parameter)
                                                      .build();
        final EntityQuery<?> query = createQuery(Collections.emptyList(), params);
        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        final EntityRecord record = EntityRecord.newBuilder()
                                                .setEntityId(Any.getDefaultInstance())
                                                .build();
        final EntityRecordWithColumns recordWithColumns = of(record);
        assertFalse(matcher.apply(recordWithColumns));
    }

    private static QueryParameters defaultQueryParameters() {
        return QueryParameters.newBuilder()
                              .build();
    }
}
