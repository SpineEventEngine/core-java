/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.collect.ImmutableList;
import io.spine.annotation.Internal;
import io.spine.server.entity.storage.ColumnName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Defines the specification of a record in a storage.
 *
 * <p>Enumerates the record columns to store along with the record itself.
 *
 * @param <I>
 *         the type of the record identifier
 * @param <R>
 *         the type of the stored record
 * @param <S>
 *         the type of the source object on top of which the values of the columns are extracted
 */
@Internal
public abstract class RecordSpec<I, R, S> {

    private final Class<R> recordType;

    /**
     * Creates a new {@code RecordSpec} instance for the record of the passed type
     */
    protected RecordSpec(Class<R> recordType) {
        this.recordType = recordType;
    }

    /**
     * Returns the type of the stored record.
     */
    public final Class<R> recordType() {
        return recordType;
    }

    /**
     * Reads the values of all columns specified for the record from the passed source.
     *
     * @param source
     *         the object from which the column values are read
     * @return {@code Map} of column names and their respective values
     */
    protected abstract Map<ColumnName, @Nullable Object> valuesIn(S source);

    /**
     * Reads the identifier value of the record.
     *
     * @param source
     *         the object providing the ID value
     * @return the value of the identifier
     */
    public abstract I idValueIn(S source);

    /**
     * Returns all columns of the record.
     */
    public abstract ImmutableList<Column> columnList();

    /**
     * Searches for a column with a given name.
     */
    public abstract Optional<Column> find(ColumnName columnName);

    /**
     * Obtains a column by name.
     *
     * @throws IllegalArgumentException
     *         if the column with the specified name is not found
     */
    public final Column get(ColumnName columnName) {
        checkNotNull(columnName);
        Column result = find(columnName).orElseThrow(() -> columnNotFound(columnName));
        return result;
    }

    protected abstract IllegalArgumentException columnNotFound(ColumnName columnName);

}
