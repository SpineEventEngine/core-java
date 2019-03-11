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

package io.spine.client;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Truth;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
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
import static io.spine.client.given.QueryBuilderTestEnv.EMPTY_ORDER_BY;
import static io.spine.client.given.QueryBuilderTestEnv.EMPTY_PAGINATION;
import static io.spine.client.given.QueryBuilderTestEnv.FIRST_FIELD;
import static io.spine.client.given.QueryBuilderTestEnv.SECOND_FIELD;
import static io.spine.client.given.QueryBuilderTestEnv.TEST_ENTITY_TYPE;
import static io.spine.client.given.QueryBuilderTestEnv.TEST_ENTITY_TYPE_URL;
import static io.spine.client.given.QueryBuilderTestEnv.orderBy;
import static io.spine.client.given.QueryBuilderTestEnv.pagination;
import static io.spine.client.given.TestEntities.randomId;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Durations2.fromHours;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("QueryBuilder should")
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
            NullPointerTester tester = new NullPointerTester();
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
            QueryBuilder select = factory.select(TEST_ENTITY_TYPE)
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
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            Target target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(EMPTY_ORDER_BY, query.getOrderBy());
            assertEquals(EMPTY_PAGINATION, query.getPagination());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with order")
        void ordered() {
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderBy(FIRST_FIELD, ASCENDING)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            OrderBy expectedOrderBy = orderBy(FIRST_FIELD, ASCENDING);
            assertEquals(expectedOrderBy, query.getOrderBy());

            assertEquals(EMPTY_PAGINATION, query.getPagination());

            Target target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with limit")
        void limited() {
            int limit = 15;
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderBy(SECOND_FIELD, DESCENDING)
                                 .limit(limit)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            OrderBy expectedOrderBy = orderBy(SECOND_FIELD, DESCENDING);
            assertEquals(expectedOrderBy, query.getOrderBy());

            Pagination expectedPagination = pagination(limit);
            assertEquals(expectedPagination, query.getPagination());

            Target target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("by id")
        void byId() {
            int id1 = 314;
            int id2 = 271;
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .byId(id1, id2)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());

            TargetFilters entityFilters = target.getFilters();
            IdFilter idFilter = entityFilters.getIdFilter();
            Collection<Any> idValues = idFilter.getIdsList();
            Collection<Integer> intIdValues = idValues
                    .stream()
                    .map(id -> toObject(id, int.class))
                    .collect(toList());

            Truth.assertThat(idValues)
                 .hasSize(2);
            assertThat(intIdValues, containsInAnyOrder(id1, id2));
        }

        @Test
        @DisplayName("by field mask")
        void byFieldMask() {
            String fieldName = "TestEntity.firstField";
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(fieldName)
                                 .build();
            assertNotNull(query);
            assertTrue(query.hasFieldMask());

            FieldMask mask = query.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();

            IterableSubject assertFieldNames = Truth.assertThat(fieldNames);

            assertFieldNames.hasSize(1);
            assertFieldNames.contains(fieldName);
        }

        @Test
        @DisplayName("by column filter")
        void byFilter() {
            String columnName = "myImaginaryColumn";
            Object columnValue = 42;

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .where(eq(columnName, columnValue))
                                 .build();
            assertNotNull(query);
            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());

            TargetFilters entityFilters = target.getFilters();
            List<CompositeFilter> aggregatingFilters = entityFilters.getFilterList();
            Truth.assertThat(aggregatingFilters)
                 .hasSize(1);
            CompositeFilter aggregatingFilter = aggregatingFilters.get(0);
            Collection<Filter> filters = aggregatingFilter.getFilterList();
            Truth.assertThat(filters)
                 .hasSize(1);
            Any actualValue = findByName(filters, columnName).getValue();
            assertNotNull(columnValue);
            Int32Value messageValue = unpack(actualValue, Int32Value.class);
            int actualGenericValue = messageValue.getValue();
            assertEquals(columnValue, actualGenericValue);
        }

        @Test
        @DisplayName("by multiple column filters")
        void byMultipleFilters() {
            String columnName1 = "myColumn";
            Object columnValue1 = 42;
            String columnName2 = "oneMore";
            Object columnValue2 = randomId();

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                                 .build();
            assertNotNull(query);
            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());

            TargetFilters entityFilters = target.getFilters();
            List<CompositeFilter> aggregatingFilters = entityFilters.getFilterList();
            Truth.assertThat(aggregatingFilters)
                 .hasSize(1);
            Collection<Filter> filters = aggregatingFilters.get(0)
                                                           .getFilterList();
            Any actualValue1 = findByName(filters, columnName1).getValue();
            assertNotNull(actualValue1);
            int actualGenericValue1 = toObject(actualValue1, int.class);
            assertEquals(columnValue1, actualGenericValue1);

            Any actualValue2 = findByName(filters, columnName2).getValue();
            assertNotNull(actualValue2);
            Message actualGenericValue2 = toObject(actualValue2, TestEntityId.class);
            assertEquals(columnValue2, actualGenericValue2);
        }

        @SuppressWarnings("OverlyLongMethod")
        // A big test for the grouping operators proper building.
        @Test
        @DisplayName("by column filter grouping")
        void byFilterGrouping() {
            String establishedTimeColumn = "establishedTime";
            String companySizeColumn = "companySize";
            String countryColumn = "country";
            String countryName = "Ukraine";

            Timestamp twoDaysAgo = subtract(currentTime(), fromHours(-48));

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .where(all(ge(companySizeColumn, 50),
                                            le(companySizeColumn, 1000)),
                                        either(gt(establishedTimeColumn, twoDaysAgo),
                                               eq(countryColumn, countryName)))
                                 .build();
            Target target = query.getTarget();
            List<CompositeFilter> filters = target.getFilters()
                                                  .getFilterList();
            Truth.assertThat(filters)
                 .hasSize(2);

            CompositeFilter firstFilter = filters.get(0);
            CompositeFilter secondFilter = filters.get(1);

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

            Truth.assertThat(allFilters)
                 .hasSize(2);
            Truth.assertThat(eitherFilters)
                 .hasSize(2);

            Filter companySizeLowerBound = allFilters.get(0);
            String columnName1 = companySizeLowerBound.getFieldPath()
                                                      .getFieldName(0);
            assertEquals(companySizeColumn, columnName1);
            assertEquals(50L,
                         (long) toObject(companySizeLowerBound.getValue(), int.class));
            assertEquals(GREATER_OR_EQUAL, companySizeLowerBound.getOperator());

            Filter companySizeHigherBound = allFilters.get(1);
            String columnName2 = companySizeHigherBound.getFieldPath()
                                                       .getFieldName(0);
            assertEquals(companySizeColumn, columnName2);
            assertEquals(1000L,
                         (long) toObject(companySizeHigherBound.getValue(), int.class));
            assertEquals(LESS_OR_EQUAL, companySizeHigherBound.getOperator());

            Filter establishedTimeFilter = eitherFilters.get(0);
            String columnName3 = establishedTimeFilter.getFieldPath()
                                                      .getFieldName(0);
            assertEquals(establishedTimeColumn, columnName3);
            assertEquals(twoDaysAgo,
                         toObject(establishedTimeFilter.getValue(), Timestamp.class));
            assertEquals(GREATER_THAN, establishedTimeFilter.getOperator());

            Filter countryFilter = eitherFilters.get(1);
            String columnName4 = countryFilter.getFieldPath()
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
            int id1 = 314;
            int id2 = 271;
            int limit = 10;
            String columnName1 = "column1";
            Object columnValue1 = 42;
            String columnName2 = "column2";
            Object columnValue2 = randomId();
            String fieldName = "TestEntity.secondField";
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(fieldName)
                                 .byId(id1, id2)
                                 .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                                 .orderBy(SECOND_FIELD, DESCENDING)
                                 .limit(limit)
                                 .build();
            assertNotNull(query);

            // Check FieldMask
            FieldMask mask = query.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();

            IterableSubject assertFieldNames = Truth.assertThat(fieldNames);

            assertFieldNames.hasSize(1);
            assertFieldNames.containsExactly(fieldName);

            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());
            TargetFilters entityFilters = target.getFilters();

            // Check IDs
            IdFilter idFilter = entityFilters.getIdFilter();
            Collection<Any> idValues = idFilter.getIdsList();
            Collection<Integer> intIdValues = idValues
                    .stream()
                    .map(id -> toObject(id, int.class))
                    .collect(toList());

            Truth.assertThat(idValues)
                 .hasSize(2);
            assertThat(intIdValues, containsInAnyOrder(id1, id2));

            // Check query params
            List<CompositeFilter> aggregatingFilters =
                    entityFilters.getFilterList();

            Truth.assertThat(aggregatingFilters)
                 .hasSize(1);
            Collection<Filter> filters = aggregatingFilters.get(0)
                                                           .getFilterList();
            Truth.assertThat(filters)
                 .hasSize(2);

            Any actualValue1 = findByName(filters, columnName1).getValue();
            assertNotNull(actualValue1);
            int actualGenericValue1 = toObject(actualValue1, int.class);
            assertEquals(columnValue1, actualGenericValue1);

            Any actualValue2 = findByName(filters, columnName2).getValue();
            assertNotNull(actualValue2);
            Message actualGenericValue2 = toObject(actualValue2, TestEntityId.class);
            assertEquals(columnValue2, actualGenericValue2);

            OrderBy expectedOrderBy = orderBy(SECOND_FIELD, DESCENDING);
            assertEquals(expectedOrderBy, query.getOrderBy());

            Pagination expectedPagination = pagination(limit);
            assertEquals(expectedPagination, query.getPagination());
        }

        private Filter findByName(Iterable<Filter> filters, String name) {
            for (Filter filter : filters) {
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
            String[] stringIds = {newUuid(), newUuid(), newUuid()};
            Integer[] intIds = {4, 5, 6};

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .byId(genericIds)
                                 .byId(longIds)
                                 .byId(stringIds)
                                 .byId(intIds)
                                 .byId(messageIds)
                                 .build();
            assertNotNull(query);

            Target target = query.getTarget();
            TargetFilters filters = target.getFilters();
            Collection<Any> entityIds = filters.getIdFilter()
                                               .getIdsList();
            Truth.assertThat(entityIds)
                 .hasSize(messageIds.length);
            Iterable<? extends Message> actualValues = entityIds
                    .stream()
                    .map(id -> toObject(id, TestEntityId.class))
                    .collect(toList());
            assertThat(actualValues, containsInAnyOrder(messageIds));
        }

        @Test
        @DisplayName("field mask")
        void lastFieldMask() {
            Iterable<String> iterableFields = singleton("TestEntity.firstField");
            String[] arrayFields = {"TestEntity.secondField"};

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(iterableFields)
                                 .withMask(arrayFields)
                                 .build();
            assertNotNull(query);
            FieldMask mask = query.getFieldMask();

            Collection<String> maskFields = mask.getPathsList();
            Truth.assertThat(maskFields)
                 .hasSize(arrayFields.length);
            assertThat(maskFields, contains(arrayFields));
        }

        @Test
        @DisplayName("limit")
        void lastLimit() {
            int expectedLimit = 10;
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderBy(FIRST_FIELD, ASCENDING)
                                 .limit(2)
                                 .limit(5)
                                 .limit(expectedLimit)
                                 .build();
            assertNotNull(query);
            assertEquals(expectedLimit, query.getPagination()
                                             .getPageSize());
        }

        @Test
        @DisplayName("order")
        void lastOrder() {
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderBy(FIRST_FIELD, ASCENDING)
                                 .orderBy(SECOND_FIELD, ASCENDING)
                                 .orderBy(SECOND_FIELD, DESCENDING)
                                 .orderBy(FIRST_FIELD, DESCENDING)
                                 .build();
            assertNotNull(query);
            assertEquals(orderBy(FIRST_FIELD, DESCENDING), query.getOrderBy());
        }
    }

    @Test
    @DisplayName("provide proper `toString()` method")
    void supportToString() {
        int id1 = 314;
        int id2 = 271;
        String columnName1 = "column1";
        Object columnValue1 = 42;
        String columnName2 = "column2";
        Message columnValue2 = randomId();
        String fieldName = "TestEntity.secondField";
        QueryBuilder builder = factory.select(TEST_ENTITY_TYPE)
                                      .withMask(fieldName)
                                      .byId(id1, id2)
                                      .where(eq(columnName1, columnValue1),
                                             eq(columnName2, columnValue2));
        String stringRepr = builder.toString();

        assertThat(stringRepr, containsString(TEST_ENTITY_TYPE.getSimpleName()));
        assertThat(stringRepr, containsString(valueOf(id1)));
        assertThat(stringRepr, containsString(valueOf(id2)));
        assertThat(stringRepr, containsString(columnName1));
        assertThat(stringRepr, containsString(columnName2));
    }
}
