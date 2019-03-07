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

package io.spine.client.given;

import com.google.protobuf.Message;
import io.spine.client.OrderBy;
import io.spine.client.OrderByVBuilder;
import io.spine.client.Pagination;
import io.spine.client.PaginationVBuilder;
import io.spine.test.client.TestEntity;
import io.spine.type.TypeUrl;

public class QueryBuilderTestEnv {

    public static final Class<? extends Message> TEST_ENTITY_TYPE = TestEntity.class;
    public static final TypeUrl TEST_ENTITY_TYPE_URL = TypeUrl.of(TEST_ENTITY_TYPE);
    public static final Pagination EMPTY_PAGINATION = Pagination.getDefaultInstance();
    public static final OrderBy EMPTY_ORDER_BY = OrderBy.getDefaultInstance();
    public static final String SECOND_FIELD = "second_field";
    public static final String FIRST_FIELD = "first_field";

    /** Prevents instantiation of this test environment class. */
    private QueryBuilderTestEnv() {
    }

    public static Pagination pagination(int pageSize) {
        return PaginationVBuilder.newBuilder()
                                 .setPageSize(pageSize)
                                 .build();
    }

    public static OrderBy orderBy(String column, OrderBy.Direction direction) {
        return OrderByVBuilder.newBuilder()
                              .setColumn(column)
                              .setDirection(direction)
                              .build();
    }
}
