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
import io.spine.server.storage.CustomColumn.Getter;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.QueryableField;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.RecordStorageDelegate;
import io.spine.server.storage.StorageFactory;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;

/**
 * A storage of the {@link io.spine.test.storage.StgProject} messages.
 *
 * <p>While the message itself is marked as an entity state, this storage operates with it
 * like it is an ordinary message record.
 *
 * <p>This storage defines several {@linkplain StgColumn custom columns} to use in tests.
 *
 * @see RecordStorageDelegateTest
 */
public class StgProjectStorage extends RecordStorageDelegate<StgProjectId, StgProject> {

    public StgProjectStorage(StorageFactory factory, boolean multitenant) {
        super(newStorage(factory, multitenant));
    }

    private static RecordStorage<StgProjectId, StgProject>
    newStorage(StorageFactory factory, boolean multitenant) {
        MessageRecordSpec<StgProject> spec = new MessageRecordSpec<>(StgProject.class,
                                                                     StgColumn.definitions());
        return factory.createRecordStorage(spec, multitenant);
    }

    public enum StgColumn implements QueryableField<StgProject> {

        project_version(Integer.class, (m) -> m.getProjectVersion()
                                               .getNumber()),

        due_date(Timestamp.class, StgProject::getDueDate);

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final CustomColumn<?, StgProject> column;

        <T> StgColumn(Class<T> type, Getter<StgProject, T> getter) {
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

}
