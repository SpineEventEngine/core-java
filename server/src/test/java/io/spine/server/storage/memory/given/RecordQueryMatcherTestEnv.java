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

package io.spine.server.storage.memory.given;

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.protobuf.AnyPacker;
import io.spine.query.Columns;
import io.spine.query.RecordColumn;
import io.spine.query.RecordColumns;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.query.Subject;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;

/**
 * The test environment for {@link io.spine.server.storage.memory.RecordQueryMatcher} tests.
 *
 * <p>Provides various types of {@linkplain RecordColumn record columns}
 * that can be used to emulate a client-side query.
 */
@SuppressWarnings("BadImport")       // `create` looks fine in this context.
public final class RecordQueryMatcherTestEnv {

    private static final MessageRecordSpec<StgProjectId, StgProject> spec =
            new MessageRecordSpec<>(StgProjectId.class,
                                    StgProject.class,
                                    StgProject::getId,
                                    StgProjectColumns.definitions());

    /** Prevents instantiation of this test env class. */
    private RecordQueryMatcherTestEnv() {
    }

    /**
     * Returns the record specification for {@code StgProject}.
     */
    public static MessageRecordSpec<StgProjectId, StgProject> projectSpec() {
        return spec;
    }

    /**
     * Creates an empty {@code Subject} for the {@code StgProject}.
     */
    public static Subject<StgProjectId, StgProject> recordSubject() {
        return RecordQuery.newBuilder(StgProjectId.class, StgProject.class)
                .build()
                .subject();
    }

    /**
     * Creates a {@code Subject} for the {@code EntityRecord} with the given ID.
     */
    public static Subject<StgProjectId, StgProject> recordSubject(StgProjectId id) {
        return RecordQuery.newBuilder(parameterizedClsOf(id), StgProject.class)
                .id().is(id)
                .build()
                .subject();
    }

    @SuppressWarnings("unchecked" /* As per the declaration. */)
    private static <I> Class<I> parameterizedClsOf(I id) {
        return (Class<I>) id.getClass();
    }

    /**
     * Creates a new query builder targeting stored {@code StgProject} instances.
     */
    public static RecordQueryBuilder<StgProjectId, StgProject> newBuilder() {
        return RecordQuery.newBuilder(StgProjectId.class, StgProject.class);
    }

    /**
     * Columns of {@code StgProject} stored as record.
     */
    @RecordColumns(ofType = StgProject.class)
    public static final class StgProjectColumns {

        private StgProjectColumns() {
        }

        public static final RecordColumn<StgProject, String> name =
                RecordColumn.create("name", String.class, StgProject::getName);

        public static final RecordColumn<StgProject, Timestamp> due_date =
                RecordColumn.create("due_date", Timestamp.class, StgProject::getDueDate);

        public static final RecordColumn<StgProject, Any> state_as_any =
                RecordColumn.create("state_as_any", Any.class, AnyPacker::pack);

        public static final RecordColumn<StgProject, String> random_non_stored_column =
                RecordColumn.create("random_non_stored_column", String.class, (p) -> "31415926");

        /**
         * Returns all the column definitions.
         */
        public static Columns<StgProject> definitions() {
            return Columns.of(name, due_date, state_as_any);
        }
    }
}
