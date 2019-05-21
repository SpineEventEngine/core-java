/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.given;

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.client.OrderBy;
import io.spine.client.Pagination;
import io.spine.client.TargetFilters;
import io.spine.server.entity.AbstractEntity;

import static io.spine.testing.Tests.assertMatchesMask;

public final class RecordBasedRepositoryTestEnv {

    private static final String ENTITY_NAME_COLUMN = "name";

    /** Prevents instantiation of this test environment class. */
    private RecordBasedRepositoryTestEnv() {
    }

    public static <E extends AbstractEntity<?, ?>>
    void assertMatches(E entity, FieldMask fieldMask) {
        Message state = entity.state();
        assertMatchesMask(state, fieldMask);
    }

    public static Pagination emptyPagination() {
        return Pagination.getDefaultInstance();
    }

    public static Pagination pagination(int pageSize) {
        return Pagination.newBuilder()
                                 .setPageSize(pageSize)
                                 .build();
    }

    public static OrderBy emptyOrder() {
        return OrderBy.getDefaultInstance();
    }

    /**
     * An order by {@linkplain #ENTITY_NAME_COLUMN entity name column}.
     */
    public static OrderBy orderByName(OrderBy.Direction direction) {
        return OrderBy.newBuilder()
                              .setColumn(ENTITY_NAME_COLUMN)
                              .setDirection(direction)
                              .build();
    }

    public static TargetFilters emptyFilters() {
        return TargetFilters.getDefaultInstance();
    }

    public static FieldMask emptyFieldMask() {
        return FieldMask.getDefaultInstance();
    }
}
