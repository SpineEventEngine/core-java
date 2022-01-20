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

package io.spine.server.delivery;

import com.google.protobuf.Timestamp;
import io.spine.query.Columns;
import io.spine.query.RecordColumn;
import io.spine.query.RecordColumns;

import static io.spine.query.RecordColumn.create;

/**
 * The columns stored for {@link CatchUp} states.
 */
@RecordColumns(ofType = CatchUp.class)
@SuppressWarnings({
        "DuplicateStringLiteralInspection"  /* Column names may repeat. */,
        "BadImport" /* `create` looks fine in this context. */,
        "WeakerAccess" /* Exposed for Spine libraries providing storage impls. */})
public final class CatchUpColumn {

    /**
     * Stores the status of the catch-up process.
     */
    public static final RecordColumn<CatchUp, CatchUpStatus>
            status = create("status", CatchUpStatus.class, CatchUp::getStatus);

    /**
     * Stores the time when the history has been last read by the catch-up process.
     */
    public static final RecordColumn<CatchUp, Timestamp>
            when_last_read = create("when_last_read", Timestamp.class, CatchUp::getWhenLastRead);

    /**
     * Stores the type URL of the projection-under-catch-up.
     */
    public static final RecordColumn<CatchUp, String>
            projection_type = create("projection_type", String.class, (m) -> m.getId()
                                                                              .getProjectionType());

    /**
     * Prevents this type from instantiation.
     *
     * <p>This class exists exclusively as a container of the column definitions. Thus it isn't
     * expected to be instantiated at all. See the {@link RecordColumns} docs for more details on
     * this approach.
     */
    private CatchUpColumn() {
    }

    /**
     * Returns all the column definitions.
     */
    public static Columns<CatchUp> definitions() {
        return Columns.of(status, when_last_read, projection_type);
    }
}
