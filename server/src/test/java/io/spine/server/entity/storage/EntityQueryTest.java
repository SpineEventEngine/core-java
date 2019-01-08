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

package io.spine.server.entity.storage;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.truth.StringSubject;
import io.spine.client.ColumnFilter;
import io.spine.client.ColumnFilters;
import io.spine.client.EntityIdFilter;
import io.spine.server.entity.EntityWithLifecycle;
import io.spine.test.entity.ProjectId;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.client.ColumnFilter.Operator.EQUAL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("EntityQuery should")
class EntityQueryTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(EntityIdFilter.class, EntityIdFilter.getDefaultInstance())
                .setDefault(QueryParameters.class, QueryParameters.newBuilder()
                                                                  .build())
                .testStaticMethods(EntityQuery.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        String columnName = deleted.name();
        EntityColumn column = findColumn(EntityWithLifecycle.class, columnName);
        ColumnFilter filter = ColumnFilters.eq(columnName, false);
        Multimap<EntityColumn, ColumnFilter> filters = ImmutableMultimap.of(column, filter);
        CompositeQueryParameter parameter = CompositeQueryParameter.from(filters, ALL);
        QueryParameters parameters = QueryParameters
                .newBuilder()
                .add(parameter)
                .build();
        Set<String> ids = singleton("my-awesome-ID");
        EntityQuery<String> query = EntityQuery.of(ids, parameters);
        reserializeAndAssert(query);
    }

    @Test
    @DisplayName("support `toString`")
    void supportToString() {
        Object someId = Sample.messageOfType(ProjectId.class);
        Collection<Object> ids = singleton(someId);
        EntityColumn someColumn = mockColumn();
        Object someValue = "something";

        Map<EntityColumn, Object> params = new HashMap<>(1);
        params.put(someColumn, someValue);

        EntityQuery query = EntityQuery.of(ids, paramsFromValues(params));

        StringSubject assertQuery = assertThat(query.toString());
        assertQuery.contains(query.getIds()
                                  .toString());
        assertQuery.contains(query.getParameters()
                                  .toString());
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        EqualsTester tester = new EqualsTester();
        addEqualityGroupA(tester);
        addEqualityGroupB(tester);
        addEqualityGroupC(tester);
        addEqualityGroupD(tester);
        tester.testEquals();
    }

    @Test
    @DisplayName("fail to append lifecycle columns if they are already present")
    void notDuplicateLifecycleColumns() {
        EntityColumn deletedColumn = Columns.findColumn(EntityWithLifecycle.class, deleted.name());
        CompositeQueryParameter queryParameter = CompositeQueryParameter.from(
                ImmutableMultimap.of(deletedColumn, ColumnFilter.getDefaultInstance()), ALL
        );
        QueryParameters parameters = QueryParameters
                .newBuilder()
                .add(queryParameter)
                .build();
        EntityQuery<String> query = EntityQuery.of(ImmutableSet.of(), parameters);
        assertFalse(query.canAppendLifecycleFlags());
    }

    /**
     * Adds an equality group containing a single Query of two IDs and two Query parameters to
     * the given {@link EqualsTester}.
     */
    private static void addEqualityGroupA(EqualsTester tester) {
        Collection<?> ids = Arrays.asList(Sample.messageOfType(ProjectId.class), 0);
        Map<EntityColumn, Object> params = new IdentityHashMap<>(2);
        params.put(mockColumn(), "anything");
        params.put(mockColumn(), 5);
        EntityQuery<?> query = EntityQuery.of(ids, paramsFromValues(params));
        tester.addEqualityGroup(query);
    }

    /**
     * Adds an equality group containing two Queries with no IDs and with a single Query parameter
     * to the given {@link EqualsTester}.
     */
    private static void addEqualityGroupB(EqualsTester tester) {
        Collection<?> ids = emptyList();
        Map<EntityColumn, Object> params = new HashMap<>(1);
        params.put(mockColumn(), 5);
        EntityQuery<?> query1 = EntityQuery.of(ids, paramsFromValues(params));
        EntityQuery<?> query2 = EntityQuery.of(ids, paramsFromValues(params));
        tester.addEqualityGroup(query1, query2);
    }

    /**
     * Adds an equality group containing two Queries with no IDs and with a single Query parameter
     * to the given {@link EqualsTester}.
     *
     * <p>The values of Query parameters are different than in
     * {@linkplain #addEqualityGroupB(EqualsTester) group C}.
     */
    private static void addEqualityGroupC(EqualsTester tester) {
        Collection<?> ids = emptySet();
        EntityColumn column = mockColumn();
        Object value = 42;
        Map<EntityColumn, Object> params = new HashMap<>(1);
        params.put(column, value);
        EntityQuery<?> query1 = EntityQuery.of(ids, paramsFromValues(params));
        EntityQuery<?> query2 = EntityQuery.of(ids, paramsFromValues(params));
        tester.addEqualityGroup(query1, query2);
    }

    /**
     * Adds an equality group containing a single Query with a single ID and no Query parameters
     * to the given {@link EqualsTester}.
     */
    private static void addEqualityGroupD(EqualsTester tester) {
        Collection<ProjectId> ids = singleton(Sample.messageOfType(ProjectId.class));
        Map<EntityColumn, Object> columns = Collections.emptyMap();
        EntityQuery<?> query = EntityQuery.of(ids, paramsFromValues(columns));
        tester.addEqualityGroup(query);
    }

    private static EntityColumn mockColumn() {
        @SuppressWarnings("unchecked") // Mock cannot have type parameters
        EntityColumn column = mock(EntityColumn.class);
        when(column.getName()).thenReturn("mockColumn");
        return column;
    }

    private static QueryParameters paramsFromValues(Map<EntityColumn, Object> values) {
        QueryParameters.Builder builder = QueryParameters.newBuilder();
        Multimap<EntityColumn, ColumnFilter> filters = HashMultimap.create(values.size(), 1);
        for (Map.Entry<EntityColumn, Object> param : values.entrySet()) {
            EntityColumn column = param.getKey();
            ColumnFilter filter = ColumnFilter
                    .newBuilder()
                    .setOperator(EQUAL)
                    .setValue(toAny(param.getValue()))
                    .setColumnName(column.getName())
                    .build();
            filters.put(column, filter);
        }
        return builder.add(CompositeQueryParameter.from(filters, ALL))
                      .build();
    }
}
