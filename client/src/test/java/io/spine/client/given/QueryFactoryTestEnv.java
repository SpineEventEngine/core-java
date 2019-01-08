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

package io.spine.client.given;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.FieldMask;
import com.google.protobuf.ProtocolStringList;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.client.Query;
import io.spine.client.Target;
import io.spine.protobuf.AnyPacker;
import io.spine.test.client.TestEntity;
import io.spine.test.client.TestEntityId;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryFactoryTestEnv {

    // See {@code client_requests.proto} for declaration.
    public static final Class<TestEntity> TEST_ENTITY_TYPE = TestEntity.class;
    private static final TypeUrl TEST_ENTITY_TYPE_URL = TypeUrl.of(TEST_ENTITY_TYPE);

    /** Prevents instantiation of this test environment class. */
    private QueryFactoryTestEnv() {
    }

    public static TestEntityId entityId(int idValue) {
        return TestEntityId.newBuilder()
                           .setValue(idValue)
                           .build();
    }

    public static Set<TestEntityId> threeIds() {
        return newHashSet(testId(1), testId(7), testId(15));
    }

    private static TestEntityId testId(int value) {
        return TestEntityId.newBuilder()
                           .setValue(value)
                           .build();
    }

    public static String[] threeRandomParts() {
        return new String[]{"some", "random", "paths"};
    }

    public static void checkFieldMaskEmpty(Query query) {
        FieldMask fieldMask = query.getFieldMask();
        assertNotNull(fieldMask);
        assertEquals(FieldMask.getDefaultInstance(), fieldMask);
    }

    public static void checkFiltersEmpty(Query query) {
        Target entityTarget = query.getTarget();
        EntityFilters filters = entityTarget.getFilters();
        assertNotNull(filters);
        assertEquals(EntityFilters.getDefaultInstance(), filters);
    }

    public static void verifyIdFilter(Set<TestEntityId> expectedIds, EntityFilters filters) {
        assertNotNull(filters);
        EntityIdFilter idFilter = filters.getIdFilter();
        assertNotNull(idFilter);
        List<EntityId> actualListOfIds = idFilter.getIdsList();
        for (TestEntityId testEntityId : expectedIds) {
            EntityId expectedEntityId = EntityId.newBuilder()
                                                .setId(AnyPacker.pack(testEntityId))
                                                .build();
            assertTrue(actualListOfIds.contains(expectedEntityId));
        }
    }

    @CanIgnoreReturnValue
    public static Target checkTargetIsTestEntity(Query query) {
        Target entityTarget = query.getTarget();
        assertNotNull(entityTarget);

        assertEquals(TEST_ENTITY_TYPE_URL.value(), entityTarget.getType());
        return entityTarget;
    }

    public static void verifySinglePathInQuery(String expectedEntityPath, Query query) {
        FieldMask fieldMask = query.getFieldMask();
        assertEquals(1, fieldMask.getPathsCount());     // as we set the only path value.

        String firstPath = fieldMask.getPaths(0);
        assertEquals(expectedEntityPath, firstPath);
    }

    public static String singleTestEntityPath() {
        return TestEntity.getDescriptor()
                         .getFields()
                         .get(1)
                         .getFullName();
    }

    public static void verifyMultiplePathsInQuery(String[] paths,
                                                  Query readAllWithPathFilteringQuery) {
        FieldMask fieldMask = readAllWithPathFilteringQuery.getFieldMask();
        assertEquals(paths.length, fieldMask.getPathsCount());
        ProtocolStringList pathsList = fieldMask.getPathsList();
        for (String expectedPath : paths) {
            assertTrue(pathsList.contains(expectedPath));
        }
    }

    public static EntityFilters stripIdFilter(EntityFilters filters) {
        return filters.toBuilder()
                      .clearIdFilter()
                      .build();
    }

    public static Target stripFilters(Target target) {
        return target.toBuilder()
                     .clearFilters()
                     .build();
    }

    public static void checkIdQueriesEqual(Query query1, Query query2) {
        assertNotEquals(query1.getId(), query2.getId());

        Target factoryTarget = query1.getTarget();
        Target builderTarget = query2.getTarget();

        EntityFilters factoryFilters = factoryTarget.getFilters();
        EntityFilters builderFilters = builderTarget.getFilters();

        // Everything except filters is the same
        assertEquals(stripFilters(factoryTarget), stripFilters(builderTarget));

        EntityIdFilter factoryIdFilter = factoryFilters.getIdFilter();
        EntityIdFilter builderIdFilter = builderFilters.getIdFilter();

        // Everything except ID filter is the same
        assertEquals(stripIdFilter(factoryFilters), stripIdFilter(builderFilters));

        Collection<EntityId> factoryEntityIds = factoryIdFilter.getIdsList();
        Collection<EntityId> builderEntityIds = builderIdFilter.getIdsList();

        // Order may differ but all the elements are the same
        assertThat(builderEntityIds).hasSize(factoryEntityIds.size());
        assertThat(builderEntityIds).containsAllIn(factoryEntityIds);
    }
}
