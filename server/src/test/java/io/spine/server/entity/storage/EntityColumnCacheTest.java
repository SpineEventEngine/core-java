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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.spine.server.entity.storage.Columns.getAllColumns;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Verify.assertFalse;
import static io.spine.testing.Verify.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Kuzmin
 */
@DisplayName("EntityColumnCache should")
class EntityColumnCacheTest {

    private static final String STRING_ID = "some-string-id-never-used";

    private Class<? extends Entity> entityClass;
    private EntityColumnCache entityColumnCache;

    @BeforeEach
    void setUp() {
        Entity entity = new EntityWithManyGetters(STRING_ID);
        entityClass = entity.getClass();
        entityColumnCache = EntityColumnCache.initializeFor(entityClass);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(EntityColumnCache.class);
        new NullPointerTester().testAllPublicInstanceMethods(entityColumnCache);
    }

    @Test
    @DisplayName("be empty on creation")
    void beEmptyOnCreation() {
        assertTrue(entityColumnCache.isEmpty());
    }

    @SuppressWarnings("CheckReturnValue")
    // This test does not use the found columns, but simply checks that they are found.
    @Test
    @DisplayName("cache columns on first access")
    void cacheOnFirstAccess() {
        EntityColumnCache cacheForGetAll = EntityColumnCache.initializeFor(entityClass);
        cacheForGetAll.getColumns();
        assertFalse(cacheForGetAll.isEmpty());

        EntityColumnCache cacheForFind = EntityColumnCache.initializeFor(entityClass);
        cacheForFind.findColumn("floatNull");
        assertFalse(cacheForFind.isEmpty());
    }

    @Test
    @DisplayName("allow to forcefully cache columns")
    void forcefullyCache() {
        entityColumnCache.ensureColumnsCached();
        assertFalse(entityColumnCache.isEmpty());
    }

    @Test
    @DisplayName("retrieve column metadata from given class")
    void getColumnMetadata() {
        String existingColumnName = "floatNull";
        EntityColumn retrievedColumn = entityColumnCache.findColumn(existingColumnName);
        assertNotNull(retrievedColumn);
        assertEquals(existingColumnName, retrievedColumn.getName());
    }

    @Test
    @DisplayName("fail to retrieve non-existing column")
    void notGetNonExisting() {
        String nonExistingColumnName = "foo";
        assertThrows(IllegalArgumentException.class,
                     () -> entityColumnCache.findColumn(nonExistingColumnName));
    }

    @Test
    @DisplayName("retain stored columns order")
    void retainOrder() {
        Collection<EntityColumn> columnsFromCache = entityColumnCache.getColumns();
        assertNotNull(columnsFromCache);

        Collection<EntityColumn> columnsViaUtil = getAllColumns(entityClass);

        int columnsFromCacheSize = columnsFromCache.size();
        int columnsViaUtilSize = columnsViaUtil.size();
        assertEquals(columnsViaUtilSize, columnsFromCacheSize);

        List<EntityColumn> columnsFromCacheList = new ArrayList<>(columnsFromCache);
        List<EntityColumn> columnsViaUtilList = new ArrayList<>(columnsViaUtil);

        assertEquals(columnsViaUtilList.get(0), columnsFromCacheList.get(0));
        assertEquals(columnsViaUtilList.get(1), columnsFromCacheList.get(1));
        assertEquals(columnsViaUtilList.get(2), columnsFromCacheList.get(2));
    }
}
