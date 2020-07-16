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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.RecordColumn;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;

/**
 * Instructs storage implementations on how to store a plain Protobuf message record.
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
public final class MessageRecordSpec<I, R extends Message> extends RecordSpec<I, R, R> {

    /**
     * The class of the record which storage is configured by this spec.
     */
    private final Class<R> recordClass;

    /**
     * A method object to extract the record identifier, once such a record is passed.
     */
    private final ExtractId<R, I> extractId;

    /**
     * The columns to store along with the record itself.
     */
    private final ImmutableMap<ColumnName, RecordColumn<R, ?>> columns;

    public MessageRecordSpec(Class<R> recordClass,
                             ExtractId<R, I> extractId,
                             Iterable<RecordColumn<R, ?>> columns) {
        super(recordClass);
        this.columns = stream(columns).collect(toImmutableMap(RecordColumn::name, (c) -> c));
        this.recordClass = recordClass;
        this.extractId = extractId;
    }

    public Class<R> recordClass() {
        return recordClass;
    }

    public MessageRecordSpec(Class<R> aClass, ExtractId<R, I> extractId) {
        this(aClass, extractId, ImmutableList.of());
    }

    @Override
    public Map<ColumnName, @Nullable Object> valuesIn(R record) {
        checkNotNull(record);
        Map<ColumnName, @Nullable Object> result = new HashMap<>();
        columns.forEach(
                (name, column) -> result.put(name, column.valueIn(record))
        );
        return result;
    }

    @Override
    protected I idValueIn(R source) {
        checkNotNull(source);
        return extractId.apply(source);
    }

    @Override
    protected Optional<Column<?, ?>> findColumn(ColumnName name) {
        RecordColumn<R, ?> result = columns.get(name);
        return Optional.ofNullable(result);
    }

    @Immutable
    @FunctionalInterface
    public interface ExtractId<R extends Message, I> extends Function<R, I> {
    }
}
