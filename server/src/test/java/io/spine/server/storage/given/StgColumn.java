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

package io.spine.server.storage.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.storage.CustomColumn;
import io.spine.server.storage.QueryableField;
import io.spine.test.storage.StgProject;

/**
 * Columns of the {@link StgProject} stored as a plain {@link Message}.
 */
public enum StgColumn implements QueryableField<StgProject> {

    /**
     * The version of the project, stored as an integer number.
     */
    project_version(Integer.class, (m) -> m.getProjectVersion()
                                           .getNumber()),

    /**
     * When the project is due; stored as {@link Timestamp}.
     */
    due_date(Timestamp.class, StgProject::getDueDate);

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private final CustomColumn<?, StgProject> column;

    <T> StgColumn(Class<T> type, CustomColumn.Getter<StgProject, T> getter) {
        ColumnName name = ColumnName.of(name());
        this.column = new CustomColumn<>(name, type, getter);
    }

    static ImmutableList<CustomColumn<?, StgProject>> definitions() {
        ImmutableList.Builder<CustomColumn<?, StgProject>> list = ImmutableList.builder();
        for (StgColumn value : values()) {
            list.add(value.column);
        }
        return list.build();
    }

    @Override
    public CustomColumn<?, StgProject> column() {
        return column;
    }
}
