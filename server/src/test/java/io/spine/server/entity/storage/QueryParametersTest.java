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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.testing.EqualsTester;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.server.ContextSpec;
import io.spine.server.bc.given.ProjectProjection;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.test.bc.ProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Time.currentTime;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.Filters.eq;
import static io.spine.client.Filters.gt;
import static io.spine.client.Filters.le;
import static io.spine.server.entity.storage.given.SimpleColumn.column;
import static io.spine.server.entity.storage.given.SimpleColumn.intColumn;
import static io.spine.server.entity.storage.given.SimpleColumn.stringColumn;
import static io.spine.server.entity.storage.given.SimpleColumn.timestampColumn;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("QueryParameters should")
class QueryParametersTest {

    /**
     * Creates new {@code QueryParameters.Builder} instance.
     *
     * @apiNote Provided for brevity of tests while avoiding {@code BadImport} ErrorProne warning.
     */
    static QueryParameters.Builder newBuilder() {
        return QueryParameters.newBuilder();
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
        Column bColumn = column();
        Filter bFilter = Filters.eq("b", "c");
        QueryParameters paramsB1 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                               .build();
        QueryParameters paramsB2 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                               .build();
        QueryParameters paramsB3 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                               .build();

        // --- Group C ---
        // Consists of an instance with a single filter targeting an integer number column
        Column cColumn = column();
        Filter cFilter = Filters.eq("a", 42);
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
        Filter[] filters = {
                eq("firstFilter", 1),
                eq("secondFilter", 42),
                gt("thirdFilter", currentTime())};
        Multimap<Column, Filter> filterMap =
                ImmutableMultimap.of(column(), filters[0],
                                     column(), filters[1],
                                     column(), filters[2]);
        CompositeQueryParameter parameter = CompositeQueryParameter.from(filterMap, ALL);
        QueryParameters parameters = newBuilder().add(parameter)
                                                 .build();
        Collection<Filter> results = newLinkedList();
        for (CompositeQueryParameter queryParameter : parameters) {
            results.addAll(queryParameter.filters()
                                         .values());
        }
        assertArrayEquals(filters, results.toArray());
    }

    @Test
    @DisplayName("retrieve filter by column")
    void retrieveFilterByColumn() {
        Filter[] filters = {
                eq("$1st", "entityColumnValue"),
                eq("$2nd", 42.0),
                gt("$3d", currentTime())};
        Column[] columns = {stringColumn(), intColumn(), timestampColumn()};
        Multimap<Column, Filter> filterMap =
                ImmutableMultimap.of(columns[0], filters[0],
                                     columns[1], filters[1],
                                     columns[2], filters[2]);
        CompositeQueryParameter parameter = CompositeQueryParameter.from(filterMap, ALL);
        QueryParameters parameters = newBuilder().add(parameter)
                                                 .build();
        CompositeQueryParameter singleParameter = parameters.iterator()
                                                            .next();
        Multimap<Column, Filter> actualFilters = singleParameter.filters();
        for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];
            Collection<Filter> readFilters = actualFilters.get(column);
            assertThat(readFilters).hasSize(1);
            assertEquals(filters[i], readFilters.iterator()
                                                .next());
        }
    }

    @Test
    @DisplayName("keep multiple filters for single column")
    void keepManyFiltersForColumn() throws ParseException {
        String columnName = "time";
        Column column = column();

        // Some valid Timestamp values
        Timestamp startTime = Timestamps.parse("2000-01-01T10:00:00.000-05:00");
        Timestamp deadline = Timestamps.parse("2017-01-01T10:00:00.000-05:00");

        Filter startTimeFilter = gt(columnName, startTime);
        Filter deadlineFilter = le(columnName, deadline);
        Multimap<Column, Filter> filterMap =
                ImmutableMultimap.<Column, Filter>builder()
                        .put(column, startTimeFilter)
                        .put(column, deadlineFilter)
                        .build();
        CompositeQueryParameter parameter = CompositeQueryParameter.from(filterMap, ALL);
        QueryParameters parameters = newBuilder().add(parameter)
                                                 .build();
        List<CompositeQueryParameter> aggregatingParameters = newArrayList(parameters);
        assertThat(aggregatingParameters).hasSize(1);
        Multimap<Column, Filter> actualFilters =
                aggregatingParameters.get(0)
                                     .filters();
        Collection<Filter> timeFilters = actualFilters.get(column);

        IterableSubject assertTimeFilters = assertThat(timeFilters);
        assertTimeFilters.hasSize(2);
        assertTimeFilters.containsExactly(startTimeFilter, deadlineFilter);
    }

    @Test
    @DisplayName("create parameters with active lifecycle flags")
    void createActiveParams() {
        InMemoryStorageFactory factory = InMemoryStorageFactory.newInstance();
        ContextSpec spec = ContextSpec.multitenant("random name");
        RecordStorage<ProjectId> storage =
                factory.createRecordStorage(spec, ProjectProjection.class);

        QueryParameters parameters = QueryParameters.activeEntityQueryParams(storage);

        assertTrue(parameters.isLifecycleAttributesSet());

        Iterator<CompositeQueryParameter> paramsIterator = parameters.iterator();
        CompositeQueryParameter lifecycleParameter = paramsIterator.next();
        assertFalse(paramsIterator.hasNext());
        assertTrue(lifecycleParameter.hasLifecycle());
        assertEquals(ALL, lifecycleParameter.operator());
        ImmutableMultimap<Column, Filter> filters = lifecycleParameter.filters();

        Columns lifecycleColumns = storage.lifecycleColumns();
        ColumnName archivedName = ColumnName.of(archived);
        Column archivedColumn = lifecycleColumns.get(archivedName);
        ColumnName deletedName = ColumnName.of(deleted);
        Column deletedColumn = lifecycleColumns.get(deletedName);

        ImmutableCollection<Filter> archivedFilters = filters.get(archivedColumn);
        UnmodifiableIterator<Filter> archivedFilterIterator = archivedFilters.iterator();
        assertEquals(eq(archivedName.value(), false), archivedFilterIterator.next());
        assertFalse(archivedFilterIterator.hasNext());

        ImmutableCollection<Filter> deletedFilters = filters.get(deletedColumn);
        UnmodifiableIterator<Filter> deletedFilterIterator = deletedFilters.iterator();
        assertEquals(eq(deletedName.value(), false), deletedFilterIterator.next());
        assertFalse(deletedFilterIterator.hasNext());
    }

    private static CompositeQueryParameter aggregatingParameter(Column column,
                                                                Filter filter) {
        Multimap<Column, Filter> filters = ImmutableMultimap.of(column, filter);
        CompositeQueryParameter result = CompositeQueryParameter.from(filters, ALL);
        return result;
    }
}
