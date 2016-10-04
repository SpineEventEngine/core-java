/*
 *
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package org.spine3.base;

import com.google.protobuf.FieldMask;
import com.google.protobuf.ProtocolStringList;
import org.junit.Test;
import org.spine3.client.EntityFilters;
import org.spine3.client.Query;
import org.spine3.client.Target;
import org.spine3.protobuf.TypeUrl;
import org.spine3.test.queries.TestEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("LocalVariableNamingConvention")
public class QueriesShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Queries.class));
    }

    @Test
    public void compose_proper_read_all_query() {
        final Class<TestEntity> targetEntityClass = TestEntity.class;
        final Query readAllQuery = Queries.readAll(targetEntityClass);
        assertNotNull(readAllQuery);

        checkTypeCorrectAndFiltersEmpty(targetEntityClass, readAllQuery);

        // `FieldMask` must be default as `paths` were not set.
        final FieldMask fieldMask = readAllQuery.getFieldMask();
        assertNotNull(fieldMask);
        assertEquals(FieldMask.getDefaultInstance(), fieldMask);
    }

    @Test
    public void compose_proper_read_all_query_with_single_path() {
        final Class<TestEntity> targetEntityClass = TestEntity.class;
        final String firstPropertyFieldName = TestEntity.getDescriptor()
                                                        .getFields()
                                                        .get(1)
                                                        .getFullName();
        final Query readAllWithPathFilteringQuery = Queries.readAll(targetEntityClass, firstPropertyFieldName);
        assertNotNull(readAllWithPathFilteringQuery);

        checkTypeCorrectAndFiltersEmpty(targetEntityClass, readAllWithPathFilteringQuery);

        final FieldMask fieldMask = readAllWithPathFilteringQuery.getFieldMask();
        assertEquals(1, fieldMask.getPathsCount());     // as we set the only path value.

        final String firstPath = fieldMask.getPaths(0);
        assertEquals(firstPropertyFieldName, firstPath);
    }

    @Test
    public void compose_proper_read_all_query_with_multiple_paths() {
        final Class<TestEntity> targetEntityClass = TestEntity.class;

        final String[] paths = {"some", "random", "paths"};
        final Query readAllWithPathFilteringQuery = Queries.readAll(targetEntityClass, paths);
        assertNotNull(readAllWithPathFilteringQuery);

        checkTypeCorrectAndFiltersEmpty(targetEntityClass, readAllWithPathFilteringQuery);

        final FieldMask fieldMask = readAllWithPathFilteringQuery.getFieldMask();
        assertEquals(paths.length, fieldMask.getPathsCount());
        final ProtocolStringList pathsList = fieldMask.getPathsList();
        for (String expectedPath : paths) {
            assertTrue(pathsList.contains(expectedPath));
        }
    }


    private static void checkTypeCorrectAndFiltersEmpty(Class<TestEntity> targetEntityClass, Query readAllQuery) {
        final Target entityTarget = readAllQuery.getTarget();
        assertNotNull(entityTarget);

        final String expectedTypeName = TypeUrl.of(targetEntityClass)
                                               .getTypeName();
        assertEquals(expectedTypeName, entityTarget.getType());

        // `EntityFilters` must be default as this value was not set.
        final EntityFilters filters = entityTarget.getFilters();
        assertNotNull(filters);
        assertEquals(EntityFilters.getDefaultInstance(), filters);
    }

}
