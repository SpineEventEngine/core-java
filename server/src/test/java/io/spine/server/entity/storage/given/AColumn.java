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

package io.spine.server.entity.storage.given;

import io.spine.server.entity.storage.EntityRecordSpec;
import io.spine.server.entity.storage.OldColumnName;
import io.spine.server.storage.OldColumn;

@SuppressWarnings("DuplicateStringLiteralInspection")
public final class AColumn {

    /** Prevents instantiation of this test env class. */
    private AColumn() {
    }

    public static OldColumn column() {
        return stringColumn();
    }

    public static OldColumn stringColumn() {
        return column("name");
    }

    public static OldColumn intColumn() {
        return column("estimate_in_days");
    }

    public static OldColumn timestampColumn() {
        return column("due_date");
    }

    private static OldColumn column(String name) {
        EntityRecordSpec spec = EntityRecordSpec.of(TaskViewProjection.class);
        OldColumnName columnName = OldColumnName.of(name);
        OldColumn column = spec.get(columnName);
        return column;
    }
}
