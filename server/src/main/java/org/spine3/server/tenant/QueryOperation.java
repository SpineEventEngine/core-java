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

import org.spine3.annotations.Internal;
import org.spine3.client.Query;
import org.spine3.client.QueryId;

/**
 * A tenant-aware operation performed in response to a query.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public abstract class QueryOperation extends TenantAwareOperation {

    private final Query query;

    /**
     * Creates new instance of the operation.
     *
     * @param query the query in response to which the operation is performed.
     */
    protected QueryOperation(Query query) {
        super(query.getContext()
                   .getTenantId());
        this.query = query;
    }

    /**
     * Obtains the ID of the query.
     */
    protected QueryId queryId() {
        return query.getId();
    }

    /**
     * Obtains the query in response to which the operation is performed.
     */
    protected Query query() {
        return query;
    }
}
