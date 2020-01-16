/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.truth.StringSubject;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.either;
import static io.spine.client.Filters.eq;
import static io.spine.client.Filters.ge;
import static io.spine.client.Filters.le;
import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactory;
import static io.spine.client.given.TestEntities.randomId;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.TypeConverter.toObject;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Topic builder should")
@SuppressWarnings("DuplicateStringLiteralInspection")
class TopicBuilderTest {

    private static final Class<? extends Message> TEST_ENTITY_TYPE = TestEntity.class;
    private static final TypeUrl TEST_ENTITY_TYPE_URL = TypeUrl.of(TEST_ENTITY_TYPE);
    private TopicFactory factory;

    static Filter findByName(Iterable<Filter> filters, String name) {
        for (Filter filter : filters) {
            if (filter.getFieldPath()
                      .getFieldName(0)
                      .equals(name)) {
                return filter;
            }
        }
        fail(format("No Filter found for %s. field", name));
        // avoid returning `null`
        throw new RuntimeException("never happens unless JUnit is broken");
    }

    @BeforeEach
    void createFactory() {
        factory = requestFactory().topic();
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void notAcceptNulls() {
        new NullPointerTester().testAllPublicStaticMethods(TopicBuilder.class);
    }

    @Nested
    @DisplayName("create a topic")
    class CreateTopic {

        @Test
        @DisplayName("for the type")
        void byType() {
            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .build();
            assertNotNull(topic);
            assertFalse(topic.hasFieldMask());

            Target target = topic.getTarget();
            assertTrue(target.getIncludeAll());

            assertEquals(TEST_ENTITY_TYPE_URL.value(), target.getType());
        }

        @Test
        @DisplayName("for IDs")
        void byId() {
            int id1 = 314;
            int id2 = 271;
            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .byId(id1, id2)
                                 .build();
            assertNotNull(topic);
            assertFalse(topic.hasFieldMask());

            Target target = topic.getTarget();
            assertFalse(target.getIncludeAll());

            TargetFilters entityFilters = target.getFilters();
            IdFilter idFilter = entityFilters.getIdFilter();
            Collection<Any> idValues = idFilter.getIdList();
            Collection<Integer> intIdValues = idValues
                    .stream()
                    .map(id -> toObject(id, int.class))
                    .collect(Collectors.toList());

            assertThat(intIdValues)
                 .containsExactly(id1, id2);
        }

        @Test
        @DisplayName("with a field mask")
        void byFieldMask() {
            String fieldName = "TestEntity.firstField";
            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(fieldName)
                                 .build();
            assertNotNull(topic);
            assertTrue(topic.hasFieldMask());

            FieldMask mask = topic.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();

            IterableSubject assertFieldNames = assertThat(fieldNames);
            assertFieldNames.hasSize(1);
            assertFieldNames.containsExactly(fieldName);
        }

        @Test
        @DisplayName("matching a predicate")
        void byFilter() {
            String columnName = "first_field";
            Object columnValue = "a column value";

            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .where(eq(columnName, columnValue))
                                 .build();
            assertNotNull(topic);
            Target target = topic.getTarget();
            assertFalse(target.getIncludeAll());

            TargetFilters entityFilters = target.getFilters();
            List<CompositeFilter> aggregatingFilters = entityFilters.getFilterList();
            assertThat(aggregatingFilters)
                 .hasSize(1);
            CompositeFilter aggregatingFilter = aggregatingFilters.get(0);
            Collection<Filter> filters = aggregatingFilter.getFilterList();
            assertThat(filters)
                 .hasSize(1);
            Any actualValue = findByName(filters, columnName).getValue();
            assertNotNull(columnValue);
            StringValue messageValue = unpack(actualValue, StringValue.class);
            String actualGenericValue = messageValue.getValue();
            assertEquals(columnValue, actualGenericValue);
        }

        @Test
        @DisplayName("matching multiple predicates")
        void byMultipleFilters() {
            String columnName1 = "first_field";
            Object columnValue1 = "the column value";
            String columnName2 = "second_field";
            Object columnValue2 = false;

            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                                 .build();
            assertNotNull(topic);
            Target target = topic.getTarget();
            assertFalse(target.getIncludeAll());

            TargetFilters entityFilters = target.getFilters();
            List<CompositeFilter> aggregatingFilters = entityFilters.getFilterList();
            assertThat(aggregatingFilters)
                 .hasSize(1);
            Collection<Filter> filters = aggregatingFilters.get(0)
                                                           .getFilterList();
            Any actualValue1 = findByName(filters, columnName1).getValue();
            assertNotNull(actualValue1);
            String actualGenericValue1 = toObject(actualValue1, String.class);
            assertEquals(columnValue1, actualGenericValue1);

            Any actualValue2 = findByName(filters, columnName2).getValue();
            assertNotNull(actualValue2);
            boolean actualGenericValue2 = toObject(actualValue2, boolean.class);
            assertEquals(columnValue2, actualGenericValue2);
        }

        @SuppressWarnings("OverlyLongMethod")
        // A big test for the grouping operators proper building.
        @Test
        @DisplayName("with filter groupings")
        void byFilterGrouping() {
            String firstColumn = "first_field";
            String secondColumn = "second_field";
            String thirdColumn = "third_field";
            String countryName = "Ukraine";

            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .where(all(ge(thirdColumn, 50),
                                            le(thirdColumn, 1000)),
                                        either(eq(secondColumn, false),
                                               eq(firstColumn, countryName)))
                                 .build();
            Target target = topic.getTarget();
            List<CompositeFilter> filters = target.getFilters()
                                                  .getFilterList();
            assertThat(filters)
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
            assertThat(allFilters)
                 .hasSize(2);
            assertThat(eitherFilters)
                 .hasSize(2);

            Filter lowerBound = allFilters.get(0);
            String columnName1 = lowerBound.getFieldPath()
                                           .getFieldName(0);
            assertEquals(thirdColumn, columnName1);
            assertEquals(50L, (long) toObject(lowerBound.getValue(), int.class));
            assertEquals(GREATER_OR_EQUAL, lowerBound.getOperator());

            Filter higherBound = allFilters.get(1);
            String columnName2 = higherBound.getFieldPath()
                                                       .getFieldName(0);
            assertEquals(thirdColumn, columnName2);
            assertEquals(1000L, (long) toObject(higherBound.getValue(), int.class));
            assertEquals(LESS_OR_EQUAL, higherBound.getOperator());

            Filter establishedTimeFilter = eitherFilters.get(0);
            String columnName3 = establishedTimeFilter.getFieldPath()
                                                      .getFieldName(0);
            assertEquals(secondColumn, columnName3);
            assertEquals(false, toObject(establishedTimeFilter.getValue(), boolean.class));
            assertEquals(EQUAL, establishedTimeFilter.getOperator());

            Filter countryFilter = eitherFilters.get(1);
            String columnName4 = countryFilter.getFieldPath()
                                              .getFieldName(0);
            assertEquals(firstColumn, columnName4);
            assertEquals(countryName, toObject(countryFilter.getValue(), String.class));
            assertEquals(EQUAL, countryFilter.getOperator());
        }

        @SuppressWarnings("OverlyLongMethod")
        // A big test case covering the query arguments coexistence.
        @Test
        @DisplayName("with all parameters")
        void byAllArguments() {
            int id1 = 314;
            int id2 = 271;
            String columnName1 = "first_field";
            Object columnValue1 = "some column value";
            String columnName2 = "second_field";
            Object columnValue2 = true;
            String fieldName = "TestEntity.secondField";
            Topic query = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(fieldName)
                                 .byId(id1, id2)
                                 .where(eq(columnName1, columnValue1),
                                        eq(columnName2, columnValue2))
                                 .build();
            assertNotNull(query);

            // Check FieldMask
            FieldMask mask = query.getFieldMask();
            Collection<String> fieldNames = mask.getPathsList();

            IterableSubject assertFieldNames = assertThat(fieldNames);
            assertFieldNames.hasSize(1);
            assertFieldNames.containsExactly(fieldName);

            Target target = query.getTarget();
            assertFalse(target.getIncludeAll());
            TargetFilters targetFilters = target.getFilters();

            // Check IDs
            IdFilter idFilter = targetFilters.getIdFilter();
            Collection<Any> idValues = idFilter.getIdList();
            Collection<Integer> intIdValues = idValues
                    .stream()
                    .map(id -> toObject(id, int.class))
                    .collect(Collectors.toList());
            assertThat(intIdValues)
                .containsExactly(id1, id2);

            // Check query params
            List<CompositeFilter> aggregatingFilters = targetFilters.getFilterList();
            assertThat(aggregatingFilters)
                 .hasSize(1);
            Collection<Filter> filters = aggregatingFilters.get(0)
                                                           .getFilterList();
            assertThat(filters)
                 .hasSize(2);

            Any actualValue1 = findByName(filters, columnName1).getValue();
            assertNotNull(actualValue1);
            String actualGenericValue1 = toObject(actualValue1, String.class);
            assertEquals(columnValue1, actualGenericValue1);

            Any actualValue2 = findByName(filters, columnName2).getValue();
            assertNotNull(actualValue2);
            boolean actualGenericValue2 = toObject(actualValue2, boolean.class);
            assertEquals(columnValue2, actualGenericValue2);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    @Test
    @DisplayName("fail when creating a topic with an invalid filter")
    void failOnInvalidFilter() {
        TopicBuilder builder = new TopicBuilder(TEST_ENTITY_TYPE, factory);
        builder.where(eq("non_existent_column", "some value"));

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Nested
    @DisplayName("persist only last received")
    class Persist {

        @Test
        @DisplayName("IDs")
        void lastIds() {
            Iterable<?> genericIds = asList(newUuid(), -1, randomId());
            Long[] longIds = {1L, 2L, 3L};
            Message[] messageIds = {randomId(), randomId(), randomId()};
            String[] stringIds = {newUuid(), newUuid(), newUuid()};
            Integer[] intIds = {4, 5, 6};

            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .byId(genericIds)
                                 .byId(longIds)
                                 .byId(stringIds)
                                 .byId(intIds)
                                 .byId(messageIds)
                                 .build();
            assertNotNull(topic);

            Target target = topic.getTarget();
            TargetFilters filters = target.getFilters();
            Collection<Any> entityIds = filters.getIdFilter()
                                               .getIdList();
            assertThat(entityIds)
                 .hasSize(messageIds.length);
            Iterable<? extends Message> actualValues = entityIds
                    .stream()
                    .map(id -> toObject(id, TestEntityId.class))
                    .collect(Collectors.toList());
            assertThat(actualValues)
                .containsExactlyElementsIn(messageIds);
        }

        @Test
        @DisplayName("field mask")
        void lastFieldMask() {
            Iterable<String> iterableFields = singleton("TestEntity.firstField");
            String[] arrayFields = {"TestEntity.secondField"};

            Topic topic = factory.select(TEST_ENTITY_TYPE)
                                 .withMask(iterableFields)
                                 .withMask(arrayFields)
                                 .build();
            assertNotNull(topic);
            FieldMask mask = topic.getFieldMask();

            Collection<String> maskFields = mask.getPathsList();
            IterableSubject assertMaskFields = assertThat(maskFields);
            assertMaskFields.hasSize(arrayFields.length);
            assertMaskFields.containsExactlyElementsIn(arrayFields);
        }
    }

    @Test
    @DisplayName("be represented as a comprehensible string")
    void supportToString() {
        int id1 = 314;
        int id2 = 271;
        String columnName1 = "column1";
        Object columnValue1 = 42;
        String columnName2 = "column2";
        Message columnValue2 = randomId();
        String fieldName = "TestEntity.secondField";
        TopicBuilder builder = factory.select(TEST_ENTITY_TYPE)
                                      .withMask(fieldName)
                                      .byId(id1, id2)
                                      .where(eq(columnName1, columnValue1),
                                             eq(columnName2, columnValue2));
        String topicString = builder.toString();

        StringSubject assertTopic = assertThat(topicString);
        assertTopic.contains(TEST_ENTITY_TYPE.getSimpleName());
        assertTopic.contains(String.valueOf(id1));
        assertTopic.contains(String.valueOf(id2));
        assertTopic.contains(columnName1);
        assertTopic.contains(columnName2);
    }
}
