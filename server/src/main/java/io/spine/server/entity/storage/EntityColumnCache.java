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

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.Columns.obtainColumns;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;

@Internal
public class EntityColumnCache {

    private final Class<? extends Entity> entityClass;
    private boolean columnsCached = false;

    /** EntityColumns of the managed entity class. */
    private Map<String, EntityColumn> entityColumns = synchronizedMap(
            new LinkedHashMap<String, EntityColumn>());

    @VisibleForTesting
    private EntityColumnCache() {
        this.entityClass = null;
    }

    private EntityColumnCache(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        this.entityClass = entityClass;
    }

    @VisibleForTesting
    static EntityColumnCache getEmptyInstance() {
        return new EntityColumnCache();
    }

    public static EntityColumnCache initializeFor(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        return new EntityColumnCache(entityClass);
    }

    public EntityColumn findColumn(String columnName) {
        checkNotEmptyOrBlank(columnName, "entity column name");

        ensureColumnsCached();

        final EntityColumn entityColumn = entityColumns.get(columnName);

        if (entityColumn == null) {
            throw new IllegalArgumentException(
                    format("Could not find an EntityColumn description for %s.%s.",
                           entityClass.getCanonicalName(),
                           columnName));

        }

        return entityColumn;
    }

    public Collection<EntityColumn> getAllColumns() {
        ensureColumnsCached();
        return entityColumns.values();
    }

    public void ensureColumnsCached() {
        if (!columnsCached) {
            obtainAndCacheColumns();
            columnsCached = true;
        }
    }

    @VisibleForTesting
    boolean isEmpty() {
        return !columnsCached && entityColumns.isEmpty();
    }

    private void obtainAndCacheColumns() {
        final Collection<EntityColumn> columns = obtainColumns(entityClass);
        cacheEntityColumns(columns);
    }

    private void cacheEntityColumns(Iterable<EntityColumn> columns) {
        for (EntityColumn column : columns) {
            entityColumns.put(column.getName(), column);
        }
    }
}
