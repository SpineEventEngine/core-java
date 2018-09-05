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

import io.spine.test.client.TestEntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.client.given.ActorRequestFactoryTestEnv.requestFactory;
import static io.spine.client.given.QueryFactoryTestEnv.TEST_ENTITY_TYPE;
import static io.spine.client.given.QueryFactoryTestEnv.checkFieldMaskEmpty;
import static io.spine.client.given.QueryFactoryTestEnv.checkFiltersEmpty;
import static io.spine.client.given.QueryFactoryTestEnv.checkIdQueriesEqual;
import static io.spine.client.given.QueryFactoryTestEnv.checkTargetIsTestEntity;
import static io.spine.client.given.QueryFactoryTestEnv.singleTestEntityPath;
import static io.spine.client.given.QueryFactoryTestEnv.threeIds;
import static io.spine.client.given.QueryFactoryTestEnv.threeRandomParts;
import static io.spine.client.given.QueryFactoryTestEnv.verifyIdFilter;
import static io.spine.client.given.QueryFactoryTestEnv.verifyMultiplePathsInQuery;
import static io.spine.client.given.QueryFactoryTestEnv.verifySinglePathInQuery;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings({"LocalVariableNamingConvention",
                   "DuplicateStringLiteralInspection"}) // A lot of similar test display names.
@DisplayName("Query factory should")
class QueryFactoryTest {

    private QueryFactory factory;

    @BeforeEach
    void createFactory() {
        factory = requestFactory().query();
    }

    @Nested
    @DisplayName("compose query of type")
    class ComposeQuery {

        @Test
        @DisplayName("`read all`")
        void readAll() {
            Query readAllQuery = factory.all(TEST_ENTITY_TYPE);
            assertNotNull(readAllQuery);

            checkFiltersEmpty(readAllQuery);
            checkTargetIsTestEntity(readAllQuery);

            checkFieldMaskEmpty(readAllQuery);
        }

        @Test
        @DisplayName("`read by IDs`")
        void readByIds() {
            Set<TestEntityId> testEntityIds = threeIds();
            Query readByIdsQuery = factory.byIds(TEST_ENTITY_TYPE, testEntityIds);
            assertNotNull(readByIdsQuery);

            checkFieldMaskEmpty(readByIdsQuery);

            Target target = checkTargetIsTestEntity(readByIdsQuery);

            verifyIdFilter(testEntityIds, target.getFilters());
        }

        @Test
        @DisplayName("`read all` with single path mask")
        void readAllWithSinglePath() {
            String expectedEntityPath = singleTestEntityPath();
            Query readAllWithPathFilteringQuery = factory.allWithMask(TEST_ENTITY_TYPE, 
                                                                      expectedEntityPath);
            assertNotNull(readAllWithPathFilteringQuery);

            checkFiltersEmpty(readAllWithPathFilteringQuery);
            checkTargetIsTestEntity(readAllWithPathFilteringQuery);
            verifySinglePathInQuery(expectedEntityPath, readAllWithPathFilteringQuery);
        }

        @Test
        @DisplayName("`read by IDs` with single path mask")
        void readByIdsWitSinglePath() {
            Set<TestEntityId> testEntityIds = threeIds();
            String expectedPath = singleTestEntityPath();
            Query readByIdsWithSinglePathQuery = factory.byIdsWithMask(TEST_ENTITY_TYPE,
                                                                       testEntityIds, expectedPath);
            assertNotNull(readByIdsWithSinglePathQuery);

            Target target = checkTargetIsTestEntity(readByIdsWithSinglePathQuery);

            verifyIdFilter(testEntityIds, target.getFilters());
            verifySinglePathInQuery(expectedPath, readByIdsWithSinglePathQuery);
        }

        @Test
        @DisplayName("`read all` with multiple paths mask")
        void readAllWithMultiplePaths() {
            String[] paths = threeRandomParts();
            Query readAllWithPathFilteringQuery = factory.allWithMask(TEST_ENTITY_TYPE, paths);
            assertNotNull(readAllWithPathFilteringQuery);

            checkFiltersEmpty(readAllWithPathFilteringQuery);
            checkTargetIsTestEntity(readAllWithPathFilteringQuery);
            verifyMultiplePathsInQuery(paths, readAllWithPathFilteringQuery);
        }

        @Test
        @DisplayName("`read by IDs` with multiple paths mask")
        void readByIdsWithMultiplePaths() {
            Set<TestEntityId> testEntityIds = threeIds();
            String[] paths = threeRandomParts();
            Query readByIdsWithSinglePathQuery = factory.byIdsWithMask(TEST_ENTITY_TYPE, 
                                                                       testEntityIds, paths);
            assertNotNull(readByIdsWithSinglePathQuery);

            Target target = checkTargetIsTestEntity(readByIdsWithSinglePathQuery);

            verifyIdFilter(testEntityIds, target.getFilters());
            verifyMultiplePathsInQuery(paths, readByIdsWithSinglePathQuery);
        }
    }

    @Test
    @DisplayName("fail to create query with mask when id list is empty")
    void failForEmptyIds() {
        assertThrows(IllegalArgumentException.class,
                     () -> factory.byIdsWithMask(TEST_ENTITY_TYPE, emptySet(), "", ""));
    }

    @Nested
    @DisplayName("be consistent with QueryBuilder when creating query")
    class CreateQuery {

        @Test
        @DisplayName("by IDs")
        void byIdConsistently() {
            Set<TestEntityId> ids = threeIds();
            Query fromFactory = factory.byIds(TEST_ENTITY_TYPE, ids);
            Query fromBuilder = factory.select(TEST_ENTITY_TYPE)
                                       .byId(ids)
                                       .build();
            checkIdQueriesEqual(fromFactory, fromBuilder);
        }

        @Test
        @DisplayName("by mask")
        void byMaskConsistently() {
            String field1 = "TestEntity.firstField";
            String field2 = "TesEntity.barField";

            Query fromFactory = factory.allWithMask(TEST_ENTITY_TYPE, field1, field2);
            Query fromBuilder = factory.select(TEST_ENTITY_TYPE)
                                       .withMask(field1, field2)
                                       .build();
            assertNotEquals(fromBuilder.getId(), fromFactory.getId());
            Target targetFromFactory = fromFactory.getTarget();
            Target targetFromBuilder = fromBuilder.getTarget();
            assertEquals(targetFromFactory, targetFromBuilder);
        }

        @Test
        @DisplayName("`read all`")
        void readAllConsistently() {
            Query fromFactory = factory.all(TEST_ENTITY_TYPE);
            Query fromBuilder = factory.select(TEST_ENTITY_TYPE)
                                       .build();
            assertEquals(fromFactory.getTarget(), fromBuilder.getTarget());
            assertNotEquals(fromFactory.getId(), fromBuilder.getId());
        }

        @Test
        @DisplayName("by IDs with mask")
        void byIdsWithMaskConsistently() {
            String field1 = "TestEntity.secondField";
            String field2 = "TesEntity.fooField";

            Set<TestEntityId> ids = threeIds();
            Query fromFactory = factory.byIdsWithMask(TEST_ENTITY_TYPE, ids, field1, field2);
            Query fromBuilder = factory.select(TEST_ENTITY_TYPE)
                                       .byId(ids)
                                       .withMask(field1, field2)
                                       .build();
            checkIdQueriesEqual(fromFactory, fromBuilder);
        }
    }
}
