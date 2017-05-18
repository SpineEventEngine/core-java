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

package org.spine3.server.entity.storage;

import com.google.common.collect.Iterators;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.spine3.client.ColumnFilter;
import org.spine3.client.ColumnFilters;
import org.spine3.server.entity.VersionableEntity;

import java.util.Collection;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.spine3.client.ColumnFilters.eq;
import static org.spine3.client.ColumnFilters.gt;
import static org.spine3.server.entity.storage.QueryParameters.newBuilder;
import static org.spine3.server.storage.EntityField.version;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.time.Time.getCurrentTime;

/**
 * @author Dmytro Dashenkov
 */
public class QueryParametersShould {

    @Test
    public void be_serializable() {
        final String columnName = version.name();
        final Column column = Columns.findColumn(VersionableEntity.class, columnName);
        final ColumnFilter filter = ColumnFilters.eq(columnName, 1);
        final QueryParameters parameters = QueryParameters.newBuilder()
                                                          .put(column, filter)
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
        final Iterable<ColumnFilter> parameters = newBuilder().put(mockColumn(), filters[0])
                                                              .put(mockColumn(), filters[1])
                                                              .put(mockColumn(), filters[2])
                                                              .build();
        final ColumnFilter[] results = Iterators.toArray(parameters.iterator(), ColumnFilter.class);
        assertArrayEquals(filters, results);
    }

    @Test
    public void retrieve_filter_by_column() {
        final ColumnFilter[] filters = {
                eq("$1nd", 42.0),
                eq("$2st", "entityColumnValue"),
                gt("$3d", getCurrentTime())};
        final Column[] columns = {mockColumn(), mockColumn(), mockColumn()};
        final QueryParameters parameters = newBuilder().put(columns[0], filters[0])
                                                       .put(columns[1], filters[1])
                                                       .put(columns[2], filters[2])
                                                       .build();
        for (int i = 0; i < columns.length; i++) {
            final Column column = columns[i];
            final Collection<ColumnFilter> readFilters = parameters.get(column);
            assertFalse(readFilters.isEmpty());
            assertSize(1, readFilters);
            assertEquals(filters[i], readFilters.iterator().next());
        }
    }

    @Test
    public void retrieve_absent_value_for_unknown_columns() {
        final QueryParameters parameters = newBuilder().build();
        final Column unknownColumn = mockColumn();
        final Collection<ColumnFilter> filter = parameters.get(unknownColumn);
        assertTrue(filter.isEmpty());
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
        final QueryParameters paramsB1 = newBuilder().put(bColumn, bFilter)
                                                     .build();
        final QueryParameters paramsB2 = newBuilder().put(bColumn, bFilter)
                                                     .build();
        final QueryParameters paramsB3 = newBuilder().put(bColumn, bFilter)
                                                     .build();

        // --- Group C ---
        // Consists of an instance with a single filter targeting an integer number Column
        final Column cColumn = mockColumn();
        final ColumnFilter cFilter = ColumnFilters.eq("a", 42);
        final QueryParameters paramsC = newBuilder().put(cColumn, cFilter)
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
}
