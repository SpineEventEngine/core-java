/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.testdata;

import io.spine.server.entity.EntityRecord;
import io.spine.testing.core.given.GivenVersion;

import static io.spine.base.Identifier.newUuid;
import static io.spine.protobuf.TypeConverter.toAny;

/**
 * Creates {@link EntityRecord}s for tests.
 *
 * @author Alexander Litus
 */
public class TestEntityStorageRecordFactory {

    private TestEntityStorageRecordFactory() {
    }

    /** Creates a new record with all fields set. */
    public static EntityRecord newEntityStorageRecord() {
        EntityRecord.Builder builder =
                // Set any non-default (non-zero) value for version.
                EntityRecord.newBuilder()
                            .setState(toAny(newUuid()))
                            .setVersion(GivenVersion.withNumber(5));
        return builder.build();
    }
}
