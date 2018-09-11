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

package io.spine.client;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.protobuf.AnyPacker;
import io.spine.test.client.TestEntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.client.ColumnFilter.Operator.EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.ColumnFilter.Operator.GREATER_THAN;
import static io.spine.client.ColumnFilter.Operator.LESS_OR_EQUAL;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.either;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.ge;
import static io.spine.client.ColumnFilters.gt;
import static io.spine.client.ColumnFilters.le;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.EITHER;
import static io.spine.client.Order.Direction.ASCENDING;
import static io.spine.client.Order.Direction.DESCENDING;
import static io.spine.client.Order.Direction.OD_UNKNOWN;
import static io.spine.client.Order.Direction.UNRECOGNIZED;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactory;
import static io.spine.client.given.EntityIdUnpacker.unpacker;
import static io.spine.client.given.QueryBuilderTestEnv.EMPTY_ORDER;
import static io.spine.client.given.QueryBuilderTestEnv.EMPTY_PAGINATION;
import static io.spine.client.given.QueryBuilderTestEnv.FIRST_FIELD;
import static io.spine.client.given.QueryBuilderTestEnv.SECOND_FIELD;
import static io.spine.client.given.QueryBuilderTestEnv.TEST_ENTITY_TYPE;
import static io.spine.client.given.QueryBuilderTestEnv.TEST_ENTITY_TYPE_URL;
import static io.spine.client.given.QueryBuilderTestEnv.newMessageId;
import static io.spine.client.given.QueryBuilderTestEnv.order;
import static io.spine.client.given.QueryBuilderTestEnv.pagination;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Verify.assertContains;
import static io.spine.testing.Verify.assertSize;
import static io.spine.testing.Verify.fail;
import static io.spine.time.Durations2.fromHours;
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

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("Query builder should")
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
        void notAcceptNulls() throws NoSuchMethodException {
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
        @DisplayName("throw IAE if negative limit long value is provided")
        void notAcceptNegativeLongLimit() {
            assertThrows(IllegalArgumentException.class,
                         () -> factory.select(TEST_ENTITY_TYPE)
                                      .limit(-99999999L));
        }

        @Test
        @DisplayName("throw IAE if limit int value is 0")
        void notAcceptZeroIntLimit() {
            assertThrows(IllegalArgumentException.class,
                         () -> factory.select(TEST_ENTITY_TYPE)
                                      .limit(0));
        }

        @Test
        @DisplayName("throw IAE if limit long value is 0")
        void notAcceptZeroLongLimit() {
            assertThrows(IllegalArgumentException.class,
                         () -> factory.select(TEST_ENTITY_TYPE)
                                      .limit(0L));
        }

        @Test
        @DisplayName("throw IAE if order direction is not ASCENDING or DESCENDING")
        void notAcceptInvalidDirection() {
            QueryBuilder select = factory.select(TEST_ENTITY_TYPE)
                                         .orderedBy(FIRST_FIELD, ASCENDING)
                                         .orderedBy(FIRST_FIELD, DESCENDING);
            assertThrows(IllegalArgumentException.class,
                         () -> select.orderedBy(FIRST_FIELD, OD_UNKNOWN));
            assertThrows(IllegalArgumentException.class,
                         () -> select.orderedBy(FIRST_FIELD, UNRECOGNIZED));
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

            assertEquals(EMPTY_ORDER, query.getOrder());
            assertEquals(EMPTY_PAGINATION, query.getPagination());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with order")
        void ordered() {
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderedBy(FIRST_FIELD, ASCENDING)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            Order expectedOrder = order(FIRST_FIELD, ASCENDING);
            assertEquals(expectedOrder, query.getOrder());

            assertEquals(EMPTY_PAGINATION, query.getPagination());

            Target target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with Integer limit")
        void limitedWithInt() {
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderedBy(FIRST_FIELD, ASCENDING)
                                 .limit(10)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            Order expectedOrder = order(FIRST_FIELD, ASCENDING);
            assertEquals(expectedOrder, query.getOrder());

            Pagination expectedPagination = pagination(10L);
            assertEquals(expectedPagination, query.getPagination());

            Target target = query.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("with limit")
        void limited() {
            long limit = 15L;
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderedBy(SECOND_FIELD, DESCENDING)
                                 .limit(limit)
                                 .build();
            assertNotNull(query);
            assertFalse(query.hasFieldMask());

            Order expectedOrder = order(SECOND_FIELD, DESCENDING);
            assertEquals(expectedOrder, query.getOrder());

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

            EntityFilters entityFilters = target.getFilters();
            EntityIdFilter idFilter = entityFilters.getIdFilter();
            Collection<EntityId> idValues = idFilter.getIdsList();
            Function<EntityId, Integer> transformer = unpacker(int.class);
            Collection<Integer> intIdValues = idValues.stream()
                                                      .map(transformer)
                                                      .collect(toList());

            assertSize(2, idValues);
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
            assertSize(1, fieldNames);
            assertContains(fieldName, fieldNames);
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

            EntityFilters entityFilters = target.getFilters();
            List<CompositeColumnFilter> aggregatingColumnFilters =
                    entityFilters.getFilterList();
            assertSize(1, aggregatingColumnFilters);
            CompositeColumnFilter aggregatingColumnFilter = aggregatingColumnFilters.get(0);
            Collection<ColumnFilter> columnFilters = aggregatingColumnFilter.getFilterList();
            assertSize(1, columnFilters);
            Any actualValue = findByName(columnFilters, columnName).getValue();
            assertNotNull(columnValue);
            Int32Value messageValue = AnyPacker.unpack(actualValue);
            int actualGenericValue = messageValue.getValue();
            assertEquals(columnValue, actualGenericValue);
        }

        @Test
        @DisplayName("by multiple column filters")
        void byMultipleFilters() {
            String columnName1 = "myColumn";
            Object columnValue1 = 42;
            String columnName2 = "oneMore";
            Object columnValue2 = newMessageId();

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                                 .build();
            assertNotNull(query);
            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());

            EntityFilters entityFilters = target.getFilters();
            List<CompositeColumnFilter> aggregatingColumnFilters =
                    entityFilters.getFilterList();
            assertSize(1, aggregatingColumnFilters);
            Collection<ColumnFilter> columnFilters = aggregatingColumnFilters.get(0)
                                                                             .getFilterList();
            Any actualValue1 = findByName(columnFilters, columnName1).getValue();
            assertNotNull(actualValue1);
            int actualGenericValue1 = toObject(actualValue1, int.class);
            assertEquals(columnValue1, actualGenericValue1);

            Any actualValue2 = findByName(columnFilters, columnName2).getValue();
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

            Timestamp twoDaysAgo = subtract(getCurrentTime(), fromHours(-48));

            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .where(all(ge(companySizeColumn, 50),
                                            le(companySizeColumn, 1000)),
                                        either(gt(establishedTimeColumn, twoDaysAgo),
                                               eq(countryColumn, countryName)))
                                 .build();
            Target target = query.getTarget();
            List<CompositeColumnFilter> filters = target.getFilters()
                                                        .getFilterList();
            assertSize(2, filters);

            CompositeColumnFilter firstFilter = filters.get(0);
            CompositeColumnFilter secondFilter = filters.get(1);

            List<ColumnFilter> allColumnFilters;
            List<ColumnFilter> eitherColumnFilters;
            if (firstFilter.getOperator() == ALL) {
                assertEquals(EITHER, secondFilter.getOperator());
                allColumnFilters = firstFilter.getFilterList();
                eitherColumnFilters = secondFilter.getFilterList();
            } else {
                assertEquals(ALL, secondFilter.getOperator());
                eitherColumnFilters = firstFilter.getFilterList();
                allColumnFilters = secondFilter.getFilterList();
            }
            assertSize(2, allColumnFilters);
            assertSize(2, eitherColumnFilters);

            ColumnFilter companySizeLowerBound = allColumnFilters.get(0);
            assertEquals(companySizeColumn, companySizeLowerBound.getColumnName());
            assertEquals(50L,
                         (long) toObject(companySizeLowerBound.getValue(), int.class));
            assertEquals(GREATER_OR_EQUAL, companySizeLowerBound.getOperator());

            ColumnFilter companySizeHigherBound = allColumnFilters.get(1);
            assertEquals(companySizeColumn, companySizeHigherBound.getColumnName());
            assertEquals(1000L,
                         (long) toObject(companySizeHigherBound.getValue(), int.class));
            assertEquals(LESS_OR_EQUAL, companySizeHigherBound.getOperator());

            ColumnFilter establishedTimeFilter = eitherColumnFilters.get(0);
            assertEquals(establishedTimeColumn, establishedTimeFilter.getColumnName());
            assertEquals(twoDaysAgo,
                         toObject(establishedTimeFilter.getValue(), Timestamp.class));
            assertEquals(GREATER_THAN, establishedTimeFilter.getOperator());

            ColumnFilter countryFilter = eitherColumnFilters.get(1);
            assertEquals(countryColumn, countryFilter.getColumnName());
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
            Object columnValue2 = newMessageId();
            String fieldName = "TestEntity.secondField";
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(fieldName)
                                 .byId(id1, id2)
                                 .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                                 .orderedBy(SECOND_FIELD, DESCENDING)
                                 .limit(limit)
                                 .build();
            assertNotNull(query);

            // Check FieldMask
            FieldMask mask = query.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();
            assertSize(1, fieldNames);
            assertContains(fieldName, fieldNames);

            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());
            EntityFilters entityFilters = target.getFilters();

            // Check IDs
            EntityIdFilter idFilter = entityFilters.getIdFilter();
            Collection<EntityId> idValues = idFilter.getIdsList();
            Function<EntityId, Integer> transformer = unpacker(int.class);
            Collection<Integer> intIdValues = idValues.stream()
                                                      .map(transformer)
                                                      .collect(toList());
            assertSize(2, idValues);
            assertThat(intIdValues, containsInAnyOrder(id1, id2));

            // Check query params
            List<CompositeColumnFilter> aggregatingColumnFilters =
                    entityFilters.getFilterList();
            assertSize(1, aggregatingColumnFilters);
            Collection<ColumnFilter> columnFilters = aggregatingColumnFilters.get(0)
                                                                             .getFilterList();
            assertSize(2, columnFilters);

            Any actualValue1 = findByName(columnFilters, columnName1).getValue();
            assertNotNull(actualValue1);
            int actualGenericValue1 = toObject(actualValue1, int.class);
            assertEquals(columnValue1, actualGenericValue1);

            Any actualValue2 = findByName(columnFilters, columnName2).getValue();
            assertNotNull(actualValue2);
            Message actualGenericValue2 = toObject(actualValue2, TestEntityId.class);
            assertEquals(columnValue2, actualGenericValue2);

            Order expectedOrder = order(SECOND_FIELD, DESCENDING);
            assertEquals(expectedOrder, query.getOrder());

            Pagination expectedPagination = pagination(limit);
            assertEquals(expectedPagination, query.getPagination());
        }

        private ColumnFilter findByName(Iterable<ColumnFilter> filters, String name) {
            for (ColumnFilter filter : filters) {
                if (filter.getColumnName()
                          .equals(name)) {
                    return filter;
                }
            }
            fail(format("No ColumnFilter found for %s.", name));
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
            Iterable<?> genericIds = asList(newUuid(), -1, newMessageId());
            Long[] longIds = {1L, 2L, 3L};
            Message[] messageIds = {newMessageId(), newMessageId(), newMessageId()};
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
            EntityFilters filters = target.getFilters();
            Collection<EntityId> entityIds = filters.getIdFilter()
                                                    .getIdsList();
            assertSize(messageIds.length, entityIds);
            Function<EntityId, TestEntityId> transformer = unpacker(TestEntityId.class);
            Iterable<? extends Message> actualValues = entityIds.stream()
                                                                .map(transformer)
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
            assertSize(arrayFields.length, maskFields);
            assertThat(maskFields, contains(arrayFields));
        }

        @Test
        @DisplayName("limit")
        void lastLimit() {
            int expectedLimit = 10;
            Query query = factory.select(TEST_ENTITY_TYPE)
                                 .orderedBy(FIRST_FIELD, ASCENDING)
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
                                 .orderedBy(FIRST_FIELD, ASCENDING)
                                 .orderedBy(SECOND_FIELD, ASCENDING)
                                 .orderedBy(SECOND_FIELD, DESCENDING)
                                 .orderedBy(FIRST_FIELD, DESCENDING)
                                 .build();
            assertNotNull(query);
            assertEquals(order(FIRST_FIELD, DESCENDING), query.getOrder());
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
        Message columnValue2 = newMessageId();
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
