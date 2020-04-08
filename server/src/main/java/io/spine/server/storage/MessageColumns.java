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
 * A message to store, along with the additional columns to store for further querying.
 */
@Immutable
@Internal
public class MessageColumns<M extends Message> extends Columns<M> {

    /**
     * The columns to store along with the message itself.
     */
    private final ImmutableMap<ColumnName, MessageColumn<?, M>> columns;

    private final Class<M> messageClass;

    public MessageColumns(Class<M> messageClass, Iterable<MessageColumn<?, M>> columns) {
        super(messageClass);
        this.columns = stream(columns).collect(toImmutableMap(AbstractColumn::name, (c) -> c));
        this.messageClass = messageClass;
    }

    private MessageColumns(Class<M> aClass) {
        this(aClass, ImmutableList.of());
    }

    public static <M extends Message> MessageColumns<M> emptyOf(Class<M> messageClass) {
        return new MessageColumns<>(messageClass);
    }

    @Override
    public Map<ColumnName, @Nullable Object> valuesIn(Object record) {
        checkNotNull(record);
        M message = asMessage(record);
        Map<ColumnName, @Nullable Object> result = new HashMap<>();
        columns.forEach(
                (name, column) -> result.put(name, column.valueIn(message))
        );
        return result;
    }

    @SuppressWarnings("unchecked")  /* It's cheaper to attempt to cast,
                                       than verify that the object is of type `M`.*/
    private M asMessage(Object record) {
        return (M) record;
    }

    /**
     * Returns all columns of the message.
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
                columnName, messageClass.getCanonicalName());
    }
}
