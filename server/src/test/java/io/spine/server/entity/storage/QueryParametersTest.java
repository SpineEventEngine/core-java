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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.testing.EqualsTester;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.client.ColumnFilter;
import io.spine.client.ColumnFilters;
import io.spine.server.entity.VersionableEntity;
import io.spine.server.entity.storage.given.QueryParametersTestEnv;
import io.spine.server.storage.RecordStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMultimap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.le;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.CompositeQueryParameter.from;
import static io.spine.server.entity.storage.QueryParameters.newBuilder;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("QueryParameters should")
class QueryParametersTest {

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        String columnName = version.name();
        EntityColumn column = findColumn(VersionableEntity.class, columnName);
        ColumnFilter filter = ColumnFilters.eq(columnName, 1);
        CompositeQueryParameter parameter = aggregatingParameter(column, filter);
        QueryParameters parameters = QueryParameters.newBuilder()
                                                    .add(parameter)
                                                    .build();
        reserializeAndAssert(parameters);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        // --- Group A ---
        // Consists of 2 empty instances
        QueryParameters paramsA1 = newBuilder().build();
        QueryParameters paramsA2 = newBuilder().build();

        // --- Group B ---
        // Consists of 3 instances with a single filter targeting a String column
        EntityColumn bColumn = QueryParametersTestEnv.mockColumn();
        ColumnFilter bFilter = ColumnFilters.eq("b", "c");
        QueryParameters paramsB1 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                               .build();
        QueryParameters paramsB2 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                               .build();
        QueryParameters paramsB3 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                               .build();

        // --- Group C ---
        // Consists of an instance with a single filter targeting an integer number column
        EntityColumn cColumn = QueryParametersTestEnv.mockColumn();
        ColumnFilter cFilter = ColumnFilters.eq("a", 42);
        QueryParameters paramsC = newBuilder().add(aggregatingParameter(cColumn, cFilter))
                                              .build();

        // --- Check ---
        new EqualsTester().addEqualityGroup(paramsA1, paramsA2)
                          .addEqualityGroup(paramsB1, paramsB2, paramsB3)
                          .addEqualityGroup(paramsC)
                          .testEquals();
    }

    @Test
    @DisplayName("be constructed from empty builder")
    void constructFromEmptyBuilder() {
        QueryParameters parameters = newBuilder().build();
        assertNotNull(parameters);
    }

    @Test
    @DisplayName("produce iterator over filters")
    void produceFilterIterator() {
        ColumnFilter[] filters = {
                eq("firstFilter", 1),
                eq("secondFilter", 42),
                gt("thirdFilter", getCurrentTime())};
        Multimap<EntityColumn, ColumnFilter> columnFilters = of(QueryParametersTestEnv.mockColumn(), filters[0],
                                                                QueryParametersTestEnv.mockColumn(), filters[1],
                                                                QueryParametersTestEnv.mockColumn(), filters[2]);
        CompositeQueryParameter parameter = from(columnFilters, ALL);
        QueryParameters parameters = newBuilder().add(parameter)
                                                 .build();
        Collection<ColumnFilter> results = newLinkedList();
        for (CompositeQueryParameter queryParameter : parameters) {
            results.addAll(queryParameter.getFilters()
                                         .values());
        }
        assertArrayEquals(filters, results.toArray());
    }

    @Test
    @DisplayName("retrieve filter by column")
    void retrieveFilterByColumn() {
        ColumnFilter[] filters = {
                eq("$1nd", 42.0),
                eq("$2st", "entityColumnValue"),
                gt("$3d", getCurrentTime())};
        EntityColumn[] columns = {QueryParametersTestEnv.mockColumn(), QueryParametersTestEnv.mockColumn(), QueryParametersTestEnv.mockColumn()};
        Multimap<EntityColumn, ColumnFilter> columnFilters = of(columns[0], filters[0],
                                                                columns[1], filters[1],
                                                                columns[2], filters[2]);
        CompositeQueryParameter parameter = from(columnFilters, ALL);
        QueryParameters parameters = newBuilder().add(parameter)
                                                 .build();
        CompositeQueryParameter singleParameter = parameters.iterator()
                                                            .next();
        Multimap<EntityColumn, ColumnFilter> actualFilters = singleParameter.getFilters();
        for (int i = 0; i < columns.length; i++) {
            EntityColumn column = columns[i];
            Collection<ColumnFilter> readFilters = actualFilters.get(column);
            assertThat(readFilters).hasSize(1);
            assertEquals(filters[i], readFilters.iterator()
                                                .next());
        }
    }

    @Test
    @DisplayName("keep multiple filters for single column")
    void keepManyFiltersForColumn() throws ParseException {
        String columnName = "time";
        EntityColumn column = mock(EntityColumn.class);

        // Some valid Timestamp values
        Timestamp startTime = Timestamps.parse("2000-01-01T10:00:00.000-05:00");
        Timestamp deadline = Timestamps.parse("2017-01-01T10:00:00.000-05:00");

        ColumnFilter startTimeFilter = gt(columnName, startTime);
        ColumnFilter deadlineFilter = le(columnName, deadline);
        Multimap<EntityColumn, ColumnFilter> columnFilters =
                ImmutableMultimap.<EntityColumn, ColumnFilter>builder()
                        .put(column, startTimeFilter)
                        .put(column, deadlineFilter)
                        .build();
        CompositeQueryParameter parameter = from(columnFilters, ALL);
        QueryParameters parameters = newBuilder().add(parameter)
                                                 .build();
        List<CompositeQueryParameter> aggregatingParameters = newArrayList(parameters);
        assertThat(aggregatingParameters).hasSize(1);
        Multimap<EntityColumn, ColumnFilter> actualColumnFilters =
                aggregatingParameters.get(0)
                                     .getFilters();
        Collection<ColumnFilter> timeFilters = actualColumnFilters.get(column);

        IterableSubject assertTimeFilters = assertThat(timeFilters);
        assertTimeFilters.hasSize(2);
        assertTimeFilters.containsExactly(startTimeFilter, deadlineFilter);
    }

    @Test
    @DisplayName("create an parameters with active lifecycle flags")
    void createActiveParams() {
        RecordStorage storage = mock(RecordStorage.class);
        Map<String, EntityColumn> columns = newHashMap();

        String archivedStoredName = "archived-stored";
        EntityColumn archivedColumn = QueryParametersTestEnv.mockColumn(archived, archivedStoredName);
        columns.put(archived.name(), archivedColumn);

        String deletedStoredName = "deleted-stored";
        EntityColumn deletedColumn = QueryParametersTestEnv.mockColumn(deleted, deletedStoredName);
        columns.put(deleted.name(), deletedColumn);

        when(storage.entityLifecycleColumns()).thenReturn(columns);

        QueryParameters parameters = QueryParameters.activeEntityQueryParams(storage);

        assertTrue(parameters.isLifecycleAttributesSet());
        assertFalse(parameters.limited());
        assertFalse(parameters.ordered());

        Iterator<CompositeQueryParameter> paramsIterator = parameters.iterator();
        CompositeQueryParameter lifecycleParameter = paramsIterator.next();
        assertFalse(paramsIterator.hasNext());
        assertTrue(lifecycleParameter.hasLifecycle());
        assertEquals(ALL, lifecycleParameter.getOperator());
        ImmutableMultimap<EntityColumn, ColumnFilter> filters = lifecycleParameter.getFilters();

        ImmutableCollection<ColumnFilter> archivedFilters = filters.get(archivedColumn);
        UnmodifiableIterator<ColumnFilter> archivedFilterIterator = archivedFilters.iterator();
        assertEquals(eq(archivedStoredName, false), archivedFilterIterator.next());
        assertFalse(archivedFilterIterator.hasNext());

        ImmutableCollection<ColumnFilter> deletedFilters = filters.get(deletedColumn);
        UnmodifiableIterator<ColumnFilter> deletedFilterIterator = deletedFilters.iterator();
        assertEquals(eq(deletedStoredName, false), deletedFilterIterator.next());
        assertFalse(deletedFilterIterator.hasNext());
    }

    private static CompositeQueryParameter aggregatingParameter(EntityColumn column,
                                                                ColumnFilter columnFilter) {
        Multimap<EntityColumn, ColumnFilter> filter = of(column, columnFilter);
        CompositeQueryParameter result = from(filter, ALL);
        return result;
    }
}
