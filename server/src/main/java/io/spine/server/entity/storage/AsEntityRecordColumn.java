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

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A definition of an {@link EntityRecord} column, which uses the name of another column.
 */
final class AsEntityRecordColumn extends RecordColumn<EntityRecord, Object> {

    private static final long serialVersionUID = 0L;

    @SuppressWarnings({"unchecked", "rawtypes"})  // to avoid the generics hell.
    AsEntityRecordColumn(Column<?, ?> original) {
        super(columnName(original), (Class) original.type(), getter(original));
    }

    @SuppressWarnings("unused")     // method always throws an `IllegalStateException`.
    private static Getter<EntityRecord, Object> getter(Column<?, ?> original) {
        throw newIllegalStateException("`EntityRecordColumn`s do not have getters.");
    }

    private static String columnName(Column<?, ?> original) {
        return original.name()
                       .value();
    }
}
