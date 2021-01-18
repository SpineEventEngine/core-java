/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
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
 * Instructs storage implementations on how to store a plain Protobuf message as a storage record.
 *
 * <p>Defines the identifier column and the collection of the data columns to store along with
 * the message record for further querying. Each column defines a way to calculate the stored value
 * basing on the passed message.
 *
 * <p>This specification is not well-suited for describing the storage of
 * {@link io.spine.server.entity.EntityRecord}s, as the values of their columns may be calculated
 * from both the entity state and {@code Entity} instance attributes.
 * See {@link io.spine.server.entity.storage.EntityRecordSpec EntityRecordSpec} for more details.
 *
 * @param <R>
 *         the type of the record
 * @see io.spine.server.entity.storage.EntityRecordSpec
 */
@Immutable
public final class MessageRecordSpec<I, R extends Message> extends RecordSpec<I, R, R> {

    /**
     * A method object to extract the record identifier, once such a record is passed.
     */
    private final ExtractId<R, I> extractId;

    /**
     * The columns to store along with the record itself.
     */
    private final ImmutableMap<ColumnName, RecordColumn<R, ?>> columns;

    /**
     * Creates a new record specification listing the columns to store along with the record.
     *
     * @param idType
     *         the type of the record identifier
     * @param recordType
     *         the type of the record
     * @param extractId
     *         a method object to extract the value of an identifier given an instance of a record
     * @param columns
     *         the definitions of the columns to store along with the record
     */
    public MessageRecordSpec(Class<I> idType,
                             Class<R> recordType,
                             ExtractId<R, I> extractId,
                             Iterable<RecordColumn<R, ?>> columns) {
        super(idType, recordType);
        this.columns =
                stream(columns).collect(
                        toImmutableMap(RecordColumn::name, (c) -> c)
                );
        this.extractId = extractId;
    }

    /**
     * Creates a new record specification.
     *
     * <p>The specifications created implies that no columns are stored for the record.
     * To define the stored columns,
     * please use {@linkplain #MessageRecordSpec(Class, Class, ExtractId, Iterable) another ctor}.
     *
     * @param idType
     *         the type of the record identifier
     * @param recordType
     *         the type of the record
     * @param extractId
     *         a method object to extract the value of an identifier given an instance of a record
     */
    public MessageRecordSpec(Class<I> idType, Class<R> recordType, ExtractId<R, I> extractId) {
        this(idType, recordType, extractId, ImmutableList.of());
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
    public Optional<Column<?, ?>> findColumn(ColumnName name) {
        RecordColumn<R, ?> result = columns.get(name);
        return Optional.ofNullable(result);
    }

    /**
     * A method object to extract the value of an record identifier given an instance of a record.
     *
     * <p>Once some storage is passed a record to store, the value of the record identifier has
     * to be determined. To avoid passing the ID value for each record, one defines an way
     * to obtain the identifier value from the record instance itself — by defining
     * an {@code ExtractId} as a part of the record specification for the storage.
     *
     * @param <R>
     *         the type of records from which to extract the ID value
     * @param <I>
     *         the type of the record identifiers to retrieve
     */
    @Immutable
    @FunctionalInterface
    public interface ExtractId<R extends Message, I> extends Function<R, I> {

        /**
         * {@inheritDoc}
         *
         * This method differs from its parent by the fact it never returns {@code null}.
         */
        @CanIgnoreReturnValue
        @Override
        I apply(@Nullable R input);
    }
}
