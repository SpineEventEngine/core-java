/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import io.spine.protobuf.AnyPacker;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityId;
import io.spine.type.TypeUrl;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.test.Verify.assertContainsAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("LocalVariableNamingConvention")
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

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_query_with_empty_IDs_with_mask() {
        factory().query()
                 .byIdsWithMask(TestEntity.class,
                                Collections.<Message>emptySet(),
                                "", "");
    }

    @Test
    public void build_consistent_with_QueryBuilder_ID_queries() {
        final Set<TestEntityId> ids = multipleIds();
        final Query fromFactory = factory().query()
                                           .byIds(TestEntity.class, ids);
        final Query fromBuilder = factory().query()
                                           .select(TestEntity.class)
                                           .byId(ids)
                                           .build();
        checkIdQueriesEqual(fromFactory, fromBuilder);
    }

    @Test
    public void build_consistent_with_QueryBuilder_queries_with_mask() {
        final String field1 = "TestEntity.firstField";
        final String field2 = "TesEntity.barField";

        final Query fromFactory = factory().query()
                                           .allWithMask(TestEntity.class, field1, field2);
        final Query fromBuilder = factory().query()
                                          .select(TestEntity.class)
                                          .withMask(field1, field2)
                                          .build();
        assertNotEquals(fromBuilder.getId(), fromFactory.getId());
        final Target targetFromFactory = fromFactory.getTarget();
        final Target targetFromBuilder = fromBuilder.getTarget();
        assertEquals(targetFromFactory, targetFromBuilder);
    }

    @Test
    public void build_consistent_with_QueryBuilder_all_queries() {
        final Query fromFactory = factory().query().all(TestEntity.class);
        final Query fromBuilder = factory().query()
                                           .select(TestEntity.class)
                                           .build();
        assertEquals(fromFactory.getTarget(), fromBuilder.getTarget());
        assertNotEquals(fromFactory.getId(), fromBuilder.getId());
    }

    @Test
    public void build_consistent_with_QueryBuilder_ID_queries_with_mask() {
        final String field1 = "TestEntity.secondField";
        final String field2 = "TesEntity.fooField";

        final Set<TestEntityId> ids = multipleIds();
        final Query fromFactory = factory().query()
                                           .byIdsWithMask(TestEntity.class, ids, field1, field2);
        final Query fromBuilder = factory().query()
                                           .select(TestEntity.class)
                                           .byId(ids)
                                           .withMask(field1, field2)
                                           .build();
        checkIdQueriesEqual(fromFactory, fromBuilder);
    }

    private static void checkIdQueriesEqual(Query query1, Query query2) {
        assertNotEquals(query1.getId(), query2.getId());

        final Target targetFromFactory = query1.getTarget();
        final Target targetFromBuilder = query2.getTarget();

        final EntityFilters filtersFromFactory = targetFromFactory.getFilters();
        final EntityFilters filtersFromBuilder = targetFromBuilder.getFilters();

        // Everything except filters is the same
        assertEquals(targetFromFactory.toBuilder()
                                      .clearFilters()
                                      .build(),
                     targetFromBuilder.toBuilder()
                                      .clearFilters()
                                      .build());

        final EntityIdFilter idFilterFromFactory = filtersFromFactory.getIdFilter();
        final EntityIdFilter idFilterFromBuilder = filtersFromBuilder.getIdFilter();

        // Everything except ID filter is the same
        assertEquals(filtersFromBuilder.toBuilder()
                                       .clearIdFilter()
                                       .build(),
                     filtersFromBuilder.toBuilder()
                                       .clearIdFilter()
                                       .build());

        final Collection<EntityId> entityIdsFromFactory = idFilterFromFactory.getIdsList();
        final Collection<EntityId> entityIdsFromBuilder = idFilterFromBuilder.getIdsList();

        // Order may differ but all the elements are the same
        assertEquals(entityIdsFromFactory.size(), entityIdsFromBuilder.size());
        assertContainsAll(entityIdsFromBuilder, entityIdsFromFactory.toArray(
                new EntityId[entityIdsFromBuilder.size()]));
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

        final String expectedTypeName = TypeUrl.of(targetEntityClass)
                                               .value();
        assertEquals(expectedTypeName, entityTarget.getType());
        return entityTarget;
    }
}
