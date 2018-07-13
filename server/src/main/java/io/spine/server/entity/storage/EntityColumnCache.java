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

import com.google.common.annotations.VisibleForTesting;
import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.entity.storage.Columns.checkColumnName;
import static io.spine.server.entity.storage.Columns.couldNotFindColumn;
import static java.util.Collections.synchronizedMap;

/**
 * A class designated to store cached {@linkplain EntityColumn entity column metadata} for a single
 * {@link Entity} class.
 *
 * <p>The cache remains empty on creation. The {@linkplain EntityColumn column metadata} is retrieved
 * on the first access to the {@linkplain EntityColumnCache cache} instance and then stored in it.
 *
 * <p>The order in which {@link EntityColumn entity columns} are stored, is retained for
 * the access operations.
 *
 * @author Dmytro Kuzmin
 * @see EntityColumn
 * @see Columns
 */
@Internal
public class EntityColumnCache {

    private final Class<? extends Entity> entityClass;
    private boolean columnsCached = false;

    /**
     * A container of {@link EntityColumn entity column} name and its data.
     *
     * Each {@link EntityColumn entity column} data instance can be accessed by the
     * corresponding entity column {@linkplain EntityColumn#getName() name}.
     *
     * <p>The data is stored this way for convenient querying of the specific columns.
     *
     * <p>This container is mutable and thread-safe.
     */
    private final Map<String, EntityColumn> entityColumnData =
            synchronizedMap(new LinkedHashMap<String, EntityColumn>());

    private EntityColumnCache(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        this.entityClass = entityClass;
    }

    /**
     * Creates an instance of {@link EntityColumnCache} for the given {@link Entity} class.
     *
     * <p>The {@linkplain EntityColumn column metadata} will not be retrieved and stored on creation.
     * Instead, the {@linkplain EntityColumnCache cache instance} will wait for the first access to it
     * to cache {@linkplain EntityColumn entity columns}.
     *
     * @param entityClass the class for which {@linkplain EntityColumn entity columns} should
     *                    be obtained and cached
     * @return new {@link EntityColumnCache} instance
     */
    public static EntityColumnCache initializeFor(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        return new EntityColumnCache(entityClass);
    }

    /**
     * Finds cached {@linkplain EntityColumn metadata} for the entity column with a given name.
     *
     * <p>If the {@linkplain EntityColumn column metadata} is not yet obtained and cached, this method
     * will {@linkplain EntityColumnCache#ensureColumnsCached() retrieve and cache it}.
     *
     * <p>If there is no {@link EntityColumn column} with the given name in the {@link Entity}
     * class managed by this {@linkplain EntityColumnCache cache}, the method will throw
     * {@link IllegalArgumentException}.
     *
     * @param columnName the {@linkplain EntityColumn#getName() name} of the searched column
     * @return {@linkplain EntityColumn found entity column}
     * @throws IllegalArgumentException if the {@link EntityColumn} is not found
     */
    public EntityColumn findColumn(String columnName) {
        checkColumnName(columnName);

        ensureColumnsCached();

        EntityColumn entityColumn = entityColumnData.get(columnName);

        if (entityColumn == null) {
            throw couldNotFindColumn(entityClass, columnName);
        }
        return entityColumn;
    }

    /**
     * Retrieves all {@linkplain EntityColumn entity columns} for the {@link Entity} class managed
     * by this {@linkplain EntityColumnCache cache}.
     *
     * <p>If the {@linkplain EntityColumn column metadata} is not yet obtained and cached, this method
     * will {@linkplain EntityColumnCache#ensureColumnsCached() retrieve and cache it}.
     *
     * @return {@linkplain EntityColumn entity column} {@link Collection} for the managed
     *         {@link Entity} class
     */
    public Collection<EntityColumn> getColumns() {
        ensureColumnsCached();
        return entityColumnData.values();
    }

    /**
     * {@linkplain Columns#getAllColumns(Class) Obtains} and caches {@link EntityColumn} data if it is not
     * yet cached.
     *
     * <p>If the data is already retrieved and cached, this method does nothing.
     */
    public void ensureColumnsCached() {
        if (!columnsCached) {
            obtainAndCacheColumns();
            columnsCached = true;
        }
    }

    /**
     * Checks if the current {@link EntityColumnCache} instance has already retrieved and cached
     * {@linkplain EntityColumn column metadata}.
     *
     * @return {@code true} if columns are cached and {@code false} otherwise
     */
    @VisibleForTesting
    boolean isEmpty() {
        return !columnsCached && entityColumnData.isEmpty();
    }

    /**
     * {@linkplain Columns#getAllColumns(Class) Obtains} and caches {@link EntityColumn}
     * column metadata.
     *
     * <p>If {@link Column} definitions are incorrect for the given {@link Entity} class this method
     * will throw {@link IllegalStateException}.
     *
     * @throws IllegalStateException if entity column definitions for the managed {@link Entity}
     * class are incorrect
     */
    private void obtainAndCacheColumns() {
        Collection<EntityColumn> columns = Columns.getAllColumns(entityClass);
        cacheEntityColumns(columns);
    }

    /**
     * Stores {@linkplain EntityColumn entity columns} from the {@link Iterable} to the inner
     * {@link EntityColumnCache#entityColumnData cache}.
     *
     * @param columns {@linkplain EntityColumn columns} to store
     */
    private void cacheEntityColumns(Iterable<EntityColumn> columns) {
        for (EntityColumn column : columns) {
            entityColumnData.put(column.getName(), column);
        }
    }
}
