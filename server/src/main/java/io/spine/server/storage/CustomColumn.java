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

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.server.entity.storage.ColumnName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * A column to store along with a message record.
 *
 * <p>The value of the column is determined by a {@linkplain Getter getter} and should be extracted
 * from the record fields.
 *
 * <p>There are some other types of the columns are defined for the {@code Entity} state
 * in a declarative way, e.g. in the Protobuf definition of the {@code Entity} state or via
 * the {@code Entity}'s Java interface. The {@code CustomColumn}s are only intended to be used
 * along with the message records which do not represent an {@code EntityState}.
 *
 * <p>{@code CustomColumn} is the only way to programmatically specify the columns to be stored
 * along with a plain Protobuf message.
 *
 * @param <V>
 *         the type of the column value
 * @param <M>
 *         the type of the message record, along with which this column should be stored
 */
@Immutable
@Internal
public final class CustomColumn<V, M extends Message> extends AbstractColumn {

    private final Getter<M, V> getter;

    public CustomColumn(ColumnName name, Class<V> type, Getter<M, V> getter) {
        super(name, type);
        this.getter = getter;
    }

    public @Nullable V valueIn(M record) {
        return getter.apply(record);
    }

    @Immutable
    @FunctionalInterface
    public interface Getter<M extends Message, V> extends Function<M, V> {

    }
}
