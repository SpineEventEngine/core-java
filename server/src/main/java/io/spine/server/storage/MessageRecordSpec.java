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
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.server.entity.storage.ColumnName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * A specification of a Protobuf message record to store.
 *
 * <p>Defines the collection of the columns to store along with the message record
 * for further querying.
 *
 * <p>This specification does not describe the storage mechanism of entity records.
 * See {@link io.spine.server.entity.storage.EntityRecordSpec EntityRecordSpec} for more details.
 *
 * @param <R>
 *         the type of the record
 * @see io.spine.server.entity.storage.EntityRecordSpec
 */
@Immutable
@Internal
public class MessageRecordSpec<R extends Message> extends RecordSpec<R> {

    /**
     * The columns to store along with the record itself.
     */
    private final ImmutableMap<ColumnName, CustomColumn<?, R>> columns;

    private final Class<R> recordClass;

    public MessageRecordSpec(Class<R> recordClass, Iterable<CustomColumn<?, R>> columns) {
        super(recordClass);
        this.columns = stream(columns).collect(toImmutableMap(AbstractColumn::name, (c) -> c));
        this.recordClass = recordClass;
    }

    private MessageRecordSpec(Class<R> aClass) {
        this(aClass, ImmutableList.of());
    }

    public static <M extends Message> MessageRecordSpec<M> emptyOf(Class<M> recordClass) {
        return new MessageRecordSpec<>(recordClass);
    }

    @Override
    public Map<ColumnName, @Nullable Object> valuesIn(Object record) {
        checkNotNull(record);
        R message = asMessage(record);
        Map<ColumnName, @Nullable Object> result = new HashMap<>();
        columns.forEach(
                (name, column) -> result.put(name, column.valueIn(message))
        );
        return result;
    }

    /**
     * Returns all columns of the record.
     */
    @Override
    public final ImmutableList<Column> columnList() {
        ImmutableList<Column> result = ImmutableList.copyOf(this.columns.values());
        return result;
    }

    /**
     * Searches for a column with a given name.
     */
    @Override
    public final Optional<Column> find(ColumnName columnName) {
        checkNotNull(columnName);
        Column column = columns.get(columnName);
        return Optional.ofNullable(column);
    }

    @Override
    protected IllegalArgumentException columnNotFound(ColumnName columnName) {
        throw newIllegalArgumentException(
                "A column with name '%s' not found in the `Message` class `%s`.",
                columnName, recordClass.getCanonicalName());
    }

    @SuppressWarnings("unchecked")  /* It's cheaper to attempt to cast,
                                       than verify that the object is of type `R`.*/
    private R asMessage(Object record) {
        return (R) record;
    }
}
