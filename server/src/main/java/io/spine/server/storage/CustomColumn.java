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
import io.spine.server.entity.storage.OldColumnName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * A column to store along with a message record.
 *
 * <p>The value of the column is determined by a {@linkplain Getter getter} and should be extracted
 * from the record fields.
 *
 * <p>There are some other types of the columns defined for the stored state of an {@code Entity}
 * in a declarative way. They may be defined  in the Protobuf definition of the {@code Entity} state
 * or via the {@code Entity}'s Java interface.
 *
 * <p>Unlike them, the {@code CustomColumn}s are only intended to be used along with the plain
 * {@code Message} records and not for storing the {@code Entity} state. The columns of this type
 * is the only way to programmatically specify the columns to be stored with such records.
 *
 * @param <V>
 *         the type of the column value
 * @param <M>
 *         the type of the message record, along with which this column should be stored
 */
@Immutable
public final class CustomColumn<V, M extends Message> extends AbstractColumn {

    private final Getter<M, V> getter;

    /**
     * Creates a new instance of a {@code CustomColumn}.
     *
     * @param name
     *         the name of the column
     * @param type
     *         the type of the column value
     * @param getter
     *         the getter for the column value
     */
    public CustomColumn(OldColumnName name, Class<V> type, Getter<M, V> getter) {
        super(name, type);
        this.getter = getter;
    }

    /**
     * Obtains the value of this column for the passed message record.
     *
     * @see Getter
     */
    public @Nullable V valueIn(M record) {
        return getter.apply(record);
    }

    /**
     * A method object serving to obtain the value of the column for some particular record of the
     * matching type.
     *
     * @param <M>
     *         the type of the message record
     * @param <V>
     *         the type of the column value
     */
    @Immutable
    @FunctionalInterface
    public interface Getter<M extends Message, V> extends Function<M, V> {
    }
}
