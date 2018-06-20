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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.spine.server.entity.storage.Columns.getAllColumns;
import static io.spine.test.Verify.assertFalse;
import static io.spine.test.Verify.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dmytro Kuzmin
 */
public class EntityColumnCacheShould {

    private static final String STRING_ID = "some-string-id-never-used";

    private Class<? extends Entity> entityClass;
    private EntityColumnCache entityColumnCache;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        Entity entity = new EntityWithManyGetters(STRING_ID);
        entityClass = entity.getClass();
        entityColumnCache = EntityColumnCache.initializeFor(entityClass);
    }

    @Test
    @DisplayName("pass null check")
    void passNullCheck() {
        new NullPointerTester().testAllPublicStaticMethods(EntityColumnCache.class);
        new NullPointerTester().testAllPublicInstanceMethods(entityColumnCache);
    }

    @Test
    @DisplayName("stay empty after creation")
    void stayEmptyAfterCreation() {
        assertTrue(entityColumnCache.isEmpty());
    }

    @SuppressWarnings("CheckReturnValue") 
        // This test does not use the found columns, but simply checks that they are found.
    @Test
    @DisplayName("cache columns on first access")
    void cacheColumnsOnFirstAccess() {
        EntityColumnCache cacheForGetAll = EntityColumnCache.initializeFor(entityClass);
        cacheForGetAll.getColumns();
        assertFalse(cacheForGetAll.isEmpty());

        EntityColumnCache cacheForFind = EntityColumnCache.initializeFor(entityClass);
        cacheForFind.findColumn("floatNull");
        assertFalse(cacheForFind.isEmpty());
    }

    @Test
    @DisplayName("allow to forcefully cache columns")
    void allowToForcefullyCacheColumns() {
        entityColumnCache.ensureColumnsCached();
        assertFalse(entityColumnCache.isEmpty());
    }

    @Test
    @DisplayName("retrieve column metadata from given class")
    void retrieveColumnMetadataFromGivenClass() {
        String existingColumnName = "floatNull";
        EntityColumn retrievedColumn = entityColumnCache.findColumn(existingColumnName);
        assertNotNull(retrievedColumn);
        assertEquals(existingColumnName, retrievedColumn.getName());
    }

    @Test
    @DisplayName("fail to retrieve non existing column")
    void failToRetrieveNonExistingColumn() {
        String nonExistingColumnName = "foo";
        thrown.expect(IllegalArgumentException.class);
        entityColumnCache.findColumn(nonExistingColumnName);
    }

    @Test
    @DisplayName("retain stored columns order")
    void retainStoredColumnsOrder() {
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
