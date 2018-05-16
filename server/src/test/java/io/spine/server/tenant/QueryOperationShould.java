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

package io.spine.server.tenant;

import io.spine.client.Queries;
import io.spine.client.Query;
import io.spine.client.QueryId;
import io.spine.test.Tests;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class QueryOperationShould {

    @Test(expected = NullPointerException.class)
    public void reject_null_input() {
        final Query nullQuery = Tests.nullRef();
        new QueryOperation(nullQuery) {
            @Override
            public void run() {
                // Do nothing;
            }
        };
    }

    @Test
    public void return_query() {
        final Query query = Query.newBuilder()
                                 .build();

        final QueryOperation op = new QueryOperation(query) {
            @Override
            public void run() {
                // Do nothing.
            }
        };

        assertEquals(query, op.query());
    }

    @Test
    public void return_query_id() {
        final QueryId id = Queries.generateId();
        final Query query = Query.newBuilder()
                                 .setId(id)
                                 .build();

        final QueryOperation op = new QueryOperation(query) {
            @Override
            public void run() {
                // Do nothing.
            }
        };

        assertEquals(id, op.queryId());
    }
}
