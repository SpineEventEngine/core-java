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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import org.junit.Test;
import org.spine3.base.FieldFilter;
import org.spine3.people.PersonName;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.client.TestEntity;
import org.spine3.test.client.TestEntityId;
import org.spine3.type.TypeName;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"LocalVariableNamingConvention", "MethodParameterNamingConvention"})
public class QueryFactoryShould extends ActorRequestFactoryShould {

    // See {@code client_requests} for declaration.
    private static final Class<TestEntity> TARGET_ENTITY_CLASS = TestEntity.class;

    @Test
    public void compose_proper_read_all_query() {
        final Query readAllQuery = factory().query()
                                            .all(TARGET_ENTITY_CLASS);
        assertNotNull(readAllQuery);

        checkTypeCorrectAndFiltersEmpty(TARGET_ENTITY_CLASS, readAllQuery);

        checkFieldMaskEmpty(readAllQuery);
    }

    @Test
    public void compose_proper_read_all_query_with_single_path() {
        final String expectedEntityPath = singleTestEntityPath();
        final Query readAllWithPathFilteringQuery =
                factory().query()
                         .allWithMask(TARGET_ENTITY_CLASS, expectedEntityPath);
        assertNotNull(readAllWithPathFilteringQuery);

        checkTypeCorrectAndFiltersEmpty(TARGET_ENTITY_CLASS, readAllWithPathFilteringQuery);
        verifySinglePathInQuery(expectedEntityPath, readAllWithPathFilteringQuery);
    }

    @Test
    public void compose_proper_read_all_query_with_multiple_random_paths() {

        final String[] paths = multipleRandomPaths();
        final Query readAllWithPathFilteringQuery =
                factory().query()
                         .allWithMask(TARGET_ENTITY_CLASS, paths);
        assertNotNull(readAllWithPathFilteringQuery);

        checkTypeCorrectAndFiltersEmpty(TARGET_ENTITY_CLASS, readAllWithPathFilteringQuery);
        verifyMultiplePathsInQuery(paths, readAllWithPathFilteringQuery);
    }

    @Test
    public void compose_proper_read_by_ids_query() {
        final Set<TestEntityId> testEntityIds = multipleIds();
        final Query readByIdsQuery = factory().query()
                                              .byIds(TARGET_ENTITY_CLASS, testEntityIds);
        assertNotNull(readByIdsQuery);

        checkFieldMaskEmpty(readByIdsQuery);

        final Target target = checkTarget(TARGET_ENTITY_CLASS, readByIdsQuery);

        verifyIdFilter(testEntityIds, target.getFilters());
    }

    @Test
    public void compose_proper_read_by_ids_query_with_single_path() {
        final Set<TestEntityId> testEntityIds = multipleIds();
        final String expectedPath = singleTestEntityPath();
        final Query readByIdsWithSinglePathQuery = factory().query()
                                                            .byIdsWithMask(
                                                                    TARGET_ENTITY_CLASS,
                                                                    testEntityIds,
                                                                    expectedPath);
        assertNotNull(readByIdsWithSinglePathQuery);

        final Target target = checkTarget(TARGET_ENTITY_CLASS, readByIdsWithSinglePathQuery);

        verifyIdFilter(testEntityIds, target.getFilters());
        verifySinglePathInQuery(expectedPath, readByIdsWithSinglePathQuery);
    }

    @Test
    public void compose_proper_read_by_ids_query_with_multiple_random_paths() {
        final Set<TestEntityId> testEntityIds = multipleIds();
        final String[] paths = multipleRandomPaths();
        final Query readByIdsWithSinglePathQuery = factory().query()
                                                            .byIdsWithMask(
                                                                    TARGET_ENTITY_CLASS,
                                                                    testEntityIds,
                                                                    paths);
        assertNotNull(readByIdsWithSinglePathQuery);

        final Target target = checkTarget(TARGET_ENTITY_CLASS, readByIdsWithSinglePathQuery);

        verifyIdFilter(testEntityIds, target.getFilters());
        verifyMultiplePathsInQuery(paths, readByIdsWithSinglePathQuery);
    }

    @Test
    public void compose_proper_read_by_columns_query() {
        final Collection<FieldFilter> columnFilters = new HashSet<>(3);
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        final Class<TestEntity> entityClass = TestEntity.class;

        final Query query = factory().query()
                                     .byColumns(entityClass, columnFilters);
        final Target target = checkTarget(entityClass, query);
        final EntityFilters entityFilters = target.getFilters();
        verifyIdFilter(Collections.<TestEntityId>emptySet(), entityFilters);
        verifyColumnFilters(columnFilters, entityFilters);
    }

    @Test
    public void compose_proper_read_by_columns_with_simple_mask_query() {
        final Collection<FieldFilter> columnFilters = new HashSet<>(3);
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        final Class<TestEntity> entityClass = TestEntity.class;
        final String requiredField = singleTestEntityPath();

        final Query query = factory().query()
                                     .byColumnsWithMask(entityClass, columnFilters, requiredField);
        final Target target = checkTarget(entityClass, query);
        final EntityFilters entityFilters = target.getFilters();
        verifyIdFilter(Collections.<TestEntityId>emptySet(), entityFilters);
        verifySinglePathInQuery(requiredField, query);
        verifyColumnFilters(columnFilters, entityFilters);
    }

    @Test
    public void compose_proper_read_by_columns_with_big_mask_query() {
        final Collection<FieldFilter> columnFilters = new HashSet<>(3);
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        final Class<TestEntity> entityClass = TestEntity.class;
        final String[] requiredFields = multipleRandomPaths();

        final Query query = factory().query()
                                     .byColumnsWithMask(entityClass, columnFilters, requiredFields);
        final Target target = checkTarget(entityClass, query);
        final EntityFilters entityFilters = target.getFilters();
        verifyIdFilter(Collections.<TestEntityId>emptySet(), entityFilters);
        verifyMultiplePathsInQuery(requiredFields, query);
        verifyColumnFilters(columnFilters, entityFilters);
    }

    @Test
    public void compose_proper_read_by_IDs_and_columns_query() {
        final Collection<FieldFilter> columnFilters = new HashSet<>(3);
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        final Class<TestEntity> entityClass = TestEntity.class;
        final Set<TestEntityId> ids = multipleIds();

        final Query query = factory().query()
                                     .byIdsAndColumns(entityClass, ids, columnFilters);
        final Target target = checkTarget(entityClass, query);
        final EntityFilters entityFilters = target.getFilters();
        verifyIdFilter(Collections.<TestEntityId>emptySet(), entityFilters);
        verifyIdFilter(ids, entityFilters);
        verifyColumnFilters(columnFilters, entityFilters);
    }

    @Test
    public void compose_proper_read_by_IDs_and_columns_query_with_mask() {
        final Collection<FieldFilter> columnFilters = new HashSet<>(3);
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        columnFilters.add(randomFieldFilter());
        final Class<TestEntity> entityClass = TestEntity.class;
        final Set<TestEntityId> ids = multipleIds();
        final String[] requiredFields = multipleRandomPaths();

        final Query query = factory().query()
                                     .byIdsAndColumnsWithMask(entityClass,
                                                              ids,
                                                              columnFilters,
                                                              requiredFields);
        final Target target = checkTarget(entityClass, query);
        final EntityFilters entityFilters = target.getFilters();
        verifyIdFilter(Collections.<TestEntityId>emptySet(), entityFilters);
        verifyIdFilter(ids, entityFilters);
        verifyColumnFilters(columnFilters, entityFilters);
        verifyMultiplePathsInQuery(requiredFields, query);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_columns_set() {
        factory().query().byColumns(TestEntity.class, Collections.<FieldFilter>emptySet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_columns_and_IDs() {
        factory().query().byIdsAndColumns(TestEntity.class,
                                          Collections.singleton(Any.getDefaultInstance()),
                                          Collections.<FieldFilter>emptySet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_columns_with_mask() {
        factory().query().byColumnsWithMask(TestEntity.class,
                                            Collections.<FieldFilter>emptySet(),
                                            "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_columns_nad_IDs_with_mask() {
        factory().query().byIdsAndColumnsWithMask(TestEntity.class,
                                                  Collections.singleton(Any.getDefaultInstance()),
                                                  Collections.<FieldFilter>emptySet(),
                                                  "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_IDs_set_and_columns() {
        factory().query().byIdsAndColumns(TestEntity.class,
                                          Collections.<Message>emptySet(),
                                          Collections.singleton(FieldFilter.getDefaultInstance()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_IDs_with_mask() {
        factory().query().byIdsWithMask(TestEntity.class,
                                        Collections.<Message>emptySet(),
                                        "", "");
    }

    private static void verifyMultiplePathsInQuery(String[] paths,
                                                   Query readAllWithPathFilteringQuery) {
        final FieldMask fieldMask = readAllWithPathFilteringQuery.getFieldMask();
        assertEquals(paths.length, fieldMask.getPathsCount());
        final ProtocolStringList pathsList = fieldMask.getPathsList();
        for (String expectedPath : paths) {
            assertTrue(pathsList.contains(expectedPath));
        }
    }

    private static void verifySinglePathInQuery(String expectedEntityPath, Query query) {
        final FieldMask fieldMask = query.getFieldMask();
        assertEquals(1, fieldMask.getPathsCount());     // as we set the only path value.

        final String firstPath = fieldMask.getPaths(0);
        assertEquals(expectedEntityPath, firstPath);
    }

    private static String[] multipleRandomPaths() {
        return new String[]{"some", "random", "paths"};
    }

    private static FieldFilter randomFieldFilter() {
        final Random random = new SecureRandom();
        final String[] fieldNames = {"foo", "bar", "baz"};
        final PersonName value1 = PersonName.newBuilder()
                                            .setFamilyName("Doe")
                                            .build();
        final PersonName value2 = PersonName.newBuilder()
                                            .setGivenName("John")
                                            .build();
        final PersonName value3 = PersonName.newBuilder()
                                            .setMiddleName("Ivan")
                                            .build();
        final FieldFilter.Builder builder = FieldFilter.newBuilder();
        builder.setFieldPath(fieldNames[random.nextInt(fieldNames.length)]);
        if (random.nextBoolean()) {
            builder.addValue(AnyPacker.pack(value1));
        }
        if (random.nextBoolean()) {
            builder.addValue(AnyPacker.pack(value2));
        }
        if (random.nextBoolean()) {
            builder.addValue(AnyPacker.pack(value3));
        }
        final FieldFilter result = builder.build();
        return result;
    }

    private static String singleTestEntityPath() {
        return TestEntity.getDescriptor()
                         .getFields()
                         .get(1)
                         .getFullName();
    }

    private static Set<TestEntityId> multipleIds() {
        return newHashSet(TestEntityId.newBuilder()
                                      .setValue(1)
                                      .build(),
                          TestEntityId.newBuilder()
                                      .setValue(7)
                                      .build(),
                          TestEntityId.newBuilder()
                                      .setValue(15)
                                      .build());
    }

    private static void verifyIdFilter(Set<TestEntityId> expectedIds, EntityFilters filters) {
        assertNotNull(filters);
        final EntityIdFilter idFilter = filters.getIdFilter();
        assertNotNull(idFilter);
        final List<EntityId> actualListOfIds = idFilter.getIdsList();
        for (TestEntityId testEntityId : expectedIds) {
            final EntityId expectedEntityId = EntityId.newBuilder()
                                                      .setId(AnyPacker.pack(testEntityId))
                                                      .build();
            assertTrue(actualListOfIds.contains(expectedEntityId));
        }
    }

    private static void verifyColumnFilters(Collection<FieldFilter> columnFilters,
                                            EntityFilters filters) {
        assertNotNull(columnFilters);
        assertNotNull(filters);
        final List<FieldFilter> actualFilters = filters.getColumnFilterList();
        assertSize(columnFilters.size(), actualFilters);
        assertThat(actualFilters, containsInAnyOrder(columnFilters.toArray()));
    }

    private static void checkFieldMaskEmpty(Query query) {
        final FieldMask fieldMask = query.getFieldMask();
        assertNotNull(fieldMask);
        assertEquals(FieldMask.getDefaultInstance(), fieldMask);
    }

    private static void checkTypeCorrectAndFiltersEmpty(Class<TestEntity> expectedTargetClass,
                                                        Query query) {
        final Target entityTarget = checkTarget(expectedTargetClass, query);

        final EntityFilters filters = entityTarget.getFilters();
        assertNotNull(filters);
        assertEquals(EntityFilters.getDefaultInstance(), filters);
    }

    private static Target checkTarget(Class<? extends Message> targetEntityClass, Query query) {
        final Target entityTarget = query.getTarget();
        assertNotNull(entityTarget);

        final String expectedTypeName = TypeName.of(targetEntityClass)
                                                .value();
        assertEquals(expectedTypeName, entityTarget.getType());
        return entityTarget;
    }
}
