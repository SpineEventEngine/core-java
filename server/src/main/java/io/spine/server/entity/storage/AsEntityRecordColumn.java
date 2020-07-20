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

package io.spine.server.entity.storage;

import io.spine.query.Column;
import io.spine.query.RecordColumn;
import io.spine.server.entity.EntityRecord;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Utility which creates views on the {@linkplain Column columns}.
 */
final class AsEntityRecordColumn {

    /**
     * Prevents the instantiation of this utility.
     */
    private AsEntityRecordColumn() {
    }

    /**
     * Creates a view on the given column as on a record column of an {@link EntityRecord}
     * with the given type of values.
     *
     * <p>The resulting view loses an ability to {@linkplain Column#valueIn(Object) obtain
     * the values from the record instance}.
     *
     * @param original the column to create a view for
     * @param typeOfValues the type of the column values
     * @param <V> the type of the column values
     * @return a view on the column
     */
    static <V> RecordColumn<EntityRecord, V> apply(Column<?, ?> original, Class<V> typeOfValues) {
        checkNotNull(original);
        checkNotNull(typeOfValues);
        String columnName = columnName(original);
        return new RecordColumn<>(columnName, typeOfValues, new NoGetter<>());
    }

    /**
     * Creates a view on the given column as on the record column of an {@linkplain EntityRecord}
     * with the {@code Object} values.
     *
     * <p>The resulting view loses an ability to {@linkplain Column#valueIn(Object) obtain
     * the values from the record instance}.
     *
     * @param original the column to create a view for
     * @return a view on the column
     */
    static RecordColumn<EntityRecord, Object> apply(Column<?, ?> original) {
        return apply(original, Object.class);
    }

    private static String columnName(Column<?, ?> original) {
        return original.name()
                       .value();
    }

    /**
     * Returns the getter which always throws an {@link IllegalStateException} upon invocation.
     */
    private static final class NoGetter<V> implements Column.Getter<EntityRecord, V> {

        @Override
        public V apply(EntityRecord record) {
            throw newIllegalStateException("`EntityRecordColumn`s do not have getters.");
        }
    }
}
