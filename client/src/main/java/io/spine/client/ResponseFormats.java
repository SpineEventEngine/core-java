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

    /**
     * Creates a new {@code ResponseFormat} with just the field mask.
     *
     * <p>The {@linkplain ResponseFormat#getLimit() limit} and
     * {@linkplain ResponseFormat#getOrderByList() ordering} are not set for the resulting instance.
     *
     * @param mask
     *         the field mask to apply to each item in the response
     */
    public static ResponseFormat formatWith(FieldMask mask) {
        checkNotNull(mask);
        return responseFormat(mask, null, null);
    }

    /**
     * Creates a new {@code ResponseFormat} with the ordering and the limit
     *
     * <p>The {@linkplain ResponseFormat#getFieldMask() field mask} is not set.
     *
     * @param orderBy
     *         the ordering to apply
     * @param limit
     *         the maximum number of records set to be returned
     */
    public static ResponseFormat formatWith(OrderBy orderBy, Integer limit) {
        checkNotNull(orderBy);
        checkNotNull(limit);
        return responseFormat(null, orderBy, limit);
    }

    /**
     * Creates a new {@code ResponseFormat} with the ordering, limit and the field mask.
     *
     * @param orderBy
     *         the ordering to apply
     * @param limit
     *         the maximum number of records set to be returned
     * @param fieldMask
     *         the field mask to apply to each item in the response
     */
    public static ResponseFormat formatWith(FieldMask fieldMask, OrderBy orderBy, Integer limit) {
        checkNotNull(fieldMask);
        checkNotNull(orderBy);
        checkNotNull(limit);
        return responseFormat(fieldMask, orderBy, limit);
    }

    /**
     * Creates a new {@code ResponseFormat}.
     *
     * <p>A caller of this method may choose which parts of the format are set. {@code null} value
     * passed signalizes that the part should not be set.
     *
     * @param mask
     *         the field mast to apply to each item in the response,
     *         or {@code null} if it should not be set
     * @param ordering
     *         the ordering to return the results in,
     *         or {@code null} if the ordering is not specified
     * @param limit
     *         the maximum number of records to return in the scope of response,
     *         or {@code null} if no particular limit should be applied
     */
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
