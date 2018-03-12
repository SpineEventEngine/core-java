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

import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

@Internal
public class ColumnValueExtractor {

    private static final String SPINE_PACKAGE = "io.spine.";
    private static final String NON_PUBLIC_CLASS_WARNING =
            "Passed entity class %s is not public. Storage fields won't be extracted.";
    private static final String NON_PUBLIC_INTERNAL_CLASS_WARNING =
            "Passed entity class %s is probably a Spine internal non-public entity. " +
                    "Storage fields won't be extracted.";

    private final Entity entity;
    private final Collection<EntityColumn> entityColumns;

    private ColumnValueExtractor(Entity entity, Collection<EntityColumn> entityColumns) {
        this.entity = entity;
        this.entityColumns = entityColumns;
    }

    static ColumnValueExtractor create(Entity entity, Collection<EntityColumn> entityColumns) {
        checkNotNull(entity);
        checkNotNull(entityColumns);

        return new ColumnValueExtractor(entity, entityColumns);
    }

    /**
     * Extracts the {@linkplain EntityColumn column} values for the processed {@link Entity}.
     *
     * <p>Uses {@linkplain EntityColumn entity columns} stored in advance for the value extraction.
     *
     * <p>This way the process of {@linkplain ColumnReader#readColumns() obtaining columns} from
     * the given {@link Entity} class can be skipped.
     *
     * <p>This method will return {@linkplain Collections#emptyMap() empty map} for {@link Entity} classes
     * that are non-public or cannot be subjected to column extraction for some other reason.
     *
     * @return a {@link Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain EntityColumn.MemoizedValue memoized values}
     * @see EntityColumn.MemoizedValue
     */
    Map<String, EntityColumn.MemoizedValue> extractColumnValues() {
        checkNotNull(entityColumns);
        checkNotNull(entity);

        final Class<? extends Entity> entityClass = entity.getClass();
        if (!canExtractColumns(entityClass)) {
            return Collections.emptyMap();
        }

        final Map<String, EntityColumn.MemoizedValue> values = readColumnValues();
        return values;
    }


    /**
     * Checks if the given {@link Entity entity class} is public.
     *
     * <p>Outputs a message to the log if the class is non-public.
     *
     * @param entityClass {@link Entity entity class} to check
     * @return {@code true} if class is public and {@code false} otherwise
     */
    private boolean canExtractColumns(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        final int modifiers = entityClass.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            logNonPublicClass(entityClass);
            return false;
        }
        return true;
    }

    /**
     * Extracts {@linkplain EntityColumn.MemoizedValue memoized values} for the given {@linkplain EntityColumn columns}
     * of the given {@link Entity}.
     *
     * @return a {@code Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain EntityColumn.MemoizedValue memoized values}
     * @see EntityColumn.MemoizedValue
     */
    private Map<String, EntityColumn.MemoizedValue> readColumnValues() {
        final Map<String, EntityColumn.MemoizedValue> values = new HashMap<>(entityColumns.size());
        for (EntityColumn column : entityColumns) {
            final String name = column.getStoredName();
            final EntityColumn.MemoizedValue value = column.memoizeFor(entity);
            values.put(name, value);
        }
        return values;
    }

    /**
     * Writes the non-public {@code Entity} class warning into the log unless the passed class
     * represents one of the Spine internal {@link Entity} implementations.
     */
    private static void logNonPublicClass(Class<? extends Entity> cls) {
        final String className = cls.getCanonicalName();
        final boolean internal = className.startsWith(SPINE_PACKAGE);
        if (internal) {
            log().trace(format(NON_PUBLIC_INTERNAL_CLASS_WARNING, className));
        } else {
            log().warn(format(NON_PUBLIC_CLASS_WARNING, className));
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Columns.class);
    }
}
