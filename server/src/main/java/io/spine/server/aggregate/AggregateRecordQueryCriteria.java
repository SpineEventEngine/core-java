/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate;

import java.util.List;

import static io.spine.validate.Validate.checkPositive;

/**
 * Specifies the filtering criteria for {@link AggregateEventRecord} queries.
 * 
 * <p>A result of executing such a query is a {@linkplain List<AggregateEventRecord>} which 
 * satisfies the query criteria.
 *
 * @author Mykhailo Drachuk
 */
class AggregateRecordQueryCriteria {
    private final int batchSize;

    /**
     * Creates a new query criteria instance with the specified batch size.
     * 
     * @param batchSize the number of events that should be returned.
     */
    AggregateRecordQueryCriteria(int batchSize) {
        checkPositive(batchSize);
        this.batchSize = batchSize;
    }

    /**
     * Obtains the number of {@linkplain AggregateEventRecord events} to read per batch.
     *
     * @return the events batch size
     */
    public int batchSize() {
        return batchSize;
    }
}
