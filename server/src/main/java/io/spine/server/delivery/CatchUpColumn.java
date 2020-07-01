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
import io.spine.query.RecordColumn;

/**
 * The columns stored for {@link CatchUp} statuses.
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // column names may repeat in different types.
public class CatchUpColumn {

    public static final RecordColumn<CatchUp, CatchUpStatus> status =
            new RecordColumn<>("status", CatchUpStatus.class, CatchUp::getStatus);

    public static final RecordColumn<CatchUp, Timestamp> when_last_read =
            new RecordColumn<>("when_last_read", Timestamp.class, CatchUp::getWhenLastRead);

    public static final RecordColumn<CatchUp, String> projection_type =
            new RecordColumn<>("projection_type", String.class, (m) -> m.getId()
                                                                        .getProjectionType());

    private CatchUpColumn() {
    }

    public static ImmutableList<RecordColumn<CatchUp, ?>> definitions() {
        return ImmutableList.of(status, when_last_read, projection_type);
    }
}
