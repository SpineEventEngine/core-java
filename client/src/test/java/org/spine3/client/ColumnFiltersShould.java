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

package org.spine3.client;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.client.ColumnFilter.Operator;
import org.spine3.protobuf.AnyPacker;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.spine3.client.ColumnFilter.Operator.EQUAL;
import static org.spine3.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static org.spine3.client.ColumnFilter.Operator.GREATER_THAN;
import static org.spine3.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static org.spine3.client.ColumnFilter.Operator.LESS_THAN;
import static org.spine3.client.ColumnFilters.all;
import static org.spine3.client.ColumnFilters.either;
import static org.spine3.client.ColumnFilters.eq;
import static org.spine3.client.ColumnFilters.ge;
import static org.spine3.client.ColumnFilters.gt;
import static org.spine3.client.ColumnFilters.le;
import static org.spine3.client.ColumnFilters.lt;
import static org.spine3.client.GroupingColumnFilter.GroupingOperator;
import static org.spine3.client.GroupingColumnFilter.GroupingOperator.ALL;
import static org.spine3.client.GroupingColumnFilter.GroupingOperator.EITHER;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.test.Verify.assertContainsAll;
import static org.spine3.time.Time.getCurrentTime;

/**
 * @author Dmytro Dashenkov
 */
public class ColumnFiltersShould {

    private static final String COLUMN_NAME = "preciseColumn";
    private static final Timestamp COLUMN_VALUE = getCurrentTime();

    @Test
    public void have_private_util_ctor() {
        assertHasPrivateParameterlessCtor(ColumnFilters.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(Timestamp.class, Timestamp.getDefaultInstance())
                .setDefault(ColumnFilter.class, ColumnFilter.getDefaultInstance())
                .testAllPublicStaticMethods(ColumnFilters.class);
    }

    @Test
    public void create_EQUALS_instances() {
        checkCreatesInstance(eq(COLUMN_NAME, COLUMN_VALUE), EQUAL);
    }

    @Test
    public void create_GREATER_THAN_instances() {
        checkCreatesInstance(gt(COLUMN_NAME, COLUMN_VALUE), GREATER_THAN);
    }

    @Test
    public void create_GREATER_OR_EQUAL_instances() {
        checkCreatesInstance(ge(COLUMN_NAME, COLUMN_VALUE), GREATER_OR_EQUAL);
    }

    @Test
    public void create_LESS_THAN_instances() {
        checkCreatesInstance(lt(COLUMN_NAME, COLUMN_VALUE), LESS_THAN);
    }

    @Test
    public void create_LESS_OR_EQUAL_instances() {
        checkCreatesInstance(le(COLUMN_NAME, COLUMN_VALUE), LESS_OR_EQUAL);
    }

    @Test
    public void create_ALL_grouping_instances() {
        final ColumnFilter[] filters = {
                le(COLUMN_NAME, COLUMN_VALUE),
                ge(COLUMN_NAME, COLUMN_VALUE)
        };
        checkCreatesInstance(all(filters[0], filters[1]), ALL, filters);
    }

    @Test
    public void create_EITHER_grouping_instances() {
        final ColumnFilter[] filters = {
                lt(COLUMN_NAME, COLUMN_VALUE),
                gt(COLUMN_NAME, COLUMN_VALUE)
        };
        checkCreatesInstance(either(filters[0], filters[1]), EITHER, filters);
    }

    @Test
    public void create_ordering_filters_for_numbers() {
        final double number = 3.14;
        final ColumnFilter filter = le("doubleColumn", number);
        assertNotNull(filter);
        assertEquals(LESS_OR_EQUAL, filter.getOperator());
        final DoubleValue value = AnyPacker.unpack(filter.getValue());
        assertEquals(number, value.getValue(), 0.0);
    }

    @Test
    public void create_ordering_filters_for_strings() {
        final String string = "abc";
        final ColumnFilter filter = gt("stringColumn", string);
        assertNotNull(filter);
        assertEquals(GREATER_THAN, filter.getOperator());
        final StringValue value = AnyPacker.unpack(filter.getValue());
        assertEquals(string, value.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_ordering_filters_for_enums() {
        ge("enumColumn", EQUAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_ordering_filters_for_non_primitive_numbers() {
        final AtomicInteger number = new AtomicInteger(42);
        ge("atomicColumn", number);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_ordering_filters_for_not_supported_types() {
        final Comparable<?> value = Calendar.getInstance(); // Comparable but not supported
        le("invaliudColumn", value);
    }

    private static void checkCreatesInstance(ColumnFilter filter,
                                             Operator operator) {
        assertEquals(COLUMN_NAME, filter.getColumnName());
        assertEquals(pack(COLUMN_VALUE), filter.getValue());
        assertEquals(operator, filter.getOperator());
    }

    private static void checkCreatesInstance(GroupingColumnFilter filter,
                                             GroupingOperator operator,
                                             ColumnFilter[] groupedFilters) {
        assertEquals(operator, filter.getOperator());
        assertContainsAll(filter.getFilterList(), groupedFilters);
    }
}
