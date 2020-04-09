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

package io.spine.client;

import com.google.protobuf.FieldMask;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility for working with {@link ResponseFormat}s.
 */
public final class ResponseFormats {

    /**
     * Prevents this utility class from instantiation.
     */
    private ResponseFormats() {
    }

    public static ResponseFormat formatWith(FieldMask mask) {
        checkNotNull(mask);
        return responseFormat(mask, null, null);
    }

    static ResponseFormat responseFormat(@Nullable FieldMask mask,
                                         @Nullable OrderBy ordering,
                                         @Nullable Integer limit) {
        ResponseFormat.Builder result = ResponseFormat.newBuilder();
        if (mask != null) {
            result.setFieldMask(mask);
        }
        if (ordering != null) {
            result.addOrderBy(ordering);
        }
        if (limit != null) {
            checkArgument(limit > 0, "Limit must be positive, if set.");
            result.setLimit(limit);
        }
        return result.vBuild();
    }
}
