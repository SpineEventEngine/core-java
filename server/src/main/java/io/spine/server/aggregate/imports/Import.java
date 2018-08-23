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

package io.spine.server.aggregate.imports;

import com.google.protobuf.Timestamp;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.time.Timestamps2;
import io.spine.time.ZoneId;
import io.spine.time.ZoneIds;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.ZonedDateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class Import {

    private final @Nullable TenantId tenantId;

    private Import(BoundedContext boundedContext, @Nullable TenantId tenantId) {
        checkNotNull(boundedContext);
        if (boundedContext.isMultitenant()) {
            checkNotNull(
                tenantId,
                "`TenantId` must be specified for importing to multi-tenant `BoundedContext` %s",
                boundedContext.getName()
            );
        }
        this.tenantId = tenantId;
        if (tenantId != null) {
            boolean isDefault = TenantId.getDefaultInstance()
                                        .equals(tenantId);
            checkArgument(
                    !isDefault,
                    "`Import` does not accept default value of `TenantId`. " +
                            "Did you mean to pass `null`?"
            );
        }
    }

    /**
     * Obtains a offset for the time-zone at the specified time.
     */
    private static ZoneOffset offsetAt(Timestamp timestamp, ZoneId zoneId) {
        java.time.ZoneId jz = ZoneIds.toJavaTime(zoneId);
        java.time.ZoneOffset offset = ZonedDateTime.ofInstant(Timestamps2.toInstant(timestamp), jz)
                                                   .getOffset();
        return ZoneOffsets.of(offset);
    }
}
