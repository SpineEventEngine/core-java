/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.testing.EqualsTester;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.client.ColumnFilter;
import io.spine.client.ColumnFilters;
import io.spine.server.entity.VersionableEntity;
import org.junit.Test;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.ImmutableMultimap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.le;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.server.entity.storage.CompositeQueryParameter.from;
import static io.spine.server.entity.storage.QueryParameters.newBuilder;
import static io.spine.server.storage.EntityField.version;
import static io.spine.test.Verify.assertContainsAll;
import static io.spine.test.Verify.assertSize;
import static io.spine.time.Time.getCurrentTime;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Dmytro Dashenkov
 */
public class QueryParametersShould {

    @Test
    public void be_serializable() {
        final String columnName = version.name();
        final Column column = Columns.findColumn(VersionableEntity.class, columnName);
        final ColumnFilter filter = ColumnFilters.eq(columnName, 1);
        final CompositeQueryParameter parameter = aggregatingParameter(column, filter);
        final QueryParameters parameters = QueryParameters.newBuilder()
                                                          .add(parameter)
                                                          .build();
        reserializeAndAssert(parameters);
    }

    @Test
    public void construct_from_empty_builder() {
        final QueryParameters parameters = newBuilder().build();
        assertNotNull(parameters);
    }

    @Test
    public void produce_iterator_over_filters() {
        final ColumnFilter[] filters = {
                eq("firstFilter", 1),
                eq("secondFilter", 42),
                gt("thirdFilter", getCurrentTime())};
        final Multimap<Column, ColumnFilter> columnFilters = of(mockColumn(), filters[0],
                                                                mockColumn(), filters[1],
                                                                mockColumn(), filters[2]);
        final CompositeQueryParameter parameter = from(columnFilters, ALL);
        final QueryParameters parameters = newBuilder().add(parameter)
                                                       .build();
        final Collection<ColumnFilter> results = newLinkedList();
        for (CompositeQueryParameter queryParameter : parameters) {
            results.addAll(queryParameter.getFilters().values());
        }
        assertArrayEquals(filters, results.toArray());
    }

    @Test
    public void retrieve_filter_by_column() {
        final ColumnFilter[] filters = {
                eq("$1nd", 42.0),
                eq("$2st", "entityColumnValue"),
                gt("$3d", getCurrentTime())};
        final Column[] columns = {mockColumn(), mockColumn(), mockColumn()};
        final Multimap<Column, ColumnFilter> columnFilters = of(columns[0], filters[0],
                                                                columns[1], filters[1],
                                                                columns[2], filters[2]);
        final CompositeQueryParameter parameter = from(columnFilters, ALL);
        final QueryParameters parameters = newBuilder().add(parameter)
                                                       .build();
        assertSize(1, newArrayList(parameters));
        final CompositeQueryParameter singleParameter = parameters.iterator().next();
        final Multimap<Column, ColumnFilter> actualFilters = singleParameter.getFilters();
        for (int i = 0; i < columns.length; i++) {
            final Column column = columns[i];
            final Collection<ColumnFilter> readFilters = actualFilters.get(column);
            assertFalse(readFilters.isEmpty());
            assertSize(1, readFilters);
            assertEquals(filters[i], readFilters.iterator().next());
        }
    }

    @Test
    public void keep_multiple_filters_for_single_column() throws ParseException {
        final String columnName = "time";
        final Column column = mock(Column.class);

        // Some valid Timestamp values
        final Timestamp startTime = Timestamps.parse("2000-01-01T10:00:00.000-05:00");
        final Timestamp deadline = Timestamps.parse("2017-01-01T10:00:00.000-05:00");

        final ColumnFilter startTimeFilter = gt(columnName, startTime);
        final ColumnFilter deadlineFilter = le(columnName, deadline);
        final Multimap<Column, ColumnFilter> columnFilters =
                ImmutableMultimap.<Column, ColumnFilter>builder()
                                 .put(column, startTimeFilter)
                                 .put(column, deadlineFilter)
                                 .build();
        final CompositeQueryParameter parameter = from(columnFilters, ALL);
        final QueryParameters parameters = newBuilder().add(parameter)
                                                       .build();
        final List<CompositeQueryParameter> aggregatingParameters = newArrayList(parameters);
        assertSize(1, aggregatingParameters);
        final Multimap<Column, ColumnFilter> actualColumnFilters = aggregatingParameters.get(0).getFilters();
        final Collection<ColumnFilter> timeFilters = actualColumnFilters.get(column);
        assertSize(2, timeFilters);
        assertContainsAll(timeFilters, startTimeFilter, deadlineFilter);
    }

    @Test
    public void support_equality() {
        // --- Group A ---
        // Consists of 2 empty instances
        final QueryParameters paramsA1 = newBuilder().build();
        final QueryParameters paramsA2 = newBuilder().build();

        // --- Group B ---
        // Consists of 3 instances with a single filter targeting a String Column
        final Column bColumn = mockColumn();
        final ColumnFilter bFilter = ColumnFilters.eq("b", "c");
        final QueryParameters paramsB1 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                                     .build();
        final QueryParameters paramsB2 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                                     .build();
        final QueryParameters paramsB3 = newBuilder().add(aggregatingParameter(bColumn, bFilter))
                                                     .build();

        // --- Group C ---
        // Consists of an instance with a single filter targeting an integer number Column
        final Column cColumn = mockColumn();
        final ColumnFilter cFilter = ColumnFilters.eq("a", 42);
        final QueryParameters paramsC = newBuilder().add(aggregatingParameter(cColumn, cFilter))
                                                    .build();

        // --- Check ---
        new EqualsTester().addEqualityGroup(paramsA1, paramsA2)
                          .addEqualityGroup(paramsB1, paramsB2, paramsB3)
                          .addEqualityGroup(paramsC)
                          .testEquals();
    }

    private static Column mockColumn() {
        final Column column = mock(Column.class);
        return column;
    }

    private static CompositeQueryParameter aggregatingParameter(Column column,
                                                                  ColumnFilter columnFilter) {
        final Multimap<Column, ColumnFilter> filter = of(column, columnFilter);
        final CompositeQueryParameter result = from(filter, ALL);
        return result;
    }
}
