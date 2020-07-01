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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.storage.CustomColumn;
import io.spine.server.storage.CustomColumn.Getter;
import io.spine.server.storage.QueryableField;

/**
 * The columns stored for {@link CatchUp} statuses.
 */
public enum CatchUpColumn implements QueryableField<CatchUp> {

    status(CatchUpStatus.class, CatchUp::getStatus),

    when_last_read(Timestamp.class, CatchUp::getWhenLastRead),

    projection_type(String.class, (m) -> m.getId()
                                          .getProjectionType());

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private final CustomColumn<?, CatchUp> column;

    <T> CatchUpColumn(Class<T> type, Getter<CatchUp, T> getter) {
        ColumnName name = ColumnName.of(name());
        this.column = new CustomColumn<>(name, type, getter);
    }

    static ImmutableList<CustomColumn<?, CatchUp>> definitions() {
        ImmutableList.Builder<CustomColumn<?, CatchUp>> list = ImmutableList.builder();
        for (CatchUpColumn value : values()) {
            list.add(value.column);
        }
        return list.build();
    }

    @Override
    public CustomColumn<?, CatchUp> column() {
        return column;
    }
}
