/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.client;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.test.client.TestEntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Durations.fromHours;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.currentTime;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.either;
import static io.spine.client.Filters.eq;
import static io.spine.client.Filters.ge;
import static io.spine.client.Filters.gt;
import static io.spine.client.Filters.le;
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.client.OrderBy.Direction.DESCENDING;
import static io.spine.client.OrderBy.Direction.OD_UNKNOWN;
import static io.spine.client.OrderBy.Direction.UNRECOGNIZED;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactory;
import static io.spine.client.given.QueryBuilderTestEnv.FIRST_FIELD;
import static io.spine.client.given.QueryBuilderTestEnv.SECOND_FIELD;
import static io.spine.client.given.QueryBuilderTestEnv.TEST_ENTITY_TYPE;
import static io.spine.client.given.QueryBuilderTestEnv.TEST_ENTITY_TYPE_URL;
import static io.spine.client.given.QueryBuilderTestEnv.orderBy;
import static io.spine.client.given.TestEntities.randomId;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`QueryBuilder` should")
class QueryBuilderTest {

    private QueryFactory factory;

    @BeforeEach
    void createFactory() {
        factory = requestFactory().query();
    }

    @Nested
    @DisplayName("check arguments and")
    class CheckArguments {

        @Test
        @DisplayName(NOT_ACCEPT_NULLS)
        void notAcceptNulls() {
            var tester = new NullPointerTester();
            tester.testAllPublicStaticMethods(QueryBuilder.class);
            tester.testAllPublicInstanceMethods(factory.select(TEST_ENTITY_TYPE));
        }

        @Test
        @DisplayName("throw IAE if negative limit int value is provided")
        void notAcceptNegativeIntLimit() {
            assertThrows(IllegalArgumentException.class,
                         () -> factory.select(TEST_ENTITY_TYPE)
                                      .limit(-10));
        }

        @Test
        @DisplayName("throw IAE if limit int value is 0")
        void notAcceptZeroIntLimit() {
            assertThrows(IllegalArgumentException.class,
                         () -> factory.select(TEST_ENTITY_TYPE)
                                      .limit(0));
        }

        @Test
        @DisplayName("throw IAE if order direction is not ASCENDING or DESCENDING")
        void notAcceptInvalidDirection() {
            var select = factory.select(TEST_ENTITY_TYPE)
                                .orderBy(FIRST_FIELD, ASCENDING)
                                .orderBy(FIRST_FIELD, DESCENDING);
            assertThrows(IllegalArgumentException.class,
                         () -> select.orderBy(FIRST_FIELD, OD_UNKNOWN));
            assertThrows(IllegalArgumentException.class,
                         () -> select.orderBy(FIRST_FIELD, UNRECOGNIZED));
        }
    }

    @Nested
    @DisplayName("create query")
    class CreateQuery {

        @Test
        @DisplayName("by only entity type")
        void byType() {
            var query = factory.select(TEST_ENTITY_TYPE)
                               .build();
            assertNotNull(query);
            var format = query.getFormat();
            assertFalse(format.hasFieldMask());

            var target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertThat(format.getOrderByCount()).isEqualTo(0);
            assertThat(format.getLimit()).isEqualTo(0);

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with order")
        void ordered() {
            var query = factory.select(TEST_ENTITY_TYPE)
                               .orderBy(FIRST_FIELD, ASCENDING)
                               .build();
            assertNotNull(query);
            var format = query.getFormat();
            assertFalse(format.hasFieldMask());

            var expectedOrderBy = orderBy(FIRST_FIELD, ASCENDING);
            assertEquals(expectedOrderBy, format.getOrderBy(0));

            assertThat(format.getLimit()).isEqualTo(0);

            var target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with limit")
        void limited() {
            var limit = 15;
            var query = factory.select(TEST_ENTITY_TYPE)
                               .orderBy(SECOND_FIELD, DESCENDING)
                               .limit(limit)
                               .build();
            assertNotNull(query);
            var format = query.getFormat();
            assertFalse(format.hasFieldMask());

            var expectedOrderBy = orderBy(SECOND_FIELD, DESCENDING);
            assertEquals(expectedOrderBy, format.getOrderBy(0));

            assertThat(format.getLimit()).isEqualTo(limit);

            var target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("by id")
        void byId() {
            var id1 = 314;
            var id2 = 271;
            var query = factory.select(TEST_ENTITY_TYPE)
                               .byId(id1, id2)
                               .build();
            assertNotNull(query);
            assertFalse(query.getFormat().hasFieldMask());

            var target = query.getTarget();
            assertFalse(target.getIncludeAll());

            var entityFilters = target.getFilters();
            var idFilter = entityFilters.getIdFilter();
            Collection<Any> idValues = idFilter.getIdList();
            Collection<Integer> intIdValues = idValues
                    .stream()
                    .map(id -> toObject(id, int.class))
                    .collect(toList());

            assertThat(idValues)
                 .hasSize(2);
            assertThat(intIdValues).containsExactly(id1, id2);
        }

        @Test
        @DisplayName("by field mask")
        void byFieldMask() {
            var fieldName = "TestEntity.firstField";
            var query = factory.select(TEST_ENTITY_TYPE)
                               .withMask(fieldName)
                               .build();
            assertNotNull(query);
            var format = query.getFormat();
            assertTrue(format.hasFieldMask());

            var mask = format.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();

            var assertFieldNames = assertThat(fieldNames);

            assertFieldNames.hasSize(1);
            assertFieldNames.contains(fieldName);
        }

        @Test
        @DisplayName("by column filter")
        void byFilter() {
            var columnName = "myImaginaryColumn";
            Object columnValue = 42;

            var query = factory.select(TEST_ENTITY_TYPE)
                               .where(eq(columnName, columnValue))
                               .build();
            assertNotNull(query);
            var target = query.getTarget();
            assertFalse(target.getIncludeAll());

            var entityFilters = target.getFilters();
            var aggregatingFilters = entityFilters.getFilterList();
            assertThat(aggregatingFilters)
                 .hasSize(1);
            var aggregatingFilter = aggregatingFilters.get(0);
            Collection<Filter> filters = aggregatingFilter.getFilterList();
            assertThat(filters)
                 .hasSize(1);
            var actualValue = findByName(filters, columnName).getValue();
            assertNotNull(columnValue);
            var messageValue = unpack(actualValue, Int32Value.class);
            var actualGenericValue = messageValue.getValue();
            assertEquals(columnValue, actualGenericValue);
        }

        @Test
        @DisplayName("by multiple column filters")
        void byMultipleFilters() {
            var columnName1 = "myColumn";
            Object columnValue1 = 42;
            var columnName2 = "oneMore";
            Object columnValue2 = randomId();

            var query = factory.select(TEST_ENTITY_TYPE)
                               .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                               .build();
            assertNotNull(query);
            var target = query.getTarget();
            assertFalse(target.getIncludeAll());

            var entityFilters = target.getFilters();
            var aggregatingFilters = entityFilters.getFilterList();
            assertThat(aggregatingFilters)
                 .hasSize(1);
            Collection<Filter> filters = aggregatingFilters.get(0)
                                                           .getFilterList();
            var actualValue1 = findByName(filters, columnName1).getValue();
            assertNotNull(actualValue1);
            int actualGenericValue1 = toObject(actualValue1, int.class);
            assertEquals(columnValue1, actualGenericValue1);

            var actualValue2 = findByName(filters, columnName2).getValue();
            assertNotNull(actualValue2);
            Message actualGenericValue2 = toObject(actualValue2, TestEntityId.class);
            assertEquals(columnValue2, actualGenericValue2);
        }

        @SuppressWarnings("OverlyLongMethod")
        // A big test for the grouping operators proper building.
        @Test
        @DisplayName("by column filter grouping")
        void byFilterGrouping() {
            var establishedTimeColumn = "establishedTime";
            var companySizeColumn = "companySize";
            var countryColumn = "country";
            var countryName = "Ukraine";

            var twoDaysAgo = subtract(currentTime(), fromHours(-48));

            var query = factory.select(TEST_ENTITY_TYPE)
                               .where(all(ge(companySizeColumn, 50),
                                            le(companySizeColumn, 1000)),
                                        either(gt(establishedTimeColumn, twoDaysAgo),
                                               eq(countryColumn, countryName)))
                               .build();
            var target = query.getTarget();
            var filters = target.getFilters()
                                .getFilterList();
            assertThat(filters)
                 .hasSize(2);

            var firstFilter = filters.get(0);
            var secondFilter = filters.get(1);

            List<Filter> allFilters;
            List<Filter> eitherFilters;
            if (firstFilter.getOperator() == ALL) {
                assertEquals(EITHER, secondFilter.getOperator());
                allFilters = firstFilter.getFilterList();
                eitherFilters = secondFilter.getFilterList();
            } else {
                assertEquals(ALL, secondFilter.getOperator());
                eitherFilters = firstFilter.getFilterList();
                allFilters = secondFilter.getFilterList();
            }

            assertThat(allFilters)
                 .hasSize(2);
            assertThat(eitherFilters)
                 .hasSize(2);

            var companySizeLowerBound = allFilters.get(0);
            var columnName1 = companySizeLowerBound.getFieldPath()
                                                   .getFieldName(0);
            assertEquals(companySizeColumn, columnName1);
            assertEquals(50L,
                         (long) toObject(companySizeLowerBound.getValue(), int.class));
            assertEquals(GREATER_OR_EQUAL, companySizeLowerBound.getOperator());

            var companySizeHigherBound = allFilters.get(1);
            var columnName2 = companySizeHigherBound.getFieldPath()
                                                    .getFieldName(0);
            assertEquals(companySizeColumn, columnName2);
            assertEquals(1000L,
                         (long) toObject(companySizeHigherBound.getValue(), int.class));
            assertEquals(LESS_OR_EQUAL, companySizeHigherBound.getOperator());

            var establishedTimeFilter = eitherFilters.get(0);
            var columnName3 = establishedTimeFilter.getFieldPath()
                                                   .getFieldName(0);
            assertEquals(establishedTimeColumn, columnName3);
            assertEquals(twoDaysAgo,
                         toObject(establishedTimeFilter.getValue(), Timestamp.class));
            assertEquals(GREATER_THAN, establishedTimeFilter.getOperator());

            var countryFilter = eitherFilters.get(1);
            var columnName4 = countryFilter.getFieldPath()
                                           .getFieldName(0);
            assertEquals(countryColumn, columnName4);
            assertEquals(countryName,
                         toObject(countryFilter.getValue(), String.class));
            assertEquals(EQUAL, countryFilter.getOperator());
        }

        @SuppressWarnings("OverlyLongMethod")
        // A big test case covering the query arguments coexistence.
        @Test
        @DisplayName("by all available arguments")
        void byAllArguments() {
            var id1 = 314;
            var id2 = 271;
            var limit = 10;
            var columnName1 = "column1";
            Object columnValue1 = 42;
            var columnName2 = "column2";
            Object columnValue2 = randomId();
            var fieldName = "TestEntity.secondField";
            var query = factory.select(TEST_ENTITY_TYPE)
                               .withMask(fieldName)
                               .byId(id1, id2)
                               .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                               .orderBy(SECOND_FIELD, DESCENDING)
                               .limit(limit)
                               .build();
            assertNotNull(query);

            var format = query.getFormat();
            var mask = format.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();
            var assertFieldNames = assertThat(fieldNames);
            assertFieldNames.hasSize(1);
            assertFieldNames.containsExactly(fieldName);

            var target = query.getTarget();
            assertFalse(target.getIncludeAll());
            var entityFilters = target.getFilters();

            var idFilter = entityFilters.getIdFilter();
            Collection<Any> idValues = idFilter.getIdList();
            Collection<Integer> intIdValues = idValues
                    .stream()
                    .map(id -> toObject(id, int.class))
                    .collect(toList());

            assertThat(idValues)
                 .hasSize(2);
            assertThat(intIdValues).containsAtLeast(id1, id2);

            // Check query params
            var aggregatingFilters =
                    entityFilters.getFilterList();

            assertThat(aggregatingFilters)
                 .hasSize(1);
            Collection<Filter> filters = aggregatingFilters.get(0)
                                                           .getFilterList();
            assertThat(filters)
                 .hasSize(2);

            var actualValue1 = findByName(filters, columnName1).getValue();
            assertNotNull(actualValue1);
            int actualGenericValue1 = toObject(actualValue1, int.class);
            assertEquals(columnValue1, actualGenericValue1);

            var actualValue2 = findByName(filters, columnName2).getValue();
            assertNotNull(actualValue2);
            Message actualGenericValue2 = toObject(actualValue2, TestEntityId.class);
            assertEquals(columnValue2, actualGenericValue2);

            var expectedOrderBy = orderBy(SECOND_FIELD, DESCENDING);
            assertEquals(expectedOrderBy, format.getOrderBy(0));

            assertThat(format.getLimit()).isEqualTo(limit);
        }

        private Filter findByName(Iterable<Filter> filters, String name) {
            for (var filter : filters) {
                if (filter.getFieldPath()
                          .getFieldName(0)
                          .equals(name)) {
                    return filter;
                }
            }
            fail(format("No Filter found for %s.", name));
            // avoid returning `null`
            throw new RuntimeException("never happens unless JUnit is broken");
        }
    }

    @Nested
    @DisplayName("persist only last given")
    class Persist {

        @Test
        @DisplayName("IDs clause")
        void lastIds() {
            Iterable<?> genericIds = asList(newUuid(), -1, randomId());
            Long[] longIds = {1L, 2L, 3L};
            Message[] messageIds = {randomId(), randomId(), randomId()};
            var stringIds = new String[]{newUuid(), newUuid(), newUuid()};
            Integer[] intIds = {4, 5, 6};

            var query = factory.select(TEST_ENTITY_TYPE)
                               .byId(genericIds)
                               .byId(longIds)
                               .byId(stringIds)
                               .byId(intIds)
                               .byId(messageIds)
                               .build();
            assertNotNull(query);

            var target = query.getTarget();
            var filters = target.getFilters();
            Collection<Any> entityIds = filters.getIdFilter()
                                               .getIdList();
            assertThat(entityIds)
                 .hasSize(messageIds.length);
            Iterable<? extends Message> actualValues = entityIds
                    .stream()
                    .map(id -> toObject(id, TestEntityId.class))
                    .collect(toList());
            assertThat(actualValues)
                    .containsAtLeastElementsIn(messageIds);
        }

        @Test
        @DisplayName("field mask")
        void lastFieldMask() {
            Iterable<String> iterableFields = singleton("TestEntity.firstField");
            var arrayFields = new String[]{"TestEntity.secondField"};

            var query = factory.select(TEST_ENTITY_TYPE)
                               .withMask(iterableFields)
                               .withMask(arrayFields)
                               .build();
            assertNotNull(query);
            var mask = query.getFormat().getFieldMask();

            Collection<String> maskFields = mask.getPathsList();
            assertThat(maskFields)
                 .hasSize(arrayFields.length);
            assertThat(maskFields)
                    .containsAtLeastElementsIn(arrayFields);
        }

        @Test
        @DisplayName("limit")
        void lastLimit() {
            var expectedLimit = 10;
            var query = factory.select(TEST_ENTITY_TYPE)
                               .orderBy(FIRST_FIELD, ASCENDING)
                               .limit(2)
                               .limit(5)
                               .limit(expectedLimit)
                               .build();
            assertNotNull(query);
            assertEquals(expectedLimit, query.getFormat().getLimit());
        }

        @Test
        @DisplayName("order")
        void lastOrder() {
            var query = factory.select(TEST_ENTITY_TYPE)
                               .orderBy(FIRST_FIELD, ASCENDING)
                               .orderBy(SECOND_FIELD, ASCENDING)
                               .orderBy(SECOND_FIELD, DESCENDING)
                               .orderBy(FIRST_FIELD, DESCENDING)
                               .build();
            assertNotNull(query);
            assertEquals(orderBy(FIRST_FIELD, DESCENDING), query.getFormat().getOrderBy(0));
        }
    }

    @Test
    @DisplayName("provide proper `toString()` method")
    void supportToString() {
        var id1 = 314;
        var id2 = 271;
        var columnName1 = "column1";
        Object columnValue1 = 42;
        var columnName2 = "column2";
        Message columnValue2 = randomId();
        var fieldName = "TestEntity.secondField";
        var builder =
                factory.select(TEST_ENTITY_TYPE)
                       .withMask(fieldName)
                       .byId(id1, id2)
                       .where(eq(columnName1, columnValue1),
                              eq(columnName2, columnValue2));
        var stringRepr = builder.toString();

        var assertOutput = assertThat(stringRepr);

        assertOutput.contains(TEST_ENTITY_TYPE.getSimpleName());
        assertOutput.contains(String.valueOf(id1));
        assertOutput.contains(String.valueOf(id2));
        assertOutput.contains(columnName1);
        assertOutput.contains(columnName2);
    }
}
