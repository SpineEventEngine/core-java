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

import com.google.protobuf.FieldMask;
import com.google.protobuf.ProtocolStringList;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.test.queries.TestEntity;
import org.spine3.test.queries.TestEntityId;
import org.spine3.type.TypeName;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"LocalVariableNamingConvention", "MethodParameterNamingConvention"})
public class QueryFactoryShould extends ActorRequestFactoryShould<QueryFactory,
        QueryFactory.Builder> {

    // See {@code client_requests} for declaration.
    private static final Class<TestEntity> TARGET_ENTITY_CLASS = TestEntity.class;

    @Override
    protected QueryFactory.Builder builder() {
        return QueryFactory.newBuilder();
    }

    @Test
    public void compose_proper_read_all_query() {
        final Query readAllQuery = factory().readAll(TARGET_ENTITY_CLASS);
        assertNotNull(readAllQuery);

        checkTypeCorrectAndFiltersEmpty(TARGET_ENTITY_CLASS, readAllQuery);

        checkFieldMaskEmpty(readAllQuery);
    }

    @Test
    public void compose_proper_read_all_query_with_single_path() {
        final String expectedEntityPath = singleTestEntityPath();
        final Query readAllWithPathFilteringQuery =
                factory().readAll(TARGET_ENTITY_CLASS, expectedEntityPath);
        assertNotNull(readAllWithPathFilteringQuery);

        checkTypeCorrectAndFiltersEmpty(TARGET_ENTITY_CLASS, readAllWithPathFilteringQuery);
        verifySinglePathInQuery(expectedEntityPath, readAllWithPathFilteringQuery);
    }

    @Test
    public void compose_proper_read_all_query_with_multiple_random_paths() {

        final String[] paths = multipleRandomPaths();
        final Query readAllWithPathFilteringQuery = factory().readAll(TARGET_ENTITY_CLASS, paths);
        assertNotNull(readAllWithPathFilteringQuery);

        checkTypeCorrectAndFiltersEmpty(TARGET_ENTITY_CLASS, readAllWithPathFilteringQuery);
        verifyMultiplePathsInQuery(paths, readAllWithPathFilteringQuery);
    }

    @Test
    public void compose_proper_read_by_ids_query() {
        final Set<TestEntityId> testEntityIds = multipleIds();
        final Query readByIdsQuery = factory().readByIds(TARGET_ENTITY_CLASS, testEntityIds);
        assertNotNull(readByIdsQuery);

        checkFieldMaskEmpty(readByIdsQuery);

        final Target target = checkTarget(TARGET_ENTITY_CLASS, readByIdsQuery);

        verifyIdFilter(testEntityIds, target.getFilters());
    }

    @Test
    public void compose_proper_read_by_ids_query_with_single_path() {
        final Set<TestEntityId> testEntityIds = multipleIds();
        final String expectedPath = singleTestEntityPath();
        final Query readByIdsWithSinglePathQuery = factory().readByIds(
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
        final Query readByIdsWithSinglePathQuery = factory().readByIds(
                TARGET_ENTITY_CLASS,
                testEntityIds,
                paths);
        assertNotNull(readByIdsWithSinglePathQuery);

        final Target target = checkTarget(TARGET_ENTITY_CLASS, readByIdsWithSinglePathQuery);

        verifyIdFilter(testEntityIds, target.getFilters());
        verifyMultiplePathsInQuery(paths, readByIdsWithSinglePathQuery);
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

    private static Target checkTarget(Class<TestEntity> targetEntityClass, Query query) {
        final Target entityTarget = query.getTarget();
        assertNotNull(entityTarget);

        final String expectedTypeName = TypeName.of(targetEntityClass)
                                                .value();
        assertEquals(expectedTypeName, entityTarget.getType());
        return entityTarget;
    }
}
