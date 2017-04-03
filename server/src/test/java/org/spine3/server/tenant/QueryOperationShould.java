/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.tenant;

import org.junit.Test;
import org.spine3.client.Query;
import org.spine3.client.QueryContext;
import org.spine3.client.QueryId;
import org.spine3.test.Tests;

import static org.junit.Assert.assertEquals;
import static org.spine3.base.Identifiers.newUuid;

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
        final QueryId id = QueryId.newBuilder()
                                     .setUuid(newUuid())
                                     .build();
        final Query query = Query.newBuilder()
                                 .setContext(QueryContext.newBuilder()
                                                         .setQueryId(id))
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
