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

package io.spine.server.entity.storage;

import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.spine.util.Exceptions.newIllegalStateException;
import static java.util.stream.Collectors.toMap;

@Internal
public final class Columns {

    /**
     * A map of entity columns by their name.
     */
    // TODO:2019-10-11:dmitry.kuzmin:WIP Think of creating a tiny type for column name.
    private final Map<String, Column> columns;

    private final EntityClass<?> entityClass;

    private Columns(Map<String, Column> columns, EntityClass<?> entityClass) {
        this.columns = columns;
        this.entityClass = entityClass;
    }

    public static Columns of(EntityClass<?> entityClass) {
        Map<String, Column> columns = new HashMap<>();
        return new Columns(columns, entityClass);
    }

    public Column get(String columnName) {
        Column result = find(columnName).orElseThrow(() -> columnNotFound(columnName));
        return result;
    }

    public Optional<Column> find(String columnName) {
        Column column = columns.get(columnName);
        Optional<Column> result = Optional.ofNullable(column);
        return result;
    }

    public Map<Column, Object> valuesIn(Entity<?, ?> source) {
        Map<Column, Object> result =
                columns.values()
                       .stream()
                       .collect(toMap(column -> column,
                                      column -> column.valueIn(source)));
        return result;
    }

    public Map<String, Object> valuesForPersistence(Entity<?, ?> source) {
        Map<String, Object> result =
                columns.values()
                       .stream()
                       .collect(toMap(Column::name,
                                      column -> valueForPersistence(source, column)));
        return result;
    }

    private static Object valueForPersistence(Entity<?, ?> source, Column column) {
        Object columnValue = column.valueIn(source);
        Object result = column.persistenceStrategy()
                              .apply(columnValue);
        return result;
    }

    private IllegalStateException columnNotFound(String columnName) {
        throw newIllegalStateException(
                "A column with name '%s' not found in entity state class `%s`.",
                columnName, entityClass.stateClass()
                                       .getCanonicalName());
    }
}
