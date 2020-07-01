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

import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.CustomColumn;
import io.spine.server.storage.CustomColumn.Getter;
import io.spine.server.storage.QueryableField;

/**
 * Columns storing the lifecycle attributes of an {@code Entity} within an {@link EntityRecord}.
 */
public enum LifecycleColumn implements QueryableField<EntityRecord> {

    archived((r) -> r.getLifecycleFlags()
                     .getArchived()),

    deleted((r) -> r.getLifecycleFlags()
                    .getDeleted());

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private final CustomColumn<Boolean, EntityRecord> column;

    <T> LifecycleColumn(Getter<EntityRecord, Boolean> getter) {
        OldColumnName name = OldColumnName.of(name());
        this.column = new CustomColumn<>(name, Boolean.class, getter);
    }

    Boolean valueIn(EntityRecord record) {
        return column.valueIn(record);
    }

    @Override
    public CustomColumn<?, EntityRecord> column() {
        return column;
    }
}
