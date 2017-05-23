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

import com.google.common.base.Function;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeConverter;
import org.spine3.test.client.TestEntity;
import org.spine3.test.validate.msg.ProjectId;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.transform;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.client.ColumnFilters.eq;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.test.Verify.fail;

/**
 * @author Dmytro Dashenkov
 */
public class QueryBuilderShould extends ActorRequestFactoryShould {

    @Test
    public void not_accept_nulls_on_creation() {
        new NullPointerTester().testAllPublicStaticMethods(QueryBuilder.class);
    }

    @Test
    public void create_queries_with_type_only() {
        final Class<? extends Message> testEntityClass = TestEntity.class;
        final Query query = factory().query()
                                     .select(testEntityClass)
                                     .build();
        assertNotNull(query);
        assertFalse(query.hasFieldMask());

        final Target target = query.getTarget();
        assertTrue(target.getIncludeAll());

        assertEquals(TypeName.of(testEntityClass)
                             .value(), target.getType());
    }

    @Test
    public void create_queries_with_ids() {
        final int id1 = 314;
        final int id2 = 271;
        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .byId(id1, id2)
                                     .build();
        assertNotNull(query);
        assertFalse(query.hasFieldMask());

        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());

        final EntityFilters entityFilters = target.getFilters();
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final Collection<EntityId> idValues = idFilter.getIdsList();
        final Function<EntityId, Integer> transformer = new EntityIdUnpacker<>(int.class);
        final Collection<Integer> intIdValues = transform(idValues, transformer);

        assertSize(2, idValues);
        assertThat(intIdValues, containsInAnyOrder(id1, id2));
    }

    @Test
    public void create_queries_with_field_mask() {
        final String fieldName = "TestEntity.firstField";
        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .withMask(fieldName)
                                     .build();
        assertNotNull(query);
        assertTrue(query.hasFieldMask());

        final FieldMask mask = query.getFieldMask();
        final Collection<String> fieldNames = mask.getPathsList();
        assertSize(1, fieldNames);
        assertContains(fieldName, fieldNames);
    }

    @Test
    public void create_queries_with_param() {
        final String columnName = "myImaginaryColumn";
        final Object columnValue = 42;

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .where(eq(columnName, columnValue))
                                     .build();
        assertNotNull(query);
        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());

        final EntityFilters entityFilters = target.getFilters();
        final List<AggregatingColumnFilter> aggregatingColumnFilters = entityFilters.getFilterList();
        assertSize(1, aggregatingColumnFilters);
        final AggregatingColumnFilter aggregatingColumnFilter = aggregatingColumnFilters.get(0);
        final Collection<ColumnFilter> columnFilters = aggregatingColumnFilter.getFilterList();
        assertSize(1, columnFilters);
        final Any actualValue = findByName(columnFilters, columnName).getValue();
        assertNotNull(columnValue);
        final Int32Value messageValue = AnyPacker.unpack(actualValue);
        final int actualGenericValue = messageValue.getValue();
        assertEquals(columnValue, actualGenericValue);
    }

    @Test
    public void create_queries_with_multiple_params() {
        final String columnName1 = "myColumn";
        final Object columnValue1 = 42;
        final String columnName2 = "oneMore";
        final Object columnValue2 = newMessageId();

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .where(eq(columnName1, columnValue1),
                                            eq(columnName2, columnValue2))
                                     .build();
        assertNotNull(query);
        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());

        final EntityFilters entityFilters = target.getFilters();
        final List<AggregatingColumnFilter> aggregatingColumnFilters = entityFilters.getFilterList();
        assertSize(1, aggregatingColumnFilters);
        final Collection<ColumnFilter> columnFilters = aggregatingColumnFilters.get(0)
                                                                               .getFilterList();
        final Any actualValue1 = findByName(columnFilters, columnName1).getValue();
        assertNotNull(actualValue1);
        final int actualGenericValue1 = TypeConverter.toObject(actualValue1, int.class);
        assertEquals(columnValue1, actualGenericValue1);

        final Any actualValue2 = findByName(columnFilters, columnName2).getValue();
        assertNotNull(actualValue2);
        final Message actualGenericValue2 = TypeConverter.toObject(actualValue2, ProjectId.class);
        assertEquals(columnValue2, actualGenericValue2);
    }

    @SuppressWarnings("OverlyLongMethod")
    // A big test case covering the query arguments co-living
    @Test
    public void create_queries_with_all_arguments() {
        final Class<? extends Message> testEntityClass = TestEntity.class;
        final int id1 = 314;
        final int id2 = 271;
        final String columnName1 = "column1";
        final Object columnValue1 = 42;
        final String columnName2 = "column2";
        final Object columnValue2 = newMessageId();
        final String fieldName = "TestEntity.secondField";
        final Query query = factory().query()
                                     .select(testEntityClass)
                                     .withMask(fieldName)
                                     .byId(id1, id2)
                                     .where(eq(columnName1, columnValue1),
                                            eq(columnName2, columnValue2))
                                     .build();
        assertNotNull(query);

        // Check FieldMask
        final FieldMask mask = query.getFieldMask();
        final Collection<String> fieldNames = mask.getPathsList();
        assertSize(1, fieldNames);
        assertContains(fieldName, fieldNames);

        final Target target = query.getTarget();
        assertFalse(target.getIncludeAll());
        final EntityFilters entityFilters = target.getFilters();

        // Check IDs
        final EntityIdFilter idFilter = entityFilters.getIdFilter();
        final Collection<EntityId> idValues = idFilter.getIdsList();
        final Function<EntityId, Integer> transformer = new EntityIdUnpacker<>(int.class);
        final Collection<Integer> intIdValues = transform(idValues, transformer);

        assertSize(2, idValues);
        assertThat(intIdValues, containsInAnyOrder(id1, id2));

        // Check query params
        final List<AggregatingColumnFilter> aggregatingColumnFilters = entityFilters.getFilterList();
        assertSize(1, aggregatingColumnFilters);
        final Collection<ColumnFilter> columnFilters = aggregatingColumnFilters.get(0)
                                                                               .getFilterList();
        assertSize(2, columnFilters);

        final Any actualValue1 = findByName(columnFilters, columnName1).getValue();
        assertNotNull(actualValue1);
        final int actualGenericValue1 = TypeConverter.toObject(actualValue1, int.class);
        assertEquals(columnValue1, actualGenericValue1);

        final Any actualValue2 = findByName(columnFilters, columnName2).getValue();
        assertNotNull(actualValue2);
        final Message actualGenericValue2 = TypeConverter.toObject(actualValue2, ProjectId.class);
        assertEquals(columnValue2, actualGenericValue2);
    }

    @Test
    public void persist_only_last_ids_clause() {
        final Iterable<?> genericIds = asList(newUuid(),
                                              -1,
                                              newMessageId());
        final Long[] longIds = {1L, 2L, 3L};
        final Message[] messageIds = {
                newMessageId(),
                newMessageId(),
                newMessageId()
        };
        final String[] stringIds = {
                newUuid(),
                newUuid(),
                newUuid()
        };
        final Integer[] intIds = {4, 5, 6};

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .byId(genericIds)
                                     .byId(longIds)
                                     .byId(stringIds)
                                     .byId(intIds)
                                     .byId(messageIds)
                                     .build();
        assertNotNull(query);

        final Target target = query.getTarget();
        final EntityFilters filters = target.getFilters();
        final Collection<EntityId> entityIds = filters.getIdFilter()
                                                      .getIdsList();
        assertSize(messageIds.length, entityIds);
        final Function<EntityId, ProjectId> transformer = new EntityIdUnpacker<>(ProjectId.class);
        final Iterable<? extends Message> actualValues = transform(entityIds, transformer);
        assertThat(actualValues, containsInAnyOrder(messageIds));
    }

    @Test
    public void persist_only_last_field_mask() {
        final Iterable<String> iterableFields = singleton("TestEntity.firstField");
        final String[] arrayFields = {"TestEntity.secondField"};

        final Query query = factory().query()
                                     .select(TestEntity.class)
                                     .withMask(iterableFields)
                                     .withMask(arrayFields)
                                     .build();
        assertNotNull(query);
        final FieldMask mask = query.getFieldMask();

        final Collection<String> maskFields = mask.getPathsList();
        assertSize(arrayFields.length, maskFields);
        assertThat(maskFields, contains(arrayFields));
    }

    @Test
    public void support_toString() {
        final Class<? extends Message> testEntityClass = TestEntity.class;
        final int id1 = 314;
        final int id2 = 271;
        final String columnName1 = "column1";
        final Object columnValue1 = 42;
        final String columnName2 = "column2";
        final Message columnValue2 = newMessageId();
        final String fieldName = "TestEntity.secondField";
        final QueryBuilder builder = factory().query()
                                              .select(testEntityClass)
                                              .withMask(fieldName)
                                              .byId(id1, id2)
                                              .where(eq(columnName1, columnValue1),
                                                     eq(columnName2, columnValue2));
        final String stringRepr = builder.toString();

        assertThat(stringRepr, containsString(testEntityClass.getSimpleName()));
        assertThat(stringRepr, containsString(valueOf(id1)));
        assertThat(stringRepr, containsString(valueOf(id2)));
        assertThat(stringRepr, containsString(columnName1));
        assertThat(stringRepr, containsString(columnName2));
    }

    private static ProjectId newMessageId() {
        return ProjectId.newBuilder()
                        .setValue(Identifiers.newUuid())
                        .build();
    }

    private static class EntityIdUnpacker<T> implements Function<EntityId, T> {

        private final Class<T> targetClass;

        private EntityIdUnpacker(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public T apply(@Nullable EntityId entityId) {
            assertNotNull(entityId);
            final Any value = entityId.getId();
            final T actual = TypeConverter.toObject(value, targetClass);
            return actual;
        }
    }

    private static ColumnFilter findByName(Iterable<ColumnFilter> filters, String name) {
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
