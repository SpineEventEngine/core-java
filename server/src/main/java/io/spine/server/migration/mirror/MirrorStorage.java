/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.migration.mirror;

import io.spine.query.RecordColumn;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.server.ContextSpec;
import io.spine.server.storage.RecordSpec;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.Storage;
import io.spine.server.storage.memory.InMemoryRecordStorage;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * A message storage for {@linkplain Mirror} projections, which exposes
 * {@linkplain MirrorStorage#readAll(RecordQuery)} method.
 */
public interface MirrorStorage extends Storage<MirrorId, Mirror> {

    RecordQueryBuilder<MirrorId, Mirror> queryBuilder();

    Iterator<Mirror> readAll(RecordQuery<MirrorId, Mirror> query);

    /**
     * In-memory implementation of {@linkplain MirrorStorage} used for tests.
     */
    class InMemory extends InMemoryRecordStorage<MirrorId, Mirror> implements MirrorStorage {

        public InMemory(ContextSpec context, RecordSpec<MirrorId, Mirror, ?> recordSpec) {
            super(context, recordSpec);
        }

        @Override
        public void write(MirrorId id, Mirror record) {
            var columns = Mirror.Column.definitions()
                    .stream()
                    .collect(Collectors.toMap(RecordColumn::name, column -> (Object) column.valueIn(record)));
            var recordWithColumns = RecordWithColumns.of(id, record, columns);
            write(recordWithColumns);
        }

        @Override
        public Iterator<Mirror> readAll(RecordQuery<MirrorId, Mirror> query) {
            return super.readAll(query);
        }
    }
}
