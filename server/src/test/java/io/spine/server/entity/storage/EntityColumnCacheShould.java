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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import org.junit.Before;
import org.junit.Test;

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


    @Before
    public void setUp() {
        final Entity entity = new EntityWithManyGetters(STRING_ID);
        entityClass = entity.getClass();
        entityColumnCache = EntityColumnCache.initializeFor(entityClass);
    }

    @Test
    public void pass_null_check() {
        new NullPointerTester().testAllPublicStaticMethods(EntityColumnCache.class);
        new NullPointerTester().testAllPublicInstanceMethods(entityColumnCache);
    }

    @Test
    public void create_empty_instance() {
        final EntityColumnCache columnCache = EntityColumnCache.getEmptyInstance();
        assertTrue(columnCache.isEmpty());
    }

    @Test
    public void stay_empty_after_creation() {
        assertTrue(entityColumnCache.isEmpty());
    }

    @Test
    public void cache_columns_on_first_access() {
        final EntityColumnCache cacheForGetAll = EntityColumnCache.initializeFor(entityClass);
        cacheForGetAll.getColumns();
        assertFalse(cacheForGetAll.isEmpty());

        final EntityColumnCache cacheForFind = EntityColumnCache.initializeFor(entityClass);
        final String floatNullKey = "floatNull";
        cacheForFind.findColumn(floatNullKey);
        assertFalse(cacheForFind.isEmpty());
    }

    @Test
    public void allow_to_forcefully_cache_columns() {
        entityColumnCache.ensureColumnsCached();
        assertFalse(entityColumnCache.isEmpty());
    }

    @Test
    public void retrieve_column_metadata_from_given_class() {
        final String existingColumnName = "floatNull";
        final EntityColumn retrievedColumn = entityColumnCache.findColumn(existingColumnName);
        assertNotNull(retrievedColumn);
        assertEquals(existingColumnName, retrievedColumn.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_retrieve_non_existing_column() {
        final String nonExistingColumnName = "foo";
        entityColumnCache.findColumn(nonExistingColumnName);
    }

    @Test
    public void retain_stored_columns_order() {
        final Collection<EntityColumn> columnsFromCache = entityColumnCache.getColumns();
        assertNotNull(columnsFromCache);

        final Collection<EntityColumn> columnsViaUtil = getAllColumns(entityClass);

        final int columnsFromCacheSize = columnsFromCache.size();
        final int columnsViaUtilSize = columnsViaUtil.size();
        assertEquals(columnsViaUtilSize, columnsFromCacheSize);

        final List<EntityColumn> columnsFromCacheList = new ArrayList<EntityColumn>(columnsFromCache);
        final List<EntityColumn> columnsViaUtilList = new ArrayList<EntityColumn>(columnsViaUtil);

        assertEquals(columnsViaUtilList.get(0), columnsFromCacheList.get(0));
        assertEquals(columnsViaUtilList.get(1), columnsFromCacheList.get(1));
        assertEquals(columnsViaUtilList.get(2), columnsFromCacheList.get(2));
    }
}
