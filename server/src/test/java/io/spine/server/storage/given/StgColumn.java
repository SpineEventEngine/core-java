/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.storage.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.query.RecordColumn;
import io.spine.query.RecordColumns;
import io.spine.test.storage.StgProject;

import static io.spine.query.RecordColumn.create;

/**
 * Columns of the {@link StgProject} stored as a plain {@link Message}.
 */
@RecordColumns(ofType = StgProject.class)
@SuppressWarnings(
        {"DuplicateStringLiteralInspection",  // Column names may repeat across records.
                "BadImport"})                 // `create` looks fine in this context.
public final class StgColumn {

    public static final RecordColumn<StgProject, Integer>
            project_version = create("project_version", Integer.class, (r) -> r.getProjectVersion()
                                                                               .getNumber());

    public static final RecordColumn<StgProject, Timestamp>
            due_date = create("due_date", Timestamp.class, StgProject::getDueDate);

    public static final RecordColumn<StgProject, String>
            status = create("status", String.class, (r) -> r.getStatus()
                                                            .name());

    /**
     * Prevents this type from instantiation.
     *
     * <p>This class exists exclusively as a container of the column definitions. Thus it isn't
     * expected to be instantiated at all. See the {@link RecordColumns} docs for more details on
     * this approach.
     */
    private StgColumn() {
    }

    public static ImmutableList<RecordColumn<StgProject, ?>> definitions() {
        return ImmutableList.of(project_version, due_date, status);
    }
}
